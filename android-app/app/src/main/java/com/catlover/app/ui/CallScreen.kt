package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CallScreen(
    username: String,
    isIncoming: Boolean,
    isVideo: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(CyberGray)
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(username.take(1), color = NeonCyan, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(username.uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (isIncoming) "INCOMING SIGNAL..." else "ESTABLISHING CONNECTION...",
                color = if (isIncoming) NeonPurple else NeonCyan,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isIncoming) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF00FF41))
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
                    }
                }
                
                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Red)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun VideoCircle(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(CyberGray)
            .border(2.dp, NeonPurple, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Здесь будет VideoPlayer для проигрывания "кружка"
        Icon(Icons.Default.Videocam, contentDescription = null, tint = NeonPurple.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
    }
}
