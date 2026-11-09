package com.apkguard.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.apkguard.analyzer.ApkScanner
import com.apkguard.data.entity.BlacklistDao
import com.apkguard.data.entity.BlacklistEntity
import com.apkguard.data.entity.ScanHistoryDao
import com.apkguard.data.entity.ScanHistoryEntity
import com.apkguard.model.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScanHistoryDao,
    private val blacklistDao: BlacklistDao,
    private val gson: Gson
) {
    private val scanner = ApkScanner(context)
    private val prefs = context.getSharedPreferences("scan_settings", Context.MODE_PRIVATE)

    fun getLocalScanCount(): Int = prefs.getInt("total_scan_count", 0)

    fun incrementLocalScanCount() {
        val current = getLocalScanCount()
        prefs.edit().putInt("total_scan_count", current + 1).apply()
    }

    fun scanApk(uri: Uri): Flow<ApkScanner.Progress> = flow {
        // 1. Blacklist tekshirish
        val packageName = getPackageNameFromUri(uri)
        if (packageName != null && blacklistDao.isBlacklisted(packageName)) {
            emit(ApkScanner.Progress.Failure("Ushbu ilova qora ro'yxatda! U zararli deb topilgan."))
            return@flow
        }

        scanner.scan(uri).collect { emit(it) }
    }

    private fun getPackageNameFromUri(uri: Uri): String? {
        return try {
            val pm = context.packageManager
            val apkFile = copyUriToTempFile(uri)
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            val pkg = info?.packageName
            apkFile.delete()
            pkg
        } catch (e: Exception) {
            null
        }
    }

    private fun copyUriToTempFile(uri: Uri): java.io.File {
        val tempFile = java.io.File(context.cacheDir, "temp_${System.currentTimeMillis()}.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    suspend fun addToBlacklist(result: ScanResult) {
        blacklistDao.insert(
            BlacklistEntity(
                packageName = result.packageName,
                appName = result.apkName,
                reason = result.riskReasons.joinToString("; ")
            )
        )
    }

    suspend fun isBlacklisted(packageName: String): Boolean {
        return blacklistDao.isBlacklisted(packageName)
    }

    fun getHistory(): Flow<List<ScanHistoryEntry>> =
        dao.getAllHistory().map { list ->
            list.map { it.toEntry() }
        }

    fun getCriticalThreats(): Flow<List<ScanHistoryEntry>> =
        dao.getCriticalThreats().map { list ->
            list.map { it.toEntry() }
        }

    suspend fun saveResult(result: ScanResult, decision: UserDecision): Long {
        val entity = ScanHistoryEntity(
            apkName = result.apkName,
            packageName = result.packageName,
            riskScore = result.riskScore,
            riskLevel = result.riskLevel.name,
            decision = decision.name,
            scannedAt = result.scannedAt,
            resultJson = gson.toJson(result)
        )
        return dao.insert(entity)
    }

    suspend fun updateDecision(id: Long, decision: UserDecision) {
        dao.updateDecision(id, decision.name)
    }

    suspend fun getScanResult(id: Long): ScanResult? {
        return dao.getById(id)?.let {
            gson.fromJson(it.resultJson, ScanResult::class.java)
        }
    }

    suspend fun deleteHistory(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAllHistory() {
        dao.clearAll()
    }

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        apps.filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || 
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }.mapNotNull { appInfo ->
            try {
                val packageInfo = pm.getPackageInfo(appInfo.packageName, 0)
                InstalledApp(
                    name = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    sourceDir = appInfo.sourceDir,
                    versionName = packageInfo.versionName ?: "1.0",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name }
    }

    private fun ScanHistoryEntity.toEntry() = ScanHistoryEntry(
        id = id,
        apkName = apkName,
        packageName = packageName,
        riskScore = riskScore,
        riskLevel = RiskLevel.valueOf(riskLevel),
        decision = UserDecision.valueOf(decision),
        scannedAt = scannedAt
    )
}
