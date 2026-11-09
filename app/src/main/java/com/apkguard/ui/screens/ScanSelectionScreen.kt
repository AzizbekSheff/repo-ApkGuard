package com.apkguard.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkguard.R
import com.apkguard.model.ScanHistoryEntry
import com.apkguard.ui.theme.SuccessGreen
import com.apkguard.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSelectionScreen(
    viewModel: ScanViewModel,
    onApkSelected: (Uri) -> Unit,
    onNavigateToInstalledApps: () -> Unit,
    onEntryClick: (Long) -> Unit
) {
    val history by viewModel.history.collectAsState()
    
    val apkPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onApkSelected(it) } }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_scan), fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0D0D1A))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                ScanHeaderSection(
                    onSelectApk = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                    onInstalledApps = onNavigateToInstalledApps
                )
            }

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(R.string.recent_scans),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (history.isEmpty()) {
                item {
                    EmptyHistoryView()
                }
            } else {
                items(history.take(5)) { entry ->
                    RecentScanItem(entry, onClick = { onEntryClick(entry.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ScanHeaderSection(onSelectApk: () -> Unit, onInstalledApps: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                // Scan Line Animation effect placeholder or just icon
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF4A9EF5)
                )
                
                // Animated scanning line simulation
                val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
                val translateY by infiniteTransition.animateFloat(
                    initialValue = -40f,
                    targetValue = 40f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "translate"
                )
                
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(2.dp)
                        .offset(y = translateY.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFF4A9EF5), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.scan_apps_desc),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScanOptionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.btn_select_apk),
                    icon = Icons.Default.FileOpen,
                    onClick = onSelectApk,
                    containerColor = Color(0xFF4A9EF5)
                )
                ScanOptionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.btn_installed_apps),
                    icon = Icons.Default.Apps,
                    onClick = onInstalledApps,
                    containerColor = Color(0xFF16213E)
                )
            }
        }
    }
}

@Composable
fun ScanOptionButton(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
        }
    }
}

@Composable
fun RecentScanItem(entry: ScanHistoryEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16213E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Android, null, tint = Color(0xFF4A9EF5), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.apkName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(entry.packageName, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${entry.riskScore}%",
                color = if (entry.riskScore < 30) SuccessGreen else Color(0xFFFF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun EmptyHistoryView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color(0xFF1A1A2E))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.no_scans_yet), color = Color.Gray, fontSize = 14.sp)
    }
}
