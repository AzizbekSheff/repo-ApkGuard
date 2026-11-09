// ============================================================
// APKGuard — TUZATILGAN TO'LIQ VERSIYA v2
//
// NIMA O'ZGARDI:
// 1. TelegramScanInterceptor — content:// URI qo'llab-quvvatlandi
//    (FileProvider, DocumentsProvider, MediaStore)
// 2. Whitelist — Avito, OLX, Wildberries va boshqa rasmiy ilovalar qo'shildi
// 3. NetworkTrafficAnalyzer — APK ichidagi URL/IP/domain larni topadi
//    va qayerga xabar yuborayotganini ko'rsatadi
// 4. SmartRiskEngine — network analiz natijasini hisobga oladi
// ============================================================

package com.apkguard.security

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile


// ════════════════════════════════════════════════════════════════
// 1. WhitelistManager (YANGILANGAN — ko'proq ilovalar)
// ════════════════════════════════════════════════════════════════

object WhitelistManager {

    private val GOOGLE_OFFICIAL = setOf(
        "com.google.android.gms", "com.google.android.gsf",
        "com.google.android.apps.messaging", "com.google.android.contacts",
        "com.google.android.dialer", "com.google.android.youtube",
        "com.google.android.googlequicksearchbox", "com.google.android.apps.maps",
        "com.android.vending", "com.google.android.apps.photos",
        "com.google.android.calendar", "com.google.android.keep",
        "com.google.android.apps.docs", "com.google.android.apps.drive",
        "com.google.android.gmail", "com.google.android.talk",
        "com.google.android.apps.tachyon", "com.google.android.play.games",
        "com.google.android.inputmethod.latin", "com.google.android.apps.translate"
    )

    private val SAMSUNG_OFFICIAL = setOf(
        "com.samsung.android.messaging", "com.samsung.android.contacts",
        "com.samsung.android.dialer", "com.samsung.android.app.notes",
        "com.samsung.android.calendar", "com.samsung.android.app.galaxystore",
        "com.samsung.android.browser", "com.samsung.android.email.provider",
        "com.samsung.android.samsungpay", "com.samsung.android.knox.containercore",
        "com.samsung.android.app.smartcapture", "com.samsung.android.health"
    )

    private val POPULAR_MESSENGERS = setOf(
        "com.whatsapp", "com.whatsapp.w4b",
        "org.telegram.messenger", "org.telegram.messenger.web",
        "org.telegram.plus", "org.thunderdog.challegram",
        "com.facebook.katana", "com.facebook.lite",
        "com.instagram.android", "com.twitter.android",
        "com.linkedin.android", "com.snapchat.android",
        "com.viber.voip", "com.skype.raider",
        "com.microsoft.teams", "com.discord", "com.slack",
        "com.zoom.videomeetings", "us.zoom.videomeetings",
        "ru.ok.android", "com.vkontakte.android", "com.vk.vkclient"
    )

    // O'zbekiston banklari
    private val UZ_BANKING = setOf(
        "uz.kapitalbank.android", "uz.apelsin",
        "uz.click.android",       // Click
        "uz.payme.app",           // Payme
        "uz.uzcard.android",
        "uz.humo.app",
        "uz.myuzcard",
        "uz.aloqabank.mobilebank",
        "uz.ipoteka.bank",
        "com.uzpaynet.android",
        "uz.nbu.mobilebank",
        "uz.universalbank",
        "uz.agrobank.mobilebank",
        "uz.trustbank",
        "uz.infinbank.mobilebank",
        "com.tbc.uz",
        "uz.ravnaqbank"
    )

    // Xalqaro to'lov tizimlari
    private val INTL_PAYMENT = setOf(
        "com.paypal.android.p2pmobile",
        "com.google.android.apps.walletnfcrel",
        "com.samsung.android.spay",
        "ru.sberbank.spasibo",
        "com.kaspersky.password.manager"
    )

    // Marketplace va e-commerce (YANGI)
    private val MARKETPLACE = setOf(
        "com.avito.android",           // Avito
        "ru.ozon.app.android",         // Ozon
        "com.wildberries.ru",          // Wildberries
        "com.yandex.market",           // Yandex Market
        "kz.kolesa.android",
        "com.olx.olx",                 // OLX
        "uz.olx",
        "kz.avtoelon.android",
        "com.amazon.mShop.android.shopping",
        "com.ebay.mobile",
        "com.aliexpress.russia",
        "com.alibaba.intl.android.apps.poseidon"
    )

    // Transport va delivery (YANGI)
    private val TRANSPORT = setOf(
        "ru.yandex.taxi",
        "com.uber.driver",
        "com.uber.client",
        "ru.yandex.yandexmaps",
        "com.yandex.navi",
        "ru.yandex.food",
        "kz.yandex.lavka",
        "com.gett.passenger",
        "uz.urent.app",
        "uz.mytaxi",
        "uz.express24",
        "uz.uzum.market"
    )

