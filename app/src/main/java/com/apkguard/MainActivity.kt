package com.apkguard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.core.net.toUri
import com.apkguard.ui.AppNavGraph
import com.apkguard.ui.theme.ApkGuardTheme
import com.apkguard.util.LocaleHelper
import com.apkguard.viewmodel.AuthState
import com.apkguard.viewmodel.AuthViewModel
import com.apkguard.viewmodel.ScanViewModel
import com.apkguard.viewmodel.UserProfile
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        
        currentIntent = intent

        setContent {
            ApkGuardTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val scanViewModel: ScanViewModel = hiltViewModel()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val userProfile by authViewModel.userProfile.collectAsState()

                    AppNavGraph(
                        navController = navController, 
                        viewModel = scanViewModel,
                        authViewModel = authViewModel
                    )

                    LaunchedEffect(currentIntent) {
                        currentIntent?.let { intent ->
                            try {
                                // 1. Auth holati aniqlanishini kutamiz (maksimum 5 soniya)
                                kotlinx.coroutines.withTimeoutOrNull(5000) {
                                    authViewModel.authState.filter { it !is AuthState.Loading && it !is AuthState.Idle }.first()
                                }

                                // 2. Agar foydalanuvchi login qilgan bo'lsa, profil yuklanishini kutamiz (maksimum 5 soniya)
                                if (authViewModel.authState.value is AuthState.Authenticated) {
                                    Log.d("ApkGuard", "Waiting for user profile...")
                                    kotlinx.coroutines.withTimeoutOrNull(5000) {
                                        authViewModel.userProfile.filter { it != null }.first()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ApkGuard", "Error waiting for profile: ${e.message}")
                            }
                            
                            val latestProfile = authViewModel.userProfile.value
                            Log.d("ApkGuard", "Profile loaded: ${latestProfile?.username}, ScanCount: ${latestProfile?.scanCount}")

                            handleIntent(
                                intent = intent,
                                scanViewModel = scanViewModel,
                                authViewModel = authViewModel,
                                userProfile = latestProfile
                            ) { route ->
                                navController.navigate(route) {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                            currentIntent = null
                        }
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }

    private fun handleIntent(
        intent: Intent,
        scanViewModel: ScanViewModel,
        authViewModel: AuthViewModel,
        userProfile: UserProfile?,
        navigate: (String) -> Unit
    ) {
        Log.d("ApkGuard", "Handling intent: ${intent.action}, Data: ${intent.data}")

        val onPermissionRequired = {
            Toast.makeText(this, getString(R.string.error_permissions_required_toast), Toast.LENGTH_LONG).show()
        }

        val onLimitExceeded = {
            Toast.makeText(this, getString(R.string.limit_reached_msg), Toast.LENGTH_LONG).show()
            navigate("subscription")
        }

        val onScanStarted = {
            authViewModel.incrementScanCount()
            navigate("scan")
        }

        // 1. Telegram/Browser orqali APK ochilganda
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                scanViewModel.startScan(
                    uri = uri,
                    userProfile = userProfile,
                    onPermissionRequired = onPermissionRequired,
                    onLimitExceeded = onLimitExceeded,
                    onScanStarted = onScanStarted
                )
            }
        }
        
        // 2. Bizning Notification orqali kelganda
        if (intent.hasExtra("SCAN_APK_URI")) {
            intent.getStringExtra("SCAN_APK_URI")?.let { uriString ->
                scanViewModel.startScan(
                    uri = Uri.parse(uriString),
                    userProfile = userProfile,
                    onPermissionRequired = onPermissionRequired,
                    onLimitExceeded = onLimitExceeded,
                    onScanStarted = onScanStarted
                )
            }
        }
    }
}
