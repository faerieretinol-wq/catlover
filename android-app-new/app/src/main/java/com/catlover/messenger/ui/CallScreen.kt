package com.catlover.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun CallScreen(username: String, onReject: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CyberBlack)) {
        Text("INCOMING CALL: $username", color = NeonCyan)
    }
}
