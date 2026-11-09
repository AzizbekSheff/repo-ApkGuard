package com.apkguard.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.apkguard.ui.theme.RiskHigh
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WarningOverlayManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null

    // Shubhali ilova ma'lumotlari uchun model
    data class SuspiciousApp(val name: String, val reason: String)

    fun showBankShieldOverlay(onScan: () -> Unit, onSkip: () -> Unit) {
        showOverlay(isFullScreen = true) {
            BankShieldUI(onScan = onScan, onSkip = { hide(); onSkip() })
        }
    }

    fun showScanningProgress() {
        showOverlay(isFullScreen = true) { ScanningUI() }
    }

    fun showScanResult(threatsFound: Int, suspiciousApps: List<SuspiciousApp> = emptyList(), onContinue: () -> Unit) {
        showOverlay(isFullScreen = true) {
            ScanResultUI(threatsFound, suspiciousApps, onContinue = { hide(); onContinue() })
        }
    }

    fun showOtpWarning() {
        showOverlay(isFullScreen = false) {
            GenericWarningUI(
                title = "OTP XAVFSIZLIK OGOHLANTIRISHI",
                message = "Maxfiy OTP kodi aniqlandi. Ehtiyot bo'ling!",
                icon = Icons.Default.Sms,
                iconColor = RiskHigh,
                onClose = { hide() }
            )
        }
    }

    private fun showOverlay(isFullScreen: Boolean, content: @Composable () -> Unit) {
        try {
            if (overlayView != null) hide()
            if (!Settings.canDrawOverlays(context)) return

            val layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                if (isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = if (isFullScreen) Gravity.CENTER else Gravity.TOP
                if (!isFullScreen) y = 100
            }

            overlayView = ComposeView(context).apply {
                setContent {
                    MaterialTheme {
                        Surface(color = if (isFullScreen) Color(0xFF101820) else Color.Transparent) {
                            content()
                        }
                    }
                }
                val owner = MyLifecycleOwner().apply {
                    performRestore(null)
                    handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                }
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(MyViewModelStoreOwner())
            }
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e("ApkGuardOverlay", "Error: ${e.message}")
        }
    }

    fun hide() {
        overlayView?.let {
            try { windowManager.removeViewImmediate(it) } catch (e: Exception) {}
            overlayView = null
        }
    }

    @Composable
    fun BankShieldUI(onScan: () -> Unit, onSkip: () -> Unit) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
            label = "scale"
        )

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77)))
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(160.dp).scale(scale).clip(CircleShape).background(Color(0xFFE0E1DD).copy(alpha = 0.1f)))
                    Box(modifier = Modifier.size(120.dp).scale(scale * 0.8f).clip(CircleShape).background(Color(0xFFE0E1DD).copy(alpha = 0.15f)))
                    Icon(Icons.Default.Shield, null, tint = Color(0xFFE0E1DD), modifier = Modifier.size(100.dp))
                }
                
                Spacer(Modifier.height(48.dp))
                
                Text(
                    text = "HIMOYA QALQONI",
                    color = Color(0xFFE0E1DD),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Siz bank ilovasini ochmoqdasiz. Xavfsizligingizni ta'minlash uchun tizimni tezkor skanerlashni tavsiya qilamiz.",
                    color = Color(0xFFE0E1DD).copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                
                Spacer(Modifier.height(64.dp))
                
                Button(
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E1DD), contentColor = Color(0xFF0D1B2A)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, null)
                    Spacer(Modifier.width(12.dp))
                    Text("TEZKOR SKANERLASH", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                Spacer(Modifier.height(20.dp))
                
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text("SKANERLAMASDAN DAVOM ETISH", color = Color(0xFFE0E1DD).copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    @Composable
    fun ScanningUI() {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B2A)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFFE0E1DD), modifier = Modifier.size(80.dp), strokeWidth = 6.dp)
                Spacer(Modifier.height(32.dp))
                Text("TAHLIL QILINMOQDA...", color = Color(0xFFE0E1DD), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("SMS va Accessibility ruxsatlari tekshirilmoqda", color = Color(0xFFE0E1DD).copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    @Composable
    fun ScanResultUI(threatsFound: Int, suspiciousApps: List<SuspiciousApp>, onContinue: () -> Unit) {
        val isSafe = threatsFound == 0
        Box(modifier = Modifier.fillMaxSize().background(if (isSafe) Color(0xFF1B4332) else Color(0xFF590D22))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Report,
                    contentDescription = null,
                    tint = Color(0xFFE0E1DD),
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = if (isSafe) "TIZIM XAVFSIZ" else "DIQQAT: XAVF!",
                    color = Color(0xFFE0E1DD),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isSafe) "Hech qanday shubhali ilova topilmadi." 
                           else "Qurilmangizda $threatsFound ta shubhali ilova aniqlandi:",
                    color = Color(0xFFE0E1DD).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp
                )
                
                if (!isSafe) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            items(suspiciousApps) { app ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(app.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Text(
                                        text = app.reason,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E1DD), contentColor = if (isSafe) Color(0xFF1B4332) else Color(0xFF590D22)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("DAVOM ETISH", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }

    @Composable
    fun GenericWarningUI(title: String, message: String, icon: ImageVector, iconColor: Color, onClose: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.ExtraBold, color = iconColor, fontSize = 16.sp)
                    Text(text = message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
            }
        }
    }

    private class MyLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val lifecycle get() = lifecycleRegistry
        override val savedStateRegistry get() = controller.savedStateRegistry
        fun handleLifecycleEvent(e: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(e)
        fun performRestore(s: android.os.Bundle?) = controller.performRestore(s)
    }
    private class MyViewModelStoreOwner : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
}
