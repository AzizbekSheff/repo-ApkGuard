package com.apkguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkguard.viewmodel.AuthViewModel
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.apkguard.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.apkguard.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToAdmin: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current
    
    val email = userProfile?.email ?: stringResource(R.string.profile_unknown)
    val username = userProfile?.username ?: stringResource(R.string.profile_user_placeholder)
    val scanCount = userProfile?.scanCount ?: 0
    val isPremium = userProfile?.premium ?: false
    val isAdmin = userProfile?.admin ?: false

    var showEditDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }

    // Ilova haqida dialogi
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text(stringResource(R.string.about_app), fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("APK Guard v1.0.2", fontWeight = FontWeight.SemiBold, color = Color(0xFF4A9EF5))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.accessibility_description), color = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Text("Ishlab chiquvchi: ApkGuard Team", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close), color = Color(0xFF4A9EF5))
                }
            }
        )
    }

    // Username tahrirlash dialogi
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text(stringResource(R.string.profile_edit_username), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text(stringResource(R.string.profile_new_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4A9EF5),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank()) {
                            viewModel.updateUsername(newUsername)
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A9EF5))
                ) {
                    Text(stringResource(R.string.profile_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.profile_logout), tint = Color(0xFFFF4444))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0D0D1A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            
            // Avatar
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A2E))
                    .border(2.dp, Color(0xFF4A9EF5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.take(1).uppercase(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4A9EF5)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(text = username, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = email, fontSize = 14.sp, color = Color.Gray)

            Spacer(Modifier.height(32.dp))

            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = stringResource(R.string.profile_scan_count), value = "$scanCount", icon = Icons.Default.Scanner)
                StatCard(modifier = Modifier.weight(1f), label = stringResource(R.string.profile_plan), value = if (isPremium) "PRO" else "FREE", icon = Icons.Default.WorkspacePremium)
            }

            Spacer(Modifier.height(24.dp))

            // Sozlamalar menyusi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ProfileMenuItem(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.profile_username),
                        subtitle = username,
                        onClick = {
                            newUsername = username
                            showEditDialog = true
                        }
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.profile_upgrade),
                        subtitle = if (isPremium) stringResource(R.string.profile_premium_active) else stringResource(R.string.profile_free),
                        onClick = onNavigateToSubscription
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about_app),
                        onClick = { showAboutDialog = true }
                    )

                    if (isAdmin) {
                        ProfileMenuItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "Admin Panel",
                            onClick = onNavigateToAdmin
                        )
                    }

                    HorizontalDivider(color = Color(0xFF0D0D1A), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileMenuItem(
                        icon = Icons.Default.SupportAgent,
                        title = stringResource(R.string.support_contact),
                        subtitle = "Telegram: @ApkGuardAdmin",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ApkGuardAdmin"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Telegram ilovasi topilmadi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color(0xFF4A9EF5), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D1A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF4A9EF5), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF3A3A4A), modifier = Modifier.size(20.dp))
        }
    }
}
