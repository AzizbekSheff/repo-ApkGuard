package com.apkguard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UserProfile(
    val username: String = "",
    val email: String = "",
    val scanCount: Int = 0,
    val premium: Boolean = false,
    val admin: Boolean = false
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object VerificationSent : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val analytics = FirebaseAnalytics.getInstance(context)
    
    private val _authState = MutableStateFlow<AuthState>(
        if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) AuthState.Authenticated 
        else AuthState.Idle
    )
    val authState = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private var userListener: ListenerRegistration? = null

    init {
        // Har safar auth holati o'zgarganda profilni yuklashni boshlash
        viewModelScope.launch {
            if (auth.currentUser?.isEmailVerified == true) {
                startObservingProfile()
            }
        }
    }

    private fun startObservingProfile() {
        val uid = auth.currentUser?.uid ?: return
        userListener?.remove()
        
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                if (snapshot != null && snapshot.exists()) {
                    _userProfile.value = snapshot.toObject(UserProfile::class.java)
                } else {
                    // Agar Firestore'da profil yo'q bo'lsa, yaratamiz
                    val user = auth.currentUser
                    val newProfile = UserProfile(
                        username = user?.displayName ?: "Foydalanuvchi",
                        email = user?.email ?: "",
                        scanCount = 0,
                        premium = false,
                        admin = false
                    )
                    db.collection("users").document(uid).set(newProfile)
                }
            }
    }

    fun getDisplayName(): String = _userProfile.value?.username ?: auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Foydalanuvchi"

    fun getString(resId: Int): String = context.getString(resId)

    fun updateUsername(newUsername: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Avval username bandligini tekshiramiz (agar o'zgargan bo'lsa)
                val currentProfile = _userProfile.value
                if (currentProfile?.username?.lowercase() == newUsername.lowercase()) return@launch

                val usernameDoc = db.collection("usernames").document(newUsername.lowercase()).get().await()
                if (usernameDoc.exists()) {
                    _authState.value = AuthState.Error(context.getString(com.apkguard.R.string.username_taken))
                    return@launch
                }

                // Eski username hujjatini o'chirish
                currentProfile?.username?.let { old ->
                    db.collection("usernames").document(old.lowercase()).delete().await()
                }

                // Yangi username band qilish
                db.collection("usernames").document(newUsername.lowercase()).set(mapOf("email" to (currentProfile?.email ?: ""), "uid" to uid))
                
                // Profilni yangilash
                db.collection("users").document(uid).update("username", newUsername).await()
                
                // Firebase Auth displayName-ni yangilash
                auth.currentUser?.updateProfile(userProfileChangeRequest { displayName = newUsername })?.await()
                
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Username yangilashda xatolik: ${e.localizedMessage}")
            }
        }
    }

    fun signUp(username: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val usernameDoc = db.collection("usernames").document(username.lowercase()).get().await()
                if (usernameDoc.exists()) {
                    _authState.value = AuthState.Error(context.getString(com.apkguard.R.string.username_taken))
                    return@launch
                }

                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = result.user ?: throw Exception("Xatolik")

                user.updateProfile(userProfileChangeRequest { displayName = username }).await()

                db.collection("usernames").document(username.lowercase()).set(mapOf("email" to email, "uid" to user.uid))
                
                val newProfile = UserProfile(username, email, 0, false)
                db.collection("users").document(user.uid).set(newProfile).await()

                user.sendEmailVerification().await()
                _authState.value = AuthState.VerificationSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Xatolik")
            }
        }
    }

    fun signIn(usernameOrEmail: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                var email = usernameOrEmail
                if (!usernameOrEmail.contains("@")) {
                    // Username orqali emailni olish (Offline xatosini oldini olish uchun retry logikasi bilan)
                    val doc = db.collection("usernames").document(usernameOrEmail.trim().lowercase())
                        .get()
                        .await()
                    
                    email = doc.getString("email") ?: throw Exception(context.getString(com.apkguard.R.string.user_not_found))
                }

                val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                result.user?.reload()?.await()
                
                if (result.user?.isEmailVerified == true) {
                    startObservingProfile()
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error(context.getString(com.apkguard.R.string.email_not_verified))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: context.getString(com.apkguard.R.string.auth_error))
            }
        }
    }

    fun signOut() {
        userListener?.remove()
        auth.signOut()
        _userProfile.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun checkVerification() {
        viewModelScope.launch {
            auth.currentUser?.reload()?.await()
            if (auth.currentUser?.isEmailVerified == true) {
                startObservingProfile()
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error(context.getString(com.apkguard.R.string.email_not_verified))
            }
        }
    }

    fun incrementScanCount() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update("scanCount", com.google.firebase.firestore.FieldValue.increment(1))
            } catch (e: Exception) {
                // Xatolikni kuzatish mumkin
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
