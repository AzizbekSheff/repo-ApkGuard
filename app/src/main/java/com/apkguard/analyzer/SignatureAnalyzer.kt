package com.apkguard.analyzer

import android.content.Context
import android.util.Log
import com.apkguard.model.SignatureInfo
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

class SignatureAnalyzer(private val context: Context) {

    companion object {
        private const val TAG = "SignatureAnalyzer"
        private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        private val DEBUG_SUBJECTS = setOf(
            "cn=android debug", "cn=unknown", "o=unknown", "ou=unknown"
        )
    }

    fun analyze(apkFile: File): SignatureInfo? {
        return try {
            analyzeSignature(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Signature analysis failed", e)
            null
        }
    }

    private fun analyzeSignature(apkFile: File): SignatureInfo {
        val jar = JarFile(apkFile, true)
        var cert: X509Certificate? = null

        try {
            val manifestEntry: JarEntry? = jar.getJarEntry("AndroidManifest.xml")
            if (manifestEntry != null) {
                jar.getInputStream(manifestEntry).use { it.readBytes() }
                val certs = manifestEntry.certificates
                if (!certs.isNullOrEmpty()) {
                    cert = certs[0] as? X509Certificate
                }
            }
        } finally {
            jar.close()
        }

        var installerSource = "Tashqi fayl (Offline APK)"
        
        if (cert == null) {
            return SignatureInfo(
                isDebugSigned = false,
                isSelfSigned = true,
                certMd5 = "N/A",
                issuer = "Topilmadi",
                subject = "N/A",
                validFrom = "N/A",
                validUntil = "N/A",
                isExpired = false,
                installerSource = installerSource,
                developerName = "Noma'lum (Imzosiz)"
            )
        }

        val subject = cert.subjectDN.name
        val issuer = cert.issuerDN.name
        val isSelfSigned = subject.equals(issuer, ignoreCase = true)
        val isDebugSigned = DEBUG_SUBJECTS.any { subject.lowercase().contains(it) }
        val isExpired = cert.notAfter?.before(Date()) ?: false

        // Detallashgan ma'lumotlarni ajratib olish
        val dnParts = parseDN(subject)
        val devName = dnParts["CN"] ?: "Noma'lum"
        val org = dnParts["O"] ?: dnParts["OU"]
        val country = dnParts["C"]

        val md5 = try {
            val md = MessageDigest.getInstance("MD5")
            md.update(cert.encoded)
            md.digest().joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) { "N/A" }

        return SignatureInfo(
            isDebugSigned = isDebugSigned,
            isSelfSigned = isSelfSigned,
            certMd5 = md5,
            issuer = issuer,
            subject = subject,
            validFrom = cert.notBefore?.let { DATE_FMT.format(it) } ?: "N/A",
            validUntil = cert.notAfter?.let { DATE_FMT.format(it) } ?: "N/A",
            isExpired = isExpired,
            installerSource = installerSource,
            developerName = devName,
            organization = org,
            country = country
        )
    }

    private fun parseDN(dn: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val parts = dn.split(",")
            for (part in parts) {
                val kv = part.split("=")
                if (kv.size == 2) {
                    map[kv[0].trim().uppercase()] = kv[1].trim()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DN parsing error", e)
        }
        return map
    }
}