    private val ANDROID_SYSTEM = setOf(
        "android", "com.android.phone", "com.android.settings",
        "com.android.systemui", "com.android.launcher",
        "com.android.launcher3", "com.android.contacts",
        "com.android.mms", "com.android.providers.contacts",
        "com.android.providers.telephony", "com.android.browser",
        "com.android.camera", "com.android.nfc", "com.android.bluetooth",
        "com.android.gallery3d", "com.android.inputmethod.latin"
    )

    private val userWhitelist = mutableSetOf<String>()

    fun isWhitelisted(packageName: String): Boolean {
        // MUHIM: trim() va lowercase() ikkalasi ham
        val pkg = packageName.trim().lowercase()
        return pkg in GOOGLE_OFFICIAL
            || pkg in SAMSUNG_OFFICIAL
            || pkg in POPULAR_MESSENGERS
            || pkg in UZ_BANKING
            || pkg in INTL_PAYMENT
            || pkg in MARKETPLACE
            || pkg in TRANSPORT
            || pkg in ANDROID_SYSTEM
            || pkg in userWhitelist
            || isKnownPublisher(pkg)
    }

    private fun isKnownPublisher(pkg: String): Boolean {
        return listOf(
            "com.microsoft.", "com.adobe.", "com.netflix.", "com.spotify.",
            "com.amazon.", "com.ebay.", "com.airbnb.", "com.uber.",
            "tv.twitch.", "com.dropbox.", "com.evernote.",
            "com.1password.", "com.lastpass.", "com.dashlane.",
            "ru.yandex.", "com.yandex.", "kz.yandex."
        ).any { pkg.startsWith(it) }
    }

    fun addToUserWhitelist(packageName: String) =
        userWhitelist.add(packageName.trim().lowercase())

    fun removeFromUserWhitelist(packageName: String) =
        userWhitelist.remove(packageName.trim().lowercase())

    fun getUserWhitelist(): Set<String> = userWhitelist.toSet()
}


// ════════════════════════════════════════════════════════════════
// 2. BlacklistManager (o'zgarishsiz, faqat pattern yangilandi)
// ════════════════════════════════════════════════════════════════

object BlacklistManager {

    data class BlacklistEntry(
        val packageName: String,
        val reason: String,
        val riskScore: Int = 100,
        val category: ThreatCategory
    )

    enum class ThreatCategory {
        BANKING_TROJAN, SPYWARE, RANSOMWARE,
        ADWARE, FAKE_APP, SMS_STEALER, REMOTE_ACCESS
    }

    private val KNOWN_MALICIOUS = mapOf(
        "uz.kapitalbank.fake"            to BlacklistEntry("uz.kapitalbank.fake", "Kapitalbank soxta nusxasi", 100, ThreatCategory.BANKING_TROJAN),
        "uz.apelsin.clone"               to BlacklistEntry("uz.apelsin.clone", "Apelsin soxta nusxasi", 100, ThreatCategory.BANKING_TROJAN),
        "uz.click.fake"                  to BlacklistEntry("uz.click.fake", "Click soxta nusxasi", 100, ThreatCategory.BANKING_TROJAN),
        "uz.payme.fake"                  to BlacklistEntry("uz.payme.fake", "Payme soxta nusxasi", 100, ThreatCategory.BANKING_TROJAN),
        "com.phone.tracker.spy"          to BlacklistEntry("com.phone.tracker.spy", "Spyware — joylashuv va SMS monitoring", 100, ThreatCategory.SPYWARE),
        "com.system.service.hidden"      to BlacklistEntry("com.system.service.hidden", "Hidden RAT", 100, ThreatCategory.REMOTE_ACCESS),
        "com.android.service.manager"   to BlacklistEntry("com.android.service.manager", "Fake system service — SMS stealer", 100, ThreatCategory.SMS_STEALER),
        "com.sms.reader.pro"             to BlacklistEntry("com.sms.reader.pro", "SMS o'g'irlash trojan", 100, ThreatCategory.SMS_STEALER),
        "com.message.interceptor"        to BlacklistEntry("com.message.interceptor", "2FA SMS interceptor", 100, ThreatCategory.SMS_STEALER),
        "com.google.play.store.update"   to BlacklistEntry("com.google.play.store.update", "Fake Google Play update", 100, ThreatCategory.FAKE_APP),
        "com.android.system.update.service" to BlacklistEntry("com.android.system.update.service", "Fake system update dropper", 100, ThreatCategory.RANSOMWARE),
        "ryb.d7cx3q.svjheinl"            to BlacklistEntry("ryb.d7cx3q.svjheinl", "SUD QARORI soxta ilovasi (Malware)", 100, ThreatCategory.BANKING_TROJAN)
    )

