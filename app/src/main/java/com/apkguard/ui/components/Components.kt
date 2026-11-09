package com.apkguard.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkguard.model.*
import com.apkguard.ui.theme.*

// ─── Risk Badge ───────────────────────────────────────────────────────────────

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val (color, icon) = when (level) {
        RiskLevel.LOW      -> RiskLow      to Icons.Default.CheckCircle
        RiskLevel.MEDIUM   -> RiskMedium   to Icons.Default.Warning
        RiskLevel.HIGH     -> RiskHigh     to Icons.Default.Error
        RiskLevel.CRITICAL -> RiskCritical to Icons.Default.Dangerous
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(level.label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Risk Score Ring ──────────────────────────────────────────────────────────

@Composable
fun RiskScoreRing(score: Int, level: RiskLevel, size: Dp = 140.dp) {
    val color = riskColor(level)
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "score"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            val sweep = (animatedScore / 100f) * 270f
            // Background arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = 135f, sweepAngle = 270f,
                useCenter = false, style = stroke
            )
            // Score arc
            drawArc(
                color = color,
                startAngle = 135f, sweepAngle = sweep,
                useCenter = false, style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${animatedScore.toInt()}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text("/ 100", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Section Card ─────────────────────────────────────────────────────────────

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (badge != null) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(badge, color = badgeColor, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ─── Permission Row ───────────────────────────────────────────────────────────

@Composable
fun PermissionRow(permission: PermissionInfo) {
    val (color, label) = when (permission.danger) {
        PermissionDanger.DANGEROUS  -> RiskHigh     to "DANGEROUS"
        PermissionDanger.SIGNATURE  -> RiskCritical to "SYSTEM"
        PermissionDanger.NORMAL     -> MaterialTheme.colorScheme.onSurfaceVariant to "NORMAL"
        PermissionDanger.UNKNOWN    -> RiskMedium   to "UNKNOWN"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(permission.simpleName, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(permission.description, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// ─── URL Row ─────────────────────────────────────────────────────────────────

@Composable
fun UrlRow(foundUrl: FoundUrl) {
    val color = when (foundUrl.risk) {
        UrlRisk.SAFE      -> RiskLow
        UrlRisk.SUSPICIOUS -> RiskMedium
        UrlRisk.MALICIOUS  -> RiskCritical
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            when (foundUrl.risk) {
                UrlRisk.SAFE -> Icons.Default.Link
                UrlRisk.SUSPICIOUS -> Icons.Default.Warning
                UrlRisk.MALICIOUS -> Icons.Default.Dangerous
            },
            contentDescription = null, tint = color, modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(foundUrl.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (foundUrl.reason.isNotEmpty()) {
                Text(foundUrl.reason, fontSize = 10.sp, color = color)
            }
            if (foundUrl.ipAddress != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${foundUrl.location ?: "Unknown Location"} (${foundUrl.ipAddress})", 
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                // IP Manzil uchun mini karta (User experience)
                Card(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Server manzili", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Xavf: ${foundUrl.risk}", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                        }
                        Text(foundUrl.location ?: "Noma'lum hudud", fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("IP: ${foundUrl.ipAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// ─── Code Pattern Row ────────────────────────────────────────────────────────

@Composable
fun PatternRow(pattern: com.apkguard.model.CodePattern) {
    val color = when (pattern.severity) {
        PatternSeverity.INFO    -> MaterialTheme.colorScheme.primary
        PatternSeverity.WARNING -> RiskMedium
        PatternSeverity.DANGER  -> RiskCritical
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            when (pattern.severity) {
                PatternSeverity.INFO -> Icons.Default.Info
                PatternSeverity.WARNING -> Icons.Default.Warning
                PatternSeverity.DANGER -> Icons.Default.BugReport
            },
            contentDescription = null, tint = color, modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(pattern.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(pattern.description, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        if (pattern.occurrences > 1) {
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text("×${pattern.occurrences}", color = color, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// ─── Scanning Animation ───────────────────────────────────────────────────────

@Composable
fun ScanningIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation"
    )
    Icon(
        Icons.Default.Security,
        contentDescription = "Scanning",
        modifier = Modifier.size(64.dp).rotate(rotation),
        tint = MaterialTheme.colorScheme.primary
    )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.LOW      -> RiskLow
    RiskLevel.MEDIUM   -> RiskMedium
    RiskLevel.HIGH     -> RiskHigh
    RiskLevel.CRITICAL -> RiskCritical
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000     -> "%.1f KB".format(bytes / 1_000.0)
    else               -> "$bytes B"
}
