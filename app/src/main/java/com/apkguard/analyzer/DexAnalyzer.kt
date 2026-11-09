package com.apkguard.analyzer

import android.util.Log
import com.apkguard.model.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DexAnalyzer {

    companion object {
        private const val TAG = "DexAnalyzer"
        private const val CHUNK_SIZE = 65536

        private val URL_REGEX = Regex(
            """https?://[a-zA-Z0-9\-._~:/?#\[\]@!${'$'}&'()*+,;=%]{4,200}""",
            RegexOption.IGNORE_CASE
        )

        private val FINANCIAL_FRAUD_DOMAINS = setOf(
            "ngrok.io", "serveo.net", "localtunnel.me", "pagekite.me",
            "duckdns.org", "no-ip.com", "ddns.net"
        )

        private val PAYLOAD_HOSTS = setOf(
            "pastebin.com", "raw.githubusercontent", "bit.ly", "t.co", "tinyurl.com"
        )

        private val CODE_PATTERNS = listOf(
            PatternDef(
                name = "WebView.loadUrl",
                regex = Regex("""loadUrl\s*\("""),
                description = "Loads URLs in WebView — potential for phishing or JS injection",
                severity = PatternSeverity.WARNING
            ),
            PatternDef(
                name = "Dynamic Class Loading",
                regex = Regex("""DexClassLoader|PathClassLoader|dalvik\.system"""),
                description = "Loads code dynamically — may hide malicious payloads",
                severity = PatternSeverity.DANGER
            ),
            PatternDef(
                name = "Runtime.exec()",
                regex = Regex("""Runtime\.getRuntime\(\)\.exec|ProcessBuilder"""),
                description = "Executes shell commands — high risk behavior",
                severity = PatternSeverity.DANGER
            ),
            PatternDef(
                name = "SMS Operations",
                regex = Regex("""SmsManager|sendTextMessage|sendMultipartTextMessage"""),
                description = "Sends SMS messages — may cause financial charges",
                severity = PatternSeverity.DANGER
            ),
            PatternDef(
                name = "Accessibility Service",
                regex = Regex("""AccessibilityService|onAccessibilityEvent|performGlobalAction"""),
                description = "Uses accessibility to read/interact with UI — common in banking trojans",
                severity = PatternSeverity.DANGER
            ),
            PatternDef(
                name = "Overlay Attack Pattern",
                regex = Regex("""TYPE_APPLICATION_OVERLAY|TYPE_SYSTEM_ALERT|SYSTEM_ALERT_WINDOW"""),
                description = "Draws overlays over other apps — common in credential theft",
                severity = PatternSeverity.DANGER
            ),
            PatternDef(
                name = "OTP/SMS Interception",
                regex = Regex("""SMS_RECEIVED|pdus|getMessagesFromIntent|otp|password|verification"""),
                description = "Intercepts SMS messages to extract OTP or passwords",
                severity = PatternSeverity.DANGER
            )
        )

        private val MALWARE_KEYWORDS = listOf(
            MalwareKeyword("keylogger", "Keylogger", PatternSeverity.DANGER),
            MalwareKeyword("trojan", "Trojan", PatternSeverity.DANGER),
            MalwareKeyword("exfiltrate", "Data Exfiltration", PatternSeverity.DANGER),
            MalwareKeyword("steal_credential", "Credential Theft", PatternSeverity.DANGER),
            MalwareKeyword("intercept_sms", "SMS Interception", PatternSeverity.DANGER),
            MalwareKeyword("phishing", "Phishing", PatternSeverity.DANGER),
            MalwareKeyword("rat", "Remote Access Trojan", PatternSeverity.DANGER),
            MalwareKeyword("spyware", "Spyware", PatternSeverity.DANGER)
        )
    }

    data class PatternDef(val name: String, val regex: Regex, val description: String, val severity: PatternSeverity)
    data class MalwareKeyword(val keyword: String, val category: String, val severity: PatternSeverity)
    data class DexAnalysisResult(val foundUrls: List<FoundUrl>, val codePatterns: List<CodePattern>, val malwareHits: List<MalwareHit>)

    suspend fun analyze(dexFile: File): DexAnalysisResult = withContext(Dispatchers.IO) {
        val content = readDexAsText(dexFile)
        DexAnalysisResult(
            foundUrls = extractUrls(content),
            codePatterns = detectPatterns(content),
            malwareHits = detectMalwareKeywords(content)
        )
    }

    private fun readDexAsText(file: File): String {
        return try {
            val sb = StringBuilder(file.length().coerceAtMost(5_000_000L).toInt())
            file.inputStream().bufferedReader(Charsets.ISO_8859_1).use { reader ->
                val buf = CharArray(CHUNK_SIZE)
                var read: Int
                while (reader.read(buf).also { read = it } != -1) {
                    for (i in 0 until read) {
                        val c = buf[i]
                        if (c.code in 0x20..0x7E || c == '\n') sb.append(c)
                    }
                    if (sb.length > 4_000_000) break
                }
            }
            sb.toString()
        } catch (e: Exception) { "" }
    }

    private fun extractUrls(content: String): List<FoundUrl> {
        val found = mutableMapOf<String, FoundUrl>()
        URL_REGEX.findAll(content).forEach { match ->
            val url = match.value.trimEnd('.', ',', ')', ']', '"', '\'')
            if (!found.containsKey(url)) {
                val classification = classifyUrl(url)
                if (classification.risk != UrlRisk.SAFE) found[url] = classification
            }
        }
        return found.values.toList()
    }

    private fun classifyUrl(url: String): FoundUrl {
        val lower = url.lowercase()
        return when {
            FINANCIAL_FRAUD_DOMAINS.any { lower.contains(it) } -> FoundUrl(url, UrlRisk.MALICIOUS, "Fraud domain")
            PAYLOAD_HOSTS.any { lower.contains(it) } -> FoundUrl(url, UrlRisk.MALICIOUS, "Hidden payload source")
            else -> FoundUrl(url, UrlRisk.SAFE)
        }
    }

    private fun detectPatterns(content: String): List<CodePattern> {
        return CODE_PATTERNS.mapNotNull { pattern ->
            val matches = pattern.regex.findAll(content).count()
            if (matches > 0) CodePattern(pattern.name, pattern.description, pattern.severity, matches) else null
        }
    }

    private fun detectMalwareKeywords(content: String): List<MalwareHit> {
        val lower = content.lowercase()
        return MALWARE_KEYWORDS.mapNotNull { kw ->
            if (lower.contains(kw.keyword)) MalwareHit(kw.keyword, kw.category, kw.severity) else null
        }
    }
}
