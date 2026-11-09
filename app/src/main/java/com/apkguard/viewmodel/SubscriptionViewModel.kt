package com.apkguard.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.apkguard.data.billing.BillingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingManager: BillingManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products = _products.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus = _uploadStatus.asStateFlow()

    sealed class UploadStatus {
        object Idle : UploadStatus()
        object Loading : UploadStatus()
        object Success : UploadStatus()
        data class Error(val message: String) : UploadStatus()
    }

    init {
        // Play Store products load (Optional now since we use manual)
        viewModelScope.launch {
            billingManager.products.collect {
                _products.value = it
            }
        }
    }

    fun uploadReceipt(uri: Uri, planId: String) {
        val user = auth.currentUser
        if (user == null) {
            _uploadStatus.value = UploadStatus.Error("Tizimga kirmagansiz. Iltimos, login qiling.")
            return
        }

        val uid = user.uid
        val email = user.email ?: "unknown"

        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Loading
            try {
                val downloadUrl = withContext(Dispatchers.IO) {
                    // Read bytes immediately to avoid URI permission expiration
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw Exception("Rasmni o'qib bo'lmadi.")
                    
                    if (bytes.isEmpty()) throw Exception("Tanlangan rasm bo'sh.")

                    val storagePath = "receipts/${uid}_${UUID.randomUUID()}.jpg"
                    val ref = storage.reference.child(storagePath)
                    
                    // Upload using putBytes is more reliable for short-lived URIs
                    ref.putBytes(bytes).await()
                    
                    // Get the download URL
                    ref.downloadUrl.await().toString()
                }

                // 3. Save Request to Firestore
                val request = hashMapOf(
                    "userId" to uid,
                    "email" to email,
                    "planId" to planId,
                    "receiptUrl" to downloadUrl,
                    "status" to "pending",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "adminNote" to ""
                )

                db.collection("payment_requests").add(request).await()
                _uploadStatus.value = UploadStatus.Success
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = when {
                    e.message?.contains("permission-denied") == true -> "Firebase Storage: Ruxsat berilmadi (Rules error)."
                    e.message?.contains("does not exist") == true -> "Fayl yuklanishda xatolik yuz berdi."
                    else -> e.localizedMessage ?: e.message ?: "Noma'lum xatolik"
                }
                _uploadStatus.value = UploadStatus.Error(errorMsg)
            }
        }
    }

    fun resetUploadStatus() {
        _uploadStatus.value = UploadStatus.Idle
    }

    fun payWithClick(activity: Activity, amount: Double) {
        // Under maintenance
    }

    fun payWithPayme(activity: Activity, amount: Double) {
        // Under maintenance
    }

    fun payWithPaynet(activity: Activity, amount: Double) {
        // Under maintenance
    }

    fun getCardNumber(): String = "9860160146805207" // Admin kartasi
    fun getCardOwner(): String = "Dushamov T"
}