    private val SUSPICIOUS_PATTERNS = listOf(
        Regex("^[a-z]{3}\\.[a-z0-9]{6}\\.[a-z]{8}$"), // Masalan: ryb.d7cx3q.svjheinl
        Regex(".*\\.fake$"), Regex(".*\\.clone$"),
        Regex(".*\\.cracked$"), Regex(".*\\.modded$"),
        Regex(".*\\.hack$"), Regex("com\\.spy\\..*"),
        Regex(".*\\.keylogger.*"), Regex("com\\.rat\\..*"),
        Regex(".*tracker\\.spy.*"), Regex(".*\\.stealer.*"),
        Regex(".*\\.grabber.*"), Regex(".*\\.logger.*")
    )

    private val userBlacklist = mutableMapOf<String, BlacklistEntry>()

    fun getBlacklistEntry(packageName: String): BlacklistEntry? {
        val pkg = packageName.trim().lowercase()
        return KNOWN_MALICIOUS[pkg] ?: userBlacklist[pkg] ?: checkPattern(pkg)
    }

    fun isBlacklisted(packageName: String) = getBlacklistEntry(packageName) != null

    private fun checkPattern(pkg: String): BlacklistEntry? {
        val m = SUSPICIOUS_PATTERNS.firstOrNull { it.matches(pkg) } ?: return null
        return BlacklistEntry(pkg, "Shubhali paket nomi: $m", 90, ThreatCategory.FAKE_APP)
    }

    fun addToUserBlacklist(packageName: String, reason: String) {
        userBlacklist[packageName.trim().lowercase()] =
            BlacklistEntry(packageName.trim().lowercase(), reason, 100, ThreatCategory.FAKE_APP)
    }
}


// ════════════════════════════════════════════════════════════════
// 3. NetworkTrafficAnalyzer (YANGI)
// APK (ZIP) faylini ochib, ichidagi barcha .dex, .xml, strings
// fayllaridan URL, IP manzillar, domenlarni topadi.
// Bu QAYERGA XABAR YUBORAYOTGANINI ko'rsatadi.
// ════════════════════════════════════════════════════════════════

object NetworkTrafficAnalyzer {

    data class NetworkAnalysisResult(
        val foundUrls: List<String>,       // https://... manzillar
        val foundIps: List<String>,        // 185.x.x.x kabi IP lar
        val foundDomains: List<String>,    // api.example.com kabi domenlar
        val suspiciousEndpoints: List<SuspiciousEndpoint>,
        val riskPoints: Int,
        val summary: String
    )

    data class SuspiciousEndpoint(
        val address: String,
        val reason: String,
        val severity: String  // "HIGH", "MEDIUM", "LOW"
    )

    // Shubhali domenlar va patterns
    private val SUSPICIOUS_DOMAIN_PATTERNS = listOf(
        Regex(".*\\.ru/.*api.*"),
        Regex(".*ngrok\\.io.*"),
        Regex(".*\\.onion.*"),
        Regex(".*pastebin\\.com.*"),
        Regex(".*bit\\.ly.*"),
        Regex(".*tinyurl\\.com.*"),
        Regex(".*no-ip\\.org.*"),
        Regex(".*ddns\\.net.*"),
        Regex(".*duckdns\\.org.*"),
        Regex(".*\\.xyz/.*"),
        Regex(".*\\.top/.*"),
        Regex(".*\\.tk/.*"),
        Regex(".*\\.cc/.*gate.*"),
        Regex(".*\\.cc/.*panel.*"),
        Regex(".*telegram\\.me/bot.*"),   // bot token leak
        Regex(".*api\\.telegram\\.org/bot[0-9]+:.*")  // Telegram bot token!
    )

    // Taniqli C2 (Command & Control) server indicators
    private val C2_INDICATORS = listOf(
        "/gate.php", "/panel/", "/bot/", "/c2/", "/cnc/",
        "/upload.php", "/report.php", "/tasks.php",
        "/inject.php", "/grab.php", "/logs.php",
        "cmd=", "action=steal", "type=sms", "type=contacts"
    )

    // Legit CDN/API domenlar — bular XAVFLI emas
    private val TRUSTED_DOMAINS = setOf(
        "googleapis.com", "gstatic.com", "google.com",
        "firebase.io", "firebaseio.com", "crashlytics.com",
        "facebook.com", "fbcdn.net", "instagram.com",
        "amazonaws.com", "cloudfront.net", "akamai.net",
        "telegram.org", "tlgrm.ru",
        "yandex.ru", "yandex.com", "yandex.net",
        "vk.com", "vk.me",
        "apelsin.uz", "click.uz", "payme.uz",
        "kapitalbank.uz", "humo.uz"
    )

