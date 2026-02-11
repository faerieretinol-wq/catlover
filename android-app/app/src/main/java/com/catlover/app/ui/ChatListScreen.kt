package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatListScreen(onChatClick: (String) -> Unit, onNewChatClick: () -> Unit) {
    Scaffold(containerColor = CyberBlack) { p ->
        LazyColumn(modifier = Modifier.padding(p)) {
            // items
        }
    }
}
