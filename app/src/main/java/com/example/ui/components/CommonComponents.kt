package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.KvmGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WindowsCyan

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (dotColor, text, bg) = when (status.uppercase()) {
        "RUNNING" -> Triple(KvmGreen, "RUNNING", KvmGreen.copy(alpha = 0.15f))
        "SUSPENDED", "PAUSED" -> Triple(WarningAmber, "SUSPENDED", WarningAmber.copy(alpha = 0.15f))
        else -> Triple(Color(0xFF94A3B8), "STOPPED", Slate800)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, dotColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            color = dotColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

@Composable
fun KvmChip(isKvm: Boolean, modifier: Modifier = Modifier) {
    val color = if (isKvm) KvmGreen else WindowsCyan
    val label = if (isKvm) "KVM ACCELERATED" else "TCG JIT (ARM64)"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.8.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun WindowsLogoMini(modifier: Modifier = Modifier, size: Int = 20) {
    // 2x2 grid of Windows 11 fluent squares
    Box(
        modifier = modifier.size(size.dp)
    ) {
        val half = (size / 2 - 1).coerceAtLeast(4)
        val pad = (size / 2 + 1)
        // Top left
        Box(
            modifier = Modifier
                .size(half.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFF00ADEF))
        )
        // Top right
        Box(
            modifier = Modifier
                .padding(start = pad.dp)
                .size(half.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFF0078D7))
        )
        // Bottom left
        Box(
            modifier = Modifier
                .padding(top = pad.dp)
                .size(half.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFF29B6F6))
        )
        // Bottom right
        Box(
            modifier = Modifier
                .padding(start = pad.dp, top = pad.dp)
                .size(half.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFF0288D1))
        )
    }
}
