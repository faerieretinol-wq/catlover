package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegistrationScreen(onRegisterClick: (String, String, String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CyberBlack), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("JOIN NETWORK", color = NeonCyan, fontSize = 24.sp)
            Button(onClick = { onRegisterClick("","", "") }) { Text("INITIALIZE") }
        }
    }
}
