package com.apkguard.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.apkguard.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject

@AndroidEntryPoint
class InstallMonitorService : AccessibilityService() {

    @Inject lateinit var scanManager: RealtimeScanManager
    @Inject lateinit var overlayManager: WarningOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastPackage: String? = null
    private var isOverlayVisible = false
    private var detectionJob: Job? = null
    private var isPremium = false
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    private val bankingKeywords = setOf("payme", "click", "oson", "kapital", "mobile24", "hamkor", "agro", "sqb", "nbu", "uzum")
    private val installerPackages = setOf("com.android.packageinstaller", "com.google.android.packageinstaller", "com.miui.packageinstaller")

    override fun onServiceConnected() {
        super.onServiceConnected()
        NotificationHelper.createNotificationChannels(this)
        startForeground(101, NotificationHelper.buildForegroundNotification(this))
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        userListener = FirebaseFirestore.getInstance().collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                // Firestore'dagi 'premium' maydonini o'qiymiz
                isPremium = snapshot?.getBoolean("premium") ?: false
            }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (isPremium) {
            processAccessibilityEvent(event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
        serviceScope.cancel()
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            
            val packageName = event.packageName?.toString() ?: ""
            
            // 1. Bank ilovalarini ochishda qalqon ko'rsatish
            if (bankingKeywords.any { packageName.lowercase().contains(it) }) {
                showFullScreenProtection()
            }
            
            // 2. APK o'rnatish jarayoni yoki Telegram/Sms-da fayl tahlili
            if (installerPackages.contains(packageName) || 
                packageName.contains("telegram") || 
                packageName.contains("messaging")) {
                detectApkInScroll(rootInActiveWindow)
            }
        }
    }

    private fun showFullScreenProtection() {
        if (isOverlayVisible) return
        isOverlayVisible = true
        overlayManager.showBankShieldOverlay(
            onScan = { performDeepScan() },
            onSkip = { isOverlayVisible = false }
        )
    }

    private fun detectApkInScroll(node: AccessibilityNodeInfo?) {
        if (node == null || isOverlayVisible) return
        
        // Telegram muloqotlaridagi APK nomlarini va "Install" tugmalarini tekshirish
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        val className = node.className?.toString() ?: ""
        
        // APK fayl nomi yoki o'rnatish tugmasi (masalan Telegramda "Install")
        if ((text != null && text.endsWith(".apk", ignoreCase = true)) || 
            (className.contains("Button") && text?.contains("Install", ignoreCase = true) == true)) {
            showFullScreenProtection()
            return
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            detectApkInScroll(child)
            child.recycle() 
        }
    }

    private fun performDeepScan() {
        serviceScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { overlayManager.showScanningProgress() }
            
            val pm = packageManager
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcesses = am.runningAppProcesses ?: emptyList()
            val runningPackages = runningAppProcesses.map { it.processName }.toSet()

            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val suspiciousApps = mutableListOf<WarningOverlayManager.SuspiciousApp>()

            // OTP o'g'irlovchi ilovalarni chuqur skanerlash (Fonda ishlayotganlar)
            installedApps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM == 0) && it.packageName != packageName }
                .forEach { app ->
                    try {
                        val info = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                        val perms = info.requestedPermissions?.toSet() ?: emptySet()
                        
                        // Bank viruslarining klassik xususiyatlari: SMS + Fonda ishlash
                        val hasSms = perms.contains("android.permission.RECEIVE_SMS") || perms.contains("android.permission.READ_SMS")
                        val isRunning = runningPackages.contains(app.packageName)

                        if (hasSms && isRunning) {
                            // Agar SMS ruxsati bor va u fonda ishlayotgan bo'olsa, kodni tezkor skanerlaymiz
                            if (checkCodeFast(app.sourceDir)) {
                                suspiciousApps.add(WarningOverlayManager.SuspiciousApp(
                                    app.loadLabel(pm).toString(), 
                                    "OTP O'G'IRLOVCHI: Fonda ishlayapti va shubhali kod topildi!"
                                ))
                            }
                        }
                    } catch (e: Exception) {}
                }

            withContext(Dispatchers.Main) {
                overlayManager.showScanResult(suspiciousApps.size, suspiciousApps) { isOverlayVisible = false }
            }
        }
    }

    private fun checkCodeFast(sourceDir: String): Boolean {
        return try {
            val file = File(sourceDir)
            val otpKeywords = arrayOf("SMS_RECEIVED", "getMessages", "pdus", "SmsMessage")
            var matchCount = 0

            ZipInputStream(file.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".dex")) {
                        val content = String(zip.readBytes(), Charsets.ISO_8859_1)
                        for (key in otpKeywords) {
                            if (content.contains(key)) matchCount++
                        }
                        if (matchCount >= 2) return true
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            false
        } catch (e: Exception) { false }
    }

    private fun isUninstallOrSystemAction(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        val text = node.text?.toString()?.lowercase() ?: node.contentDescription?.toString()?.lowercase()
        // O'chirishga oid kalit so'zlar (O'zbek, Rus, Ingliz)
        val uninstallKeywords = setOf("uninstall", "o'chirish", "ochirish", "удалить", "удаление", "clear data", "ma'lumotlarni tozalash")
        
        if (text != null) {
            for (key in uninstallKeywords) {
                if (text.contains(key)) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = isUninstallOrSystemAction(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    override fun onInterrupt() {}
}
