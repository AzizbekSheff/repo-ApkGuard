package com.apkguard.analyzer

import android.content.pm.PackageInfo
import android.content.pm.ApplicationInfo
import com.apkguard.model.*

class ManifestAnalyzer {

    companion object {
        private val DANGEROUS_PERMISSIONS = mapOf(
            "READ_SMS" to "SMS xabarlarni o'qish",
            "RECEIVE_SMS" to "Kiruvchi SMSlarni tutib olish",
            "SEND_SMS" to "SMS yuborish",
            "READ_CONTACTS" to "Kontaktlarni o'qish",
            "INTERNET" to "Internetga chiqish",
            "SYSTEM_ALERT_WINDOW" to "Boshqa ilovalar ustidan chizish",
            "BIND_ACCESSIBILITY_SERVICE" to "Maxsus imkoniyatlar xizmati",
            "REQUEST_INSTALL_PACKAGES" to "Ilovalarni o'rnatish so'rovi"
        )
    }

    data class ManifestData(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdkVersion: String,
        val targetSdkVersion: String,
        val permissions: List<PermissionInfo>,
        val hasDebuggable: Boolean,
        val hasAllowBackup: Boolean,
        val exportedComponents: List<String>
    )

    fun convert(packageInfo: PackageInfo): ManifestData {
        val appInfo = packageInfo.applicationInfo
        
        val permissions = packageInfo.requestedPermissions?.map { perm ->
            val simpleName = perm.substringAfterLast(".")
            PermissionInfo(
                name = perm,
                simpleName = simpleName,
                danger = if (DANGEROUS_PERMISSIONS.containsKey(simpleName)) PermissionDanger.DANGEROUS else PermissionDanger.NORMAL,
                description = DANGEROUS_PERMISSIONS[simpleName] ?: "Standart ruxsatnoma"
            )
        } ?: emptyList()

        return ManifestData(
            packageName = packageInfo.packageName ?: "unknown",
            versionName = packageInfo.versionName ?: "1.0",
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            },
            minSdkVersion = appInfo?.minSdkVersion?.toString() ?: "N/A",
            targetSdkVersion = appInfo?.targetSdkVersion?.toString() ?: "N/A",
            permissions = permissions,
            hasDebuggable = (appInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            hasAllowBackup = (appInfo?.flags ?: 0) and ApplicationInfo.FLAG_ALLOW_BACKUP != 0,
            exportedComponents = emptyList() // Buni olish uchun murakkabroq tahlil kerak
        )
    }
}