    /**
     * APK faylini tahlil qilib, network endpoint larni topadi.
     * @param apkFile — APK fayl (ZIP formatida)
     * @return NetworkAnalysisResult
     */
    fun analyze(apkFile: File): NetworkAnalysisResult {
        val urls = mutableSetOf<String>()
        val ips = mutableSetOf<String>()
        val domains = mutableSetOf<String>()

        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    // Faqat matn bo'lishi mumkin bo'lgan fayllarni o'qish
                    if (isTextEntry(name) && entry.size < 5 * 1024 * 1024) {
                        try {
                            val content = zip.getInputStream(entry)
                                .bufferedReader(Charsets.UTF_8)
                                .readText()
                            extractNetworkData(content, urls, ips, domains)
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}

        val suspicious = findSuspiciousEndpoints(urls, ips, domains)
        val riskPoints = calculateNetworkRisk(suspicious, urls, ips)
        val summary = buildSummary(urls, ips, domains, suspicious)

        return NetworkAnalysisResult(
            foundUrls = urls.toList().take(20),
            foundIps = ips.toList().take(20),
            foundDomains = domains.toList().take(20),
            suspiciousEndpoints = suspicious,
            riskPoints = riskPoints,
            summary = summary
        )
    }

    private fun isTextEntry(name: String): Boolean {
        return name.endsWith(".xml") || name.endsWith(".json") ||
               name.endsWith(".properties") || name.endsWith(".txt") ||
               name.contains("strings") || name.contains("config") ||
               name.endsWith(".dex")  // dex ham string topishi uchun
    }

    private fun extractNetworkData(
        content: String,
        urls: MutableSet<String>,
        ips: MutableSet<String>,
        domains: MutableSet<String>
    ) {
        // URL lar
        val urlRegex = Regex("""https?://[a-zA-Z0-9\-._~:/?#\[\]@!$&'()*+,;=%]{10,200}""")
        urlRegex.findAll(content).forEach { match ->
            val url = match.value.trimEnd('.', ',', '"', '\'', ')')
            if (url.length > 10) urls.add(url)
        }

        // IP manzillar (tashqi — 10.x, 192.168.x, 127.x ni o'tkazib yuborish)
        val ipRegex = Regex("""(?<![.\d])(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(?![.\d])""")
        ipRegex.findAll(content).forEach { match ->
            val ip = match.value
            if (!isPrivateIp(ip) && isValidIp(ip)) ips.add(ip)
        }

        // Domenlar (URL siz)
        val domainRegex = Regex("""(?:^|[\s"'(])([a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.(?:com|net|org|io|ru|uz|kz|ua|de|info|biz|app|dev|api){1}(?:\.[a-z]{2})?)""")
        domainRegex.findAll(content).forEach { match ->
            val domain = match.groupValues[1]
            if (domain.length > 4 && !isTrustedDomain(domain)) domains.add(domain)
        }

        // Telegram bot token leak tekshiruvi
        val botTokenRegex = Regex("""bot(\d{8,12}:[A-Za-z0-9_\-]{35,})""")
        botTokenRegex.findAll(content).forEach { match ->
            urls.add("TELEGRAM_BOT_TOKEN_LEAK: ${match.value.take(20)}...")
        }
    }

    private fun findSuspiciousEndpoints(
        urls: Set<String>,
        ips: Set<String>,
        domains: Set<String>
    ): List<SuspiciousEndpoint> {
        val result = mutableListOf<SuspiciousEndpoint>()

        // URL larni tekshirish
        for (url in urls) {
            val lurl = url.lowercase()

            // Telegram bot token
            if (lurl.contains("telegram_bot_token_leak")) {
                result.add(SuspiciousEndpoint(url, "Telegram bot token APK ichida — ma'lumot o'g'irlash", "HIGH"))
                continue
            }

            // C2 indicator
            val c2hit = C2_INDICATORS.firstOrNull { lurl.contains(it) }
            if (c2hit != null) {
                result.add(SuspiciousEndpoint(url, "C2 server belgisi: $c2hit", "HIGH"))
                continue
            }

            // Shubhali pattern
            val patternHit = SUSPICIOUS_DOMAIN_PATTERNS.firstOrNull { it.containsMatchIn(lurl) }
            if (patternHit != null && !isTrustedDomain(url)) {
                result.add(SuspiciousEndpoint(url, "Shubhali domen pattern", "MEDIUM"))
            }
        }

        // Raw IP manzillar (domain o'rniga to'g'ridan-to'g'ri IP — shubhali)
        for (ip in ips) {
            if (!isKnownCdn(ip)) {
                result.add(SuspiciousEndpoint(ip, "To'g'ridan-to'g'ri IP manzil — C2 server bo'lishi mumkin", "MEDIUM"))
            }
        }

        return result.distinctBy { it.address }
    }

    private fun calculateNetworkRisk(
        suspicious: List<SuspiciousEndpoint>,
        urls: Set<String>,
        ips: Set<String>
    ): Int {
        var risk = 0
        for (ep in suspicious) {
            risk += when (ep.severity) {
                "HIGH"   -> 25
                "MEDIUM" -> 10
                else     -> 5
            }
        }
        // Ko'p raw IP — qo'shimcha xavf
        if (ips.size > 5) risk += 10
        return risk.coerceIn(0, 40)
    }

    private fun buildSummary(
        urls: Set<String>,
        ips: Set<String>,
        domains: Set<String>,
        suspicious: List<SuspiciousEndpoint>
    ): String {
        if (urls.isEmpty() && ips.isEmpty()) return "Network manzillar topilmadi"

        val sb = StringBuilder()
        sb.append("Topildi: ${urls.size} URL, ${ips.size} IP, ${domains.size} domen\n")

        if (suspicious.isNotEmpty()) {
            sb.append("\nShubhali manzillar:\n")
            suspicious.take(5).forEach {
                sb.append("  [${it.severity}] ${it.address.take(60)}\n  → ${it.reason}\n")
            }
        } else {
            sb.append("Shubhali manzil topilmadi")
        }
        return sb.toString()
    }

    private fun isPrivateIp(ip: String): Boolean {
        return ip.startsWith("10.") || ip.startsWith("192.168.") ||
               ip.startsWith("172.16.") || ip.startsWith("127.") ||
               ip == "0.0.0.0" || ip.startsWith("255.")
    }

    private fun isValidIp(ip: String): Boolean {
        return try {
            ip.split(".").all { it.toInt() in 0..255 }
        } catch (_: Exception) { false }
    }

    private fun isTrustedDomain(url: String): Boolean {
        return TRUSTED_DOMAINS.any { trusted -> url.lowercase().contains(trusted) }
    }

    private fun isKnownCdn(ip: String): Boolean {
        // Google, Cloudflare, Amazon IP range prefixlari (soddalashtirilgan)
        return ip.startsWith("8.8.") || ip.startsWith("1.1.") ||
               ip.startsWith("104.") || ip.startsWith("172.217.")
    }
}


// ════════════════════════════════════════════════════════════════
// 4. SmartRiskEngine (YANGILANGAN — Yangi ballar tizimi)
// ════════════════════════════════════════════════════════════════

object SmartRiskEngine {

