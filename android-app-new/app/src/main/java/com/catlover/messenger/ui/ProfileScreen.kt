package com.catlover.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(username: String, bio: String?, onEditClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CyberBlack)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(username, color = NeonCyan)
            Text(bio ?: "", color = NeonPurple)
        }
    }
}
