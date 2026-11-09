package com.apkguard.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apkguard.R
import com.apkguard.util.LocaleHelper
import com.apkguard.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AuthViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    var isOverlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    val isProtectionOn = isOverlayEnabled && isAccessibilityEnabled

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayEnabled = Settings.canDrawOverlays(context)
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield, 
                            null, 
                            tint = Color(0xFF4A9EF5), 
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "APK Guard", 
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLanguageDialog = true }) { 
                        Icon(Icons.Default.Translate, null, tint = Color.White) 
                    }
                    IconButton(onClick = onNavigateToHistory) { 
                        Icon(Icons.Default.History, null, tint = Color.White) 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D1A)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 2. PROTECTION STATUS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .then(if (isProtectionOn) Modifier.scale(pulseScale) else Modifier),
                            tint = if (isProtectionOn) Color(0xFF00C853) else Color.Gray
                        )
                        if (isProtectionOn) {
                            // Glow effect can be simulated with a larger, faint circle
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C853).copy(alpha = 0.1f))
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.home_overall_protection),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isProtectionOn) stringResource(R.string.status_protected) else stringResource(R.string.status_inactive),
                        color = if (isProtectionOn) Color(0xFF00C853) else Color(0xFFFF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    if (!isProtectionOn) {
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (!isOverlayEnabled) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                } else if (!isAccessibilityEnabled) {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF4A9EF5), Color(0xFF7B61FF))
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.btn_enable_protection),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. SERVICE STATUS SECTION
            Text(
                stringResource(R.string.home_services_status),
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            ServiceCard(
                icon = Icons.Default.ShieldMoon,
                name = stringResource(R.string.feature_premium_shield_title),
                isActive = userProfile?.isPremium ?: false,
                onToggle = { onNavigateToProfile() }
            )
            ServiceCard(
                icon = Icons.Default.Layers,
                name = stringResource(R.string.home_overlay_permission),
                isActive = isOverlayEnabled,
                onToggle = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }
            )
            ServiceCard(
                icon = Icons.Default.Accessibility,
                name = stringResource(R.string.home_accessibility),
                isActive = isAccessibilityEnabled,
                onToggle = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            // 4. FEATURE HIGHLIGHT CARDS
            Text(
                stringResource(R.string.home_features),
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureHighlightCard(
                    title = "Premium Shield",
                    subtitle = "Bank himoyasi",
                    icon = Icons.Default.Security,
                    gradient = listOf(Color(0xFF4A9EF5), Color(0xFF16213E))
                )
                FeatureHighlightCard(
                    title = "OTP Guard",
                    subtitle = "Xabarlar himoyasi",
                    icon = Icons.Default.Sms,
                    gradient = listOf(Color(0xFF7B61FF), Color(0xFF16213E))
                )
                FeatureHighlightCard(
                    title = "APK Skaneri",
                    subtitle = "Xavfsiz tahlil",
                    icon = Icons.Default.QrCodeScanner,
                    gradient = listOf(Color(0xFF00C853), Color(0xFF16213E))
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(context) { showLanguageDialog = false }
    }
}

@Composable
fun ServiceCard(
    icon: ImageVector,
    name: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF4A9EF5), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    if (isActive) stringResource(R.string.status_active) else stringResource(R.string.status_inactive),
                    color = if (isActive) Color(0xFF00C853) else Color(0xFFFF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4A9EF5),
                    uncheckedThumbColor = Color(0xFF8A8A9A),
                    uncheckedTrackColor = Color(0xFF3A3A4A)
                )
            )
        }
    }
}

@Composable
fun FeatureHighlightCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>
) {
    Card(
        modifier = Modifier.size(140.dp, 100.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradient))
                .padding(12.dp)
        ) {
            Column {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.weight(1f))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LanguageDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                TextButton(onClick = { LocaleHelper.setLocale(context, "uz"); (context as? Activity)?.recreate() }) { Text("O'zbekcha 🇺🇿") }
                TextButton(onClick = { LocaleHelper.setLocale(context, "ru"); (context as? Activity)?.recreate() }) { Text("Русский 🇷🇺") }
                TextButton(onClick = { LocaleHelper.setLocale(context, "en"); (context as? Activity)?.recreate() }) { Text("English 🇺🇸") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedId = "${context.packageName}/${context.packageName}.service.InstallMonitorService"
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabledServices?.split(':')?.any { it.equals(expectedId, ignoreCase = true) } == true
}
