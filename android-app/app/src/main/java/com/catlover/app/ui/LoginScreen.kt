package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onLoginClick: (String, String) -> Unit, onNavigateToRegister: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CyberBlack), contentAlignment = Alignment.Center) {
        Column {
            Text("ACCESS GRANTED", color = NeonPurple, fontSize = 24.sp)
            Button(onClick = { onLoginClick("", "") }) { Text("AUTHENTICATE") }
        }
    }
}
