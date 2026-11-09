package com.apkguard.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkguard.R
import com.apkguard.model.ScanState

@Composable
fun ScanProgressScreen(
    state: ScanState.Scanning,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "progress"
    )

    // Qalqon belgisi uchun pulsatsiya animatsiyasi
    val infiniteTransition = rememberInfiniteTransition(label = "shield_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101820)) // To'q rangli fon
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Markazdagi ko'k qalqon belgisi
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp),
            tint = Color(0xFF3498DB)
        )

        Spacer(Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.scanning),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "step"
        ) { step ->
            Text(
                text = step,
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(40.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color.DarkGray, RoundedCornerShape(5.dp)),
            color = Color(0xFF3498DB),
            trackColor = Color.Transparent
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(60.dp))

        // Bekor qilish tugmasi
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.width(140.dp).height(48.dp),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
        ) {
            Text(stringResource(R.string.cancel), color = Color.White)
        }
    }
}
