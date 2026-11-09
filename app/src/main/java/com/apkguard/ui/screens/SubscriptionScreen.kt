package com.apkguard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.apkguard.ui.theme.ApkGuardTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.apkguard.R
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.apkguard.viewmodel.SubscriptionViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    var selectedAmount by remember { mutableStateOf(0.0) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCardDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val clipboardManager = LocalClipboardManager.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Handle Upload Status
    when (val status = uploadStatus) {
        is SubscriptionViewModel.UploadStatus.Success -> {
            Toast.makeText(context, stringResource(R.string.receipt_sent_success), Toast.LENGTH_LONG).show()
            viewModel.resetUploadStatus()
            showCardDialog = false
            selectedImageUri = null
        }
        is SubscriptionViewModel.UploadStatus.Error -> {
            Toast.makeText(context, "Error: ${status.message}", Toast.LENGTH_LONG).show()
            viewModel.resetUploadStatus()
        }
        else -> {}
    }

    if (showCardDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (uploadStatus !is SubscriptionViewModel.UploadStatus.Loading) {
                    showCardDialog = false 
                }
            },
            title = { Text(stringResource(R.string.payment_card)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.card_number_label), fontWeight = FontWeight.Bold)
                    Text(viewModel.getCardNumber(), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    
                    Text(stringResource(R.string.card_owner_label), fontWeight = FontWeight.Bold)
                    Text(viewModel.getCardOwner())
                    
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(viewModel.getCardNumber()))
                            Toast.makeText(context, context.getString(R.string.card_copy_msg), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.btn_copy).uppercase())
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    Text(stringResource(R.string.upload_receipt), fontWeight = FontWeight.Bold)
                    
                    if (selectedImageUri != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.select_receipt_img).uppercase())
                    }

                    Spacer(Modifier.height(8.dp))

                    if (uploadStatus is SubscriptionViewModel.UploadStatus.Loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Button(
                            onClick = {
                                if (selectedImageUri != null && selectedPlanId != null) {
                                    viewModel.uploadReceipt(selectedImageUri!!, selectedPlanId!!)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.error_no_receipt), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text(stringResource(R.string.send_confirmation).uppercase())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showCardDialog = false },
                    enabled = uploadStatus !is SubscriptionViewModel.UploadStatus.Loading
                ) { 
                    Text(stringResource(R.string.cancel)) 
                }
            }
        )
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text(stringResource(R.string.select_payment_method)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentOption(
                        name = stringResource(R.string.payment_card),
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            showPaymentDialog = false
                            showCardDialog = true
                        }
                    )
                    
                    // Technical Work Options
                    MaintenancePaymentOption(stringResource(R.string.payment_google_play))
                    MaintenancePaymentOption(stringResource(R.string.payment_click))
                    MaintenancePaymentOption(stringResource(R.string.payment_payme))
                    MaintenancePaymentOption(stringResource(R.string.payment_paynet))
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sub_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.sub_screen_subtitle),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 1. FREE PLAN
            PlanCard(
                title = stringResource(R.string.plan_free_title),
                price = stringResource(R.string.plan_free_price),
                description = stringResource(R.string.plan_free_desc),
                features = listOf(
                    stringResource(R.string.plan_free_feat1),
                    stringResource(R.string.plan_free_feat2),
                    stringResource(R.string.plan_free_feat3)
                ),
                buttonText = stringResource(R.string.plan_current),
                isCurrent = true,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // 2. PREMIUM PLAN
            PlanCard(
                title = stringResource(R.string.plan_premium_title),
                price = "2.99 $",
                priceInUzS = "39,999 so'm",
                description = stringResource(R.string.plan_premium_desc),
                features = listOf(
                    stringResource(R.string.feature_premium_shield_title),
                    stringResource(R.string.feature_otp_guard_title),
                    stringResource(R.string.feat_sig),
                    stringResource(R.string.profile_unlimited)
                ),
                buttonText = stringResource(R.string.plan_premium_btn),
                isCurrent = false,
                highlight = true,
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    selectedPlanId = "premium_monthly"
                    selectedAmount = 39999.0
                    showPaymentDialog = true
                }
            )

            Spacer(Modifier.height(16.dp))

            // 3. PRO PLAN
            PlanCard(
                title = stringResource(R.string.plan_pro_title),
                price = "29.99 $",
                priceInUzS = "389,999 so'm",
                description = stringResource(R.string.plan_pro_desc),
                features = listOf(
                    stringResource(R.string.plan_pro_desc),
                    stringResource(R.string.feat_code),
                    stringResource(R.string.feat_risk),
                    "AI Analysis"
                ),
                buttonText = stringResource(R.string.plan_pro_btn),
                isCurrent = false,
                color = Color(0xFFFFD700).copy(alpha = 0.2f), // Gold color
                onClick = {
                    selectedPlanId = "pro_yearly"
                    selectedAmount = 389999.0
                    showPaymentDialog = true
                }
            )
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                stringResource(R.string.sub_payment_info),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MaintenancePaymentOption(name: String) {
    val context = LocalContext.current
    Surface(
        onClick = { 
            Toast.makeText(context, context.getString(R.string.payment_under_maintenance), Toast.LENGTH_SHORT).show()
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Gray)
                Text(name, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Text(
                stringResource(R.string.payment_technical_work),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PaymentOption(name: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(name, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    priceInUzS: String? = null,
    description: String,
    features: List<String>,
    buttonText: String,
    isCurrent: Boolean,
    highlight: Boolean = false,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = if (highlight) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                if (highlight) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Text(price, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            
            if (priceInUzS != null) {
                Text(
                    text = "≈ $priceInUzS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
            
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(feature, fontSize = 14.sp)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCurrent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonText.uppercase(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SubscriptionScreenPreview() {
    ApkGuardTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlanCard(
                    title = "Boshlang'ich (Free)",
                    price = "0 so'm",
                    description = "Asosiy skanerlash",
                    features = listOf(
                        "Qo'lda skanerlash (3 marta/kun)",
                        "Oddiy ruxsatlar tahlili",
                        "Oxirgi 5 ta tarix"
                    ),
                    buttonText = "Hozirgi tarif",
                    isCurrent = true,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