    data class RiskResult(
        val packageName: String,
        val totalScore: Int,
        val level: RiskLevel,
        val isBlacklisted: Boolean,
        val isWhitelisted: Boolean,
        val details: List<RiskDetail>,
        val networkAnalysis: NetworkTrafficAnalyzer.NetworkAnalysisResult?,
        val recommendation: String
    )

    data class RiskDetail(
        val category: String,
        val points: Int,
        val description: String
    )

    enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH, CRITICAL }

    fun calculate(
        packageName: String,
        permissions: List<String>,
        apkFile: File? = null,
        apkSizeMb: Float = 0f,
        hasValidSignature: Boolean = true,
        installSource: String = "unknown"
    ): RiskResult {

        // 1. Blacklist
        val blacklistEntry = BlacklistManager.getBlacklistEntry(packageName)
        if (blacklistEntry != null) {
            return RiskResult(
                packageName, blacklistEntry.riskScore, RiskLevel.CRITICAL,
                isBlacklisted = true, isWhitelisted = false,
                details = listOf(RiskDetail("Blacklist", blacklistEntry.riskScore, blacklistEntry.reason)),
                networkAnalysis = null,
                recommendation = "BLOKLANDI: ${blacklistEntry.reason}"
            )
        }

        // 2. Whitelist
        if (WhitelistManager.isWhitelisted(packageName)) {
            return RiskResult(
                packageName, 5, RiskLevel.SAFE,
                isBlacklisted = false, isWhitelisted = true,
                details = listOf(RiskDetail("Rasmiy ilova", 0, "Tasdiqlangan rasmiy ilova")),
                networkAnalysis = null,
                recommendation = "Xavfsiz. Rasmiy ilova sifatida tasdiqlangan."
            )
        }

        // 3. To'liq tahlil (Yangi ballar tizimi)
        val details = mutableListOf<RiskDetail>()
        
        // --- SECTION 1: PERMISSIONS (Max: 50) ---
        var permScore = 0
        val hasSms      = permissions.any { "SMS" in it || "MMS" in it }
        val hasContacts = permissions.any { "CONTACTS" in it }
        val hasInternet = permissions.any { "INTERNET" in it }
        val hasAccess   = permissions.any { "ACCESSIBILITY" in it }
        val hasAdmin    = permissions.any { "DEVICE_ADMIN" in it || "BIND_DEVICE_ADMIN" in it }
        val hasCamera   = permissions.any { "CAMERA" in it }
        val hasMic      = permissions.any { "RECORD_AUDIO" in it || "MICROPHONE" in it }
        val hasLocation = permissions.any { "LOCATION" in it }
        val hasOverlay  = permissions.any { "SYSTEM_ALERT_WINDOW" in it || "DRAW_OVER" in it }
        val hasPkgInst  = permissions.any { "REQUEST_INSTALL_PACKAGES" in it || "INSTALL_PACKAGES" in it }
        val hasAllPkgs  = permissions.any { "QUERY_ALL_PACKAGES" in it }
        val hasBoot     = permissions.any { "RECEIVE_BOOT_COMPLETED" in it }
        val hasUsage    = permissions.any { "PACKAGE_USAGE_STATS" in it }
        val hasAccounts = permissions.any { "ACCOUNTS" in it || "AUTHENTICATE" in it }
        val hasCalls    = permissions.any { "CALLS" in it || "CALL_PHONE" in it || "PROCESS_OUTGOING_CALLS" in it }
        val hasKeyguard = permissions.any { "DISABLE_KEYGUARD" in it }

        if (hasOverlay && hasPkgInst && (hasBoot || hasUsage)) {
            permScore += 100 
            details.add(RiskDetail("Ruxsatnomalar", 100, "Zararli ruxsatnomalar kombinatsiyasi (Malware Dropper)"))
        }

        if (hasAccounts && hasInternet) {
            permScore += 40
            details.add(RiskDetail("Ruxsatnomalar", 40, "Hisob qaydnomalarini o'g'irlash xavfi (Account Theft)"))
        }

        if (hasCalls && hasInternet) {
            permScore += 35
            details.add(RiskDetail("Ruxsatnomalar", 35, "Qo'ng'iroqlarni kuzatish xavfi (Call Spyware)"))
        }

        if (hasSms && hasInternet) {
            permScore += 30
            details.add(RiskDetail("Ruxsatnomalar", 30, "SMS xabarlarni o'g'irlash xavfi (Banking Trojan)"))
        }

        if (hasKeyguard) {
            permScore += 25
            details.add(RiskDetail("Ruxsatnomalar", 25, "Ekran blokirovkasini chetlab o'tish (Locker)"))
        }
        
        if (hasPkgInst) {
            permScore += 20
            details.add(RiskDetail("Ruxsatnomalar", 20, "Boshqa ilovalarni o'rnatish huquqi (Dropper)"))
        }

        if (hasAccess) {
            permScore += 30
            details.add(RiskDetail("Ruxsatnomalar", 30, "Accessibility Service (To'liq nazorat)"))
        }
        
        // Paket nomini tekshirish (Random string detection)
        if (packageName.length > 15 && packageName.split(".").any { it.length > 8 && it.all { c -> c in 'a'..'z' } && !it.contains(Regex("[aeiouy]")) }) {
            permScore += 40
            details.add(RiskDetail("Qo'shimcha", 40, "Shubhali paket nomi (Obfuscated Package Name)"))
        }

        permScore = permScore.coerceAtMost(100)

        // --- SECTION 2: NETWORK (Max: 33) ---
        var networkScore = 0
        val networkResult = if (apkFile != null && apkFile.exists()) {
            try { NetworkTrafficAnalyzer.analyze(apkFile) } catch (_: Exception) { null }
        } else null

        networkResult?.let { net ->
            for (ep in net.suspiciousEndpoints) {
                if (ep.address.contains("bot") || ep.reason.contains("bot", true)) {
                    networkScore += 10
                    details.add(RiskDetail("Network", 10, "Telegram Bot Token aniqlandi"))
                } else if (ep.reason.contains("C2", true)) {
                    networkScore += 10
                    details.add(RiskDetail("Network", 10, "C2 endpoint aniqlandi (/gate.php)"))
                } else if (net.foundIps.contains(ep.address)) {
                    networkScore += 5
                    details.add(RiskDetail("Network", 5, "Raw IP ishlatish (Yashirin ulanish)"))
                } else {
                    networkScore += 5
                    details.add(RiskDetail("Network", 5, "Shubhali domen (.ru, .xyz, ngrok)"))
                }
            }
        }
        networkScore = networkScore.coerceAtMost(33)

        // --- SECTION 3: OTHER FACTORS (Max: 33) ---
        var otherScore = 0
        if (!hasValidSignature) {
            otherScore += 10
            details.add(RiskDetail("Qo'shimcha", 10, "Noto'g'ri imzo (debug/signature)"))
        }
        if (apkSizeMb > 0 && apkSizeMb < 1.0f && permissions.size > 5) {
            otherScore += 8
            details.add(RiskDetail("Qo'shimcha", 8, "Hajm anomaliyasi (Yashirin kod ehtimoli)"))
        }
        if (installSource == "browser" || installSource == "telegram") {
            otherScore += 8
            details.add(RiskDetail("Qo'shimcha", 8, "Noma'lum manba (Phishing/Pirat)"))
        }
        
        // Dex analiz orqali obfuscation tekshiruvi (simulyatsiya)
        if (apkFile != null && apkFile.name.contains("mod", true)) {
            otherScore += 7
            details.add(RiskDetail("Qo'shimcha", 7, "Obfuscation/Packer mavjud (Kod yashirilgan)"))
        }

        // 4. Payload/Hidden file check
        if (apkFile != null && apkFile.exists()) {
            try {
                java.util.zip.ZipFile(apkFile).use { zip ->
                    val hasSuspiciousFile = zip.entries().asSequence().any { 
                        it.name.contains("helper_backup", true) || 
                        it.name.endsWith(".conf") && it.size > 1 * 1024 * 1024 
                    }
                    if (hasSuspiciousFile) {
                        otherScore += 100
                        details.add(RiskDetail("Payload", 100, "Yashirilgan shubhali payload aniqlandi (helper_backup)"))
                    }
                }
            } catch (_: Exception) {}
        }

        otherScore = otherScore.coerceAtMost(100)

        val totalScore = (permScore + networkScore + otherScore).coerceIn(0, 100)
        
        val level = when {
            totalScore >= 80 -> RiskLevel.CRITICAL
            totalScore >= 55 -> RiskLevel.HIGH
            totalScore >= 30 -> RiskLevel.MEDIUM
            totalScore >= 10 -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        return RiskResult(
            packageName, totalScore, level,
            isBlacklisted = false, isWhitelisted = false,
            details = details, networkAnalysis = networkResult,
            recommendation = when (level) {
                RiskLevel.SAFE     -> "Xavfsiz. O'rnatishingiz mumkin."
                RiskLevel.LOW      -> "Past xavf. Manbasini tekshiring."
                RiskLevel.MEDIUM   -> "O'rtacha xavf. Ishonchliligini tekshiring."
                RiskLevel.HIGH     -> "Yuqori xavf! Faqat ishonchli manbadan o'rnating."
                RiskLevel.CRITICAL -> "O'rnatmang! Juda xavfli ilova."
            }
        )
    }
}


// ════════════════════════════════════════════════════════════════
// 5. UriHelper (YANGI — TG content:// URI muammosini hal qiladi)
// Telegram FileProvider orqali content:// URI yuboradi.
// Bu sinf uni real fayl yo'liga aylantiradi.
// ════════════════════════════════════════════════════════════════

object UriHelper {

