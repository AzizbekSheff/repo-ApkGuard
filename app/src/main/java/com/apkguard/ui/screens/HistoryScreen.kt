package com.apkguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun HistoryScreen(
    history: List<ScanHistoryEntry>,
    onBack: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, stringResource(R.string.delete_all), tint = RiskHigh)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_scans), fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.scan_prompt), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { entry ->
                    HistoryCard(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) },
                        onDelete = { onDelete(entry.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RiskCritical) },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.clear_history_msg, history.size)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    showClearDialog = false
                }) { Text(stringResource(R.string.delete_all), color = RiskCritical) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun HistoryCard(
    entry: ScanHistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val df = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val decisionColor = when (entry.decision) {
        UserDecision.APPROVED -> RiskLow
        UserDecision.REJECTED -> RiskCritical
        UserDecision.PENDING  -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val decisionLabel = when (entry.decision) {
        UserDecision.APPROVED -> "✓ Approved"
        UserDecision.REJECTED -> "✕ Rejected"
        UserDecision.PENDING  -> "Pending"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Score circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                CircularProgressIndicator(
                    progress = { entry.riskScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = riskColor(entry.riskLevel),
                    trackColor = riskColor(entry.riskLevel).copy(alpha = 0.15f),
                    strokeWidth = 4.dp
                )
                Text(
                    "${entry.riskScore}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = riskColor(entry.riskLevel)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.apkName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(entry.packageName, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RiskBadge(entry.riskLevel)
                    Text(decisionLabel, fontSize = 11.sp, color = decisionColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterVertically))
                }
                Text(df.format(Date(entry.scannedAt)), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
