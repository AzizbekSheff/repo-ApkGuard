package com.apkguard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkguard.R
import com.apkguard.model.*
import com.apkguard.ui.components.*
import com.apkguard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: ScanResult,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onBack: () -> Unit,
    readOnly: Boolean = false
) {
    val scrollState = rememberScrollState()
    var showFullReport by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_report), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!readOnly) {
                DecisionBar(onApprove = onApprove, onReject = onReject, riskLevel = result.riskLevel)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeaderCard(result)
            RiskScoreCard(result)

            if (!showFullReport) {
                Button(
                    onClick = { showFullReport = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("HISOBOT", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (showFullReport) {
                AppInfoCard(result)

                if (result.riskReasons.isNotEmpty()) {
                    RiskReasonsCard(result.riskReasons)
                }

                result.signatureInfo?.let { SignatureCard(it) }
                PermissionsCard(result.permissions)

                if (result.malwareHits.isNotEmpty()) {
                    MalwareCard(result.malwareHits)
                }

                if (result.codePatterns.isNotEmpty()) {
                    CodePatternsCard(result.codePatterns)
                }

                if (result.foundUrls.isNotEmpty()) {
                    UrlsCard(result.foundUrls)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DeviceInfoCard(device: DeviceInfo) {
    SectionCard(
        title = "Qurilma Ma'lumotlari",
        icon = Icons.Default.Devices
    ) {
        val items = listOf(
            "IP Manzil" to (device.ipAddress ?: "Aniqlanmadi"),
            "Joylashuv" to (device.location ?: "Aniqlanmadi"),
            "Koordinata" to if (device.latitude != null) "${device.latitude}, ${device.longitude}" else "N/A"
        )
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HeaderCard(result: ScanResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Default.Android, contentDescription = null,
                modifier = Modifier.size(40.dp), tint = Color(0xFF3DDC84))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.apkName, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(result.packageName, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                RiskBadge(result.riskLevel)
            }
        }
    }
}

@Composable
private fun RiskScoreCard(result: ScanResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.risk_score), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            RiskScoreRing(result.riskScore, result.riskLevel)
            Spacer(Modifier.height(8.dp))
            Text(
                when (result.riskLevel) {
                    RiskLevel.LOW -> stringResource(R.string.risk_low_msg)
                    RiskLevel.MEDIUM -> stringResource(R.string.risk_med_msg)
                    RiskLevel.HIGH -> stringResource(R.string.risk_high_msg)
                    RiskLevel.CRITICAL -> stringResource(R.string.risk_crit_msg)
                },
                fontSize = 13.sp,
                color = riskColor(result.riskLevel)
            )
        }
    }
}

@Composable
private fun AppInfoCard(result: ScanResult) {
    SectionCard(
        title = stringResource(R.string.app_info),
        icon = Icons.Default.Info
    ) {
        val df = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val items = listOf(
            "Package" to result.packageName,
            "Version" to "${result.versionName} (${result.versionCode})",
            "Min SDK" to result.minSdkVersion,
            "Target SDK" to result.targetSdkVersion,
            "APK Size" to formatBytes(result.apkSizeBytes),
            "Scanned" to df.format(Date(result.scannedAt))
        )
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun RiskReasonsCard(reasons: List<String>) {
    SectionCard(
        title = stringResource(R.string.risk_factors),
        icon = Icons.Default.ReportProblem,
        badge = "${reasons.size}",
        badgeColor = RiskHigh
    ) {
        reasons.forEachIndexed { i, reason ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${i + 1}.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(20.dp))
                Text(reason, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SignatureCard(sig: SignatureInfo) {
    SectionCard(
        title = stringResource(R.string.signature),
        icon = Icons.Default.VerifiedUser
    ) {
        val items = listOf(
            "Debug Signed" to if (sig.isDebugSigned) "YES" else "No",
            "Self Signed" to if (sig.isSelfSigned) "YES" else "No",
            "Expired" to if (sig.isExpired) "YES" else "No"
        )
        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun PermissionsCard(permissions: List<PermissionInfo>) {
    var showAll by remember { mutableStateOf(false) }
    SectionCard(
        title = stringResource(R.string.feat_perms),
        icon = Icons.Default.Lock,
        badge = "${permissions.size}"
    ) {
        val shown = if (showAll) permissions else permissions.take(5)
        shown.forEach { PermissionRow(it) }
        if (permissions.size > 5) {
            TextButton(onClick = { showAll = !showAll }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showAll) "Hide" else "Show more...")
            }
        }
    }
}

@Composable
private fun MalwareCard(hits: List<MalwareHit>) {
    SectionCard(
        title = stringResource(R.string.malware_indicators),
        icon = Icons.Default.Dangerous,
        badge = "${hits.size}",
        badgeColor = RiskCritical
    ) {
        hits.forEach { hit ->
            Text("${hit.category}: ${hit.keyword}", fontSize = 12.sp)
        }
    }
}

@Composable
private fun CodePatternsCard(patterns: List<CodePattern>) {
    SectionCard(
        title = stringResource(R.string.code_patterns),
        icon = Icons.Default.Code,
        badge = "${patterns.size}"
    ) {
        patterns.forEach { PatternRow(it) }
    }
}

@Composable
private fun UrlsCard(urls: List<FoundUrl>) {
    SectionCard(
        title = stringResource(R.string.embedded_urls),
        icon = Icons.Default.Link,
        badge = "${urls.size}"
    ) {
        urls.take(10).forEach { UrlRow(it) }
    }
}

@Composable
private fun DecisionBar(
    onApprove: () -> Unit,
    onReject: () -> Unit,
    riskLevel: RiskLevel
) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCritical)
            ) {
                Text(stringResource(R.string.reject), fontWeight = FontWeight.SemiBold)
            }
            if (riskLevel != RiskLevel.CRITICAL) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (riskLevel == RiskLevel.LOW || riskLevel == RiskLevel.MEDIUM)
                            RiskLow else RiskHigh
                    )
                ) {
                    Text(stringResource(R.string.approve), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