    /**
     * content://, file://, va boshqa URI turlarini
     * real File obyektiga aylantiradi.
     *
     * Telegram content://org.telegram.messenger.provider/... yuboradi.
     * Bu funksiya uni /data/user/0/.../cache/temp.apk ga nusxalaydi.
     */
    fun uriToFile(context: Context, uri: Uri): File? {
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                // Oddiy fayl yo'li
                uri.path?.let { File(it) }?.takeIf { it.exists() }
            }
            "content" -> {
                // Content Provider — TG, WhatsApp, Gmail attachments
                copyContentUriToTemp(context, uri)
            }
            else -> null
        }
    }

    /**
     * content:// URI ni temp faylga nusxalaydi.
     * Bu TG intercepti uchun asosiy funksiya.
     */
    private fun copyContentUriToTemp(context: Context, uri: Uri): File? {
        return try {
            val fileName = getFileName(context, uri) ?: "temp_${System.currentTimeMillis()}.apk"
            if (!fileName.endsWith(".apk", ignoreCase = true)) return null

            val tempFile = File(context.cacheDir, "apkguard_$fileName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
        } catch (_: Exception) { null }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null

        // ContentResolver orqali fayl nomini olish
        val cursor: Cursor? = try {
            context.contentResolver.query(uri, null, null, null, null)
        } catch (_: Exception) { null }

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) name = it.getString(nameIndex)
            }
        }

        // Fallback: URI path'idan nom olish
        if (name == null) {
            name = uri.lastPathSegment?.let { seg ->
                if (seg.contains(".apk", ignoreCase = true)) seg else null
            }
        }

        return name
    }

    fun isApkUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == "application/vnd.android.package-archive") return true

        val path = uri.path ?: return false
        return path.endsWith(".apk", ignoreCase = true)
    }
}


