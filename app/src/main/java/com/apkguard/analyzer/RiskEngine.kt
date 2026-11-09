package com.apkguard.analyzer

import android.content.Context
import com.apkguard.R
import com.apkguard.model.*
import com.apkguard.security.SmartRiskEngine

class RiskEngine(private val context: Context) {

    data class RiskAssessment(
        val score: Int,
        val level: RiskLevel,
        val reasons: List<String>
    )

    fun assess(
        apkFile: java.io.File,
        manifestData: ManifestAnalyzer.ManifestData,
        dexResult: DexAnalyzer.DexAnalysisResult,
        signatureInfo: SignatureInfo?,
        unzipFailedEntries: Boolean = false
    ): RiskAssessment {
        val packageName = manifestData.packageName
        val permissions = manifestData.permissions.map { it.name }
        
        // SmartRiskEngine orqali hisoblash
        val result = SmartRiskEngine.calculate(
            packageName = packageName,
            permissions = permissions,
            apkFile = apkFile,
            apkSizeMb = apkFile.length() / (1024f * 1024f),
            hasValidSignature = signatureInfo?.isDebugSigned == false,
            installSource = "unknown"
        )

        val reasons = result.details.map { "${it.category}: ${it.description}" }.toMutableList()
        var finalScore = result.totalScore

        // Unzip muammosi (shifrlangan entrylar) virus belgisi bo'lishi mumkin
        if (unzipFailedEntries) {
            finalScore += 60 // Shifrlangan fayllar uchun juda katta jarima
            reasons.add("KRITIK XAVF: APK ichidagi fayllar shifrlangan yoki himoyalangan. Bu odatda viruslar o'z kodini yashirish va tahlildan qochish uchun foydalanadigan usuldir.")
        }
        
        // Dex tahlil natijalarini qo'shish
        dexResult.codePatterns.forEach { pattern ->
            if (!result.isWhitelisted) {
                reasons.add("Kod tahlili: ${pattern.name} aniqlandi")
            }
        }

        // Agar skan qilib bo'lmaydigan darajada shubhali bo'lsa yoki shifrlangan bo'lsa va boshqa xavf belgilari bo'lsa
        if (unzipFailedEntries && (finalScore >= 80 || result.isBlacklisted)) {
            finalScore = 100
        }

        // Agar hech qanday DEX fayl topilmasa, lekin APK katta bo'lsa - bu ham juda shubhali
        if (dexResult.codePatterns.isEmpty() && unzipFailedEntries) {
            finalScore = 100
        }

        return RiskAssessment(
            score = finalScore.coerceAtMost(100),
            level = RiskLevel.fromScore(finalScore),
            reasons = reasons.distinct()
        )
    }
}
