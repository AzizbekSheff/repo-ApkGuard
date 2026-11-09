package com.apkguard.model

import androidx.compose.ui.graphics.Color

// ─── Risk Level ────────────────────────────────────────────────────────────────

enum class RiskLevel(val label: String, val score: Int) {
    LOW("Low Risk", 0),
    MEDIUM("Medium Risk", 1),
    HIGH("High Risk", 2),
    CRITICAL("Critical Risk", 3);

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score >= 80 -> CRITICAL
            score >= 55 -> HIGH
            score >= 30 -> MEDIUM
            else -> LOW
        }
    }
}

// ─── Permission Models ─────────────────────────────────────────────────────────

enum class PermissionDanger { NORMAL, DANGEROUS, SIGNATURE, UNKNOWN }

data class PermissionInfo(
    val name: String,
    val simpleName: String,
    val danger: PermissionDanger,
    val description: String
)

// ─── URL Finding ───────────────────────────────────────────────────────────────

enum class UrlRisk { SAFE, SUSPICIOUS, MALICIOUS }

data class FoundUrl(
    val url: String,
    val risk: UrlRisk,
    val reason: String = "",
    val ipAddress: String? = null,    // Server IP manzili
    val location: String? = null,     // Server joylashuvi (davlat)
    val latitude: Double? = null,     // Xarita uchun
    val longitude: Double? = null     // Xarita uchun
)

// ─── Code Pattern ──────────────────────────────────────────────────────────────

enum class PatternSeverity { INFO, WARNING, DANGER }

data class CodePattern(
    val name: String,
    val description: String,
    val severity: PatternSeverity,
    val occurrences: Int
)

// ─── Signature Info ────────────────────────────────────────────────────────────

data class SignatureInfo(
    val isDebugSigned: Boolean,
    val isSelfSigned: Boolean,
    val certMd5: String,
    val issuer: String,
    val subject: String,
    val validFrom: String,
    val validUntil: String,
    val isExpired: Boolean,
    val installerSource: String = "Unknown", // Qayerdan o'rnatilgani
    val developerName: String = "Noma'lum",  // Kim tomonidan yozilgani
    val organization: String? = null,        // Tashkilot nomi
    val country: String? = null             // Davlat (Imzodagi)
)

// ─── Malware Keywords ──────────────────────────────────────────────────────────

data class MalwareHit(
    val keyword: String,
    val category: String,
    val severity: PatternSeverity
)

// ─── Device Info ───────────────────────────────────────────────────────────────

data class DeviceInfo(
    val ipAddress: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?
)

// ─── Full Scan Result ──────────────────────────────────────────────────────────

data class ScanResult(
    val apkName: String,
    val apkPath: String,
    val apkSizeBytes: Long,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: String,
    val targetSdkVersion: String,
    val permissions: List<PermissionInfo>,
    val foundUrls: List<FoundUrl>,
    val codePatterns: List<CodePattern>,
    val malwareHits: List<MalwareHit>,
    val signatureInfo: SignatureInfo?,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val riskReasons: List<String>,
    val scanDurationMs: Long,
    val scannedAt: Long = System.currentTimeMillis()
)

// ─── Scan State ────────────────────────────────────────────────────────────────

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val step: String, val progress: Float) : ScanState()
    data class Done(val result: ScanResult) : ScanState()
    data class Error(val message: String) : ScanState()
}

// ─── History Entry ─────────────────────────────────────────────────────────────

enum class UserDecision { APPROVED, REJECTED, PENDING }

data class ScanHistoryEntry(
    val id: Long = 0,
    val apkName: String,
    val packageName: String,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val decision: UserDecision,
    val scannedAt: Long
)