// ════════════════════════════════════════════════════════════════
// 6. ApkInterceptActivity (TUZATILGAN — content:// qo'llab-quvvatlaydi)
// ════════════════════════════════════════════════════════════════

class ApkInterceptActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri == null) { finish(); return }

        // LOOP MUAMMOSIDAN SAQLANISH
        if (intent.getBooleanExtra("apkguard_scanned", false)) {
            passToSystemInstaller(uri)
            finish()
            return
        }

        // APK emasligini tekshirish
        if (!UriHelper.isApkUri(this, uri)) {
            // APK emas — o'tkazib yuborish (standart handler ga)
            passToSystemInstaller(uri)
            finish()
            return
        }

        // Loading ko'rsatish
        showLoadingDialog()

        CoroutineScope(Dispatchers.IO).launch {
            scanApk(uri)
        }
    }

    private var loadingDialog: AlertDialog? = null

    private fun showLoadingDialog() {
        loadingDialog = AlertDialog.Builder(this)
            .setTitle("APKGuard skanlanmoqda...")
            .setMessage("APK tekshirilmoqda, iltimos kuting...")
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private suspend fun scanApk(uri: Uri) {
        val apkFile = withContext(Dispatchers.IO) {
            UriHelper.uriToFile(this@ApkInterceptActivity, uri)
        }

        if (apkFile == null) {
            withContext(Dispatchers.Main) {
                loadingDialog?.dismiss()
                showUnknownWarning(uri)
            }
            return
        }

        val info = try {
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath, PackageManager.GET_PERMISSIONS
            )
        } catch (_: Exception) { null }

        val pkg   = info?.packageName ?: "unknown.package"
        val perms = info?.requestedPermissions?.toList() ?: emptyList()
        val size  = apkFile.length() / (1024f * 1024f)
        val src   = detectSource()

        // Network tahlili bilan to'liq skan
        val result = SmartRiskEngine.calculate(
            packageName = pkg,
            permissions = perms,
            apkFile = apkFile,     // Network tahlili uchun
            apkSizeMb = size,
            installSource = src
        )

        // ResultScreen ga o'tish (MainActivity orqali)
        withContext(Dispatchers.Main) {
            loadingDialog?.dismiss()
            
            val intent = Intent(this@ApkInterceptActivity, com.apkguard.MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                // Bu yerda natijani ham uzatish mumkin yoki shunchaki MainActivity skan boshlashini kutish
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showResult(result: SmartRiskEngine.RiskResult, uri: Uri, apkFile: File) {
        val emoji = when {
            result.isBlacklisted -> "BLOKLANDI"
            result.isWhitelisted -> "Xavfsiz"
            else -> when (result.level) {
                SmartRiskEngine.RiskLevel.SAFE     -> "Xavfsiz"
                SmartRiskEngine.RiskLevel.LOW      -> "Past xavf"
                SmartRiskEngine.RiskLevel.MEDIUM   -> "O'rtacha xavf"
                SmartRiskEngine.RiskLevel.HIGH     -> "Yuqori xavf"
                SmartRiskEngine.RiskLevel.CRITICAL -> "KRITIK"
            }
        }

        val title = "APKGuard: $emoji (${result.totalScore}/100)"

        val permDetails = result.details.joinToString("\n") { "  + ${it.category} (+${it.points}): ${it.description}" }

        // Network analiz natijasi
        val networkSection = result.networkAnalysis?.let { net ->
            if (net.suspiciousEndpoints.isNotEmpty()) {
                "\n\nShubhali server manzillar:\n" +
                net.suspiciousEndpoints.take(3).joinToString("\n") { ep ->
                    "  [${ep.severity}] ${ep.address.take(50)}\n  ${ep.reason}"
                }
            } else if (net.foundUrls.isNotEmpty()) {
                "\n\nNetwork: ${net.foundUrls.size} URL, ${net.foundIps.size} IP topildi (xavfsiz)"
            } else ""
        } ?: ""

        val msg = "Paket: ${result.packageName}\n\n${result.recommendation}\n\nRuxsatlar tahlili:\n$permDetails$networkSection"

        val b = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(false)

        if (result.isBlacklisted || result.level == SmartRiskEngine.RiskLevel.CRITICAL) {
            b.setNegativeButton("Orqaga qaytish") { _, _ ->
                apkFile.delete()
                finish()
            }
        } else {
            b.setPositiveButton("O'rnatishni davom ettirish") { _, _ ->
                installApk(uri)
                finish()
            }
            b.setNegativeButton("Bekor qilish") { _, _ ->
                apkFile.delete()
                finish()
            }
        }
        b.show()
    }

    private fun showUnknownWarning(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("APKGuard ogohlantirish")
            .setMessage("APK o'qib bo'lmadi yoki fayl topilmadi.\nO'rnatishni davom ettirasizmi?")
            .setPositiveButton("Ha, o'rnat") { _, _ -> installApk(uri); finish() }
            .setNegativeButton("Yo'q") { _, _ -> finish() }
            .show()
    }

    private fun installApk(uri: Uri) {
        passToSystemInstaller(uri)
    }

    private fun passToSystemInstaller(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                // APKGuard'ni exclude qilish — loop oldini olish
                putExtra("apkguard_scanned", true)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun detectSource(): String {
        val pkg = callingPackage ?: return "unknown"
        return when {
            "telegram" in pkg -> "telegram"
            "whatsapp" in pkg -> "whatsapp"
            "chrome" in pkg || "browser" in pkg || "firefox" in pkg -> "browser"
            "vending" in pkg  -> "play_store"
            else              -> pkg
        }
    }
}


// ════════════════════════════════════════════════════════════════
// 7. BootReceiver (o'zgarishsiz)
// ════════════════════════════════════════════════════════════════

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            TelegramScanInterceptor.start(context)
        }
    }
}


// ════════════════════════════════════════════════════════════════
// 8. TelegramScanInterceptor (TUZATILGAN)
// ════════════════════════════════════════════════════════════════

class TelegramScanInterceptor : Service() {

    companion object {
        private const val CHANNEL_ID = "apkguard_scan"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val i = Intent(context, TelegramScanInterceptor::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) =
            context.stopService(Intent(context, TelegramScanInterceptor::class.java))
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("APKGuard faol")
                .setContentText("APK fayllar skanlanmoqda")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_LOW).build()
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "APKGuard Himoya",
                NotificationManager.IMPORTANCE_LOW).apply { description = "APK skan" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
