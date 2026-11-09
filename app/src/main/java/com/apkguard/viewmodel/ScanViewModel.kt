package com.apkguard.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkguard.analyzer.ApkScanner
import com.apkguard.model.*
import com.apkguard.repository.ScanRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: ScanRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val auth = FirebaseAuth.getInstance()

    // ── Scan state ────────────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // ── History ───────────────────────────────────────────────────────────────
    val history: StateFlow<List<ScanHistoryEntry>> = repository.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Installed Apps ────────────────────────────────────────────────────────
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    // ── Detail result for history view ────────────────────────────────────────
    private val _detailResult = MutableStateFlow<ScanResult?>(null)
    val detailResult: StateFlow<ScanResult?> = _detailResult.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = repository.getInstalledApps()
        }
    }

    fun startScan(
        uri: Uri,
        userProfile: UserProfile?,
        onPermissionRequired: () -> Unit,
        onLimitExceeded: () -> Unit,
        onScanStarted: () -> Unit
    ) {
        val isOverlayEnabled = android.provider.Settings.canDrawOverlays(context)
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(context)

        if (!isOverlayEnabled || !isAccessibilityEnabled) {
            onPermissionRequired()
            return
        }

        viewModelScope.launch {
            // 1. Limitni tekshirish (History + LocalPrefs + Cloud)
            // Telegramdan kelganda bypass bo'lmasligi uchun barcha manbalarni tekshiramiz
            val historyCount = repository.getHistory().first().size
            val localCount = maxOf(repository.getLocalScanCount(), historyCount)
            
            val isPremium = userProfile?.premium ?: false
            val cloudCount = userProfile?.scanCount ?: 0
            
            // Ikkala joydan olingan eng katta qiymatni hisobga olamiz
            val effectiveCount = maxOf(cloudCount, localCount)

            android.util.Log.d("ApkGuard", "Limit Check: Cloud=$cloudCount, Local=$localCount, Total=$effectiveCount, Premium=$isPremium")

            if (!isPremium && effectiveCount >= 3) {
                onLimitExceeded()
                return@launch
            }

            // 2. Mahalliy limitni darhol oshiramiz (poyga holatini oldini olish uchun)
            repository.incrementLocalScanCount()
            
            // Skanerlashni boshlash
            onScanStarted()
            _scanState.value = ScanState.Scanning(context.getString(com.apkguard.R.string.scanning), 0f)

            repository.scanApk(uri).collect { progress ->
                when (progress) {
                    is ApkScanner.Progress.Step ->
                        _scanState.value = ScanState.Scanning(progress.message, progress.fraction)
                    is ApkScanner.Progress.Result -> {
                        _scanState.value = ScanState.Done(progress.result)
                        
                        // Firebase Analytics-ga ma'lumot yuborish
                        analytics.logEvent("apk_scan_completed") {
                            param("risk_score", progress.result.riskScore.toLong())
                            param("is_dangerous", if (progress.result.riskScore > 60) 1L else 0L)
                            param("user_id", auth.currentUser?.uid ?: "anonymous")
                        }
                    }
                    is ApkScanner.Progress.Failure -> {
                        _scanState.value = ScanState.Error(progress.error)
                        analytics.logEvent("apk_scan_failed") {
                            param("error_message", progress.error)
                        }
                    }
                }
            }
        }
    }

    fun saveDecision(result: ScanResult, decision: UserDecision) {
        viewModelScope.launch {
            repository.saveResult(result, decision)
            _scanState.value = ScanState.Idle
        }
    }

    fun loadDetailResult(id: Long) {
        viewModelScope.launch {
            _detailResult.value = repository.getScanResult(id)
        }
    }

    fun updateDecision(id: Long, decision: UserDecision) {
        viewModelScope.launch {
            repository.updateDecision(id, decision)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedId = "${context.packageName}/${context.packageName}.service.InstallMonitorService"
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.split(':')?.any { it.equals(expectedId, ignoreCase = true) } == true
    }
}
