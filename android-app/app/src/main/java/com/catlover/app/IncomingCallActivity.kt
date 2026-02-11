package com.catlover.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.app.ui.GlassBackground
import com.catlover.app.services.CatLoverService

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Показать экран поверх блокировки
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        val userId = intent.getStringExtra("CALL_USER_ID") ?: ""
        val userName = intent.getStringExtra("CALL_USER_NAME") ?: "Пользователь"
        val isVideo = intent.getBooleanExtra("CALL_IS_VIDEO", false)
        
        setContent {
            MaterialTheme {
                IncomingCallScreen(
                    userName = userName,
                    isVideo = isVideo,
                    onAccept = {
                        // Открыть MainActivity с параметрами звонка
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("CALL_USER_ID", userId)
                            putExtra("CALL_USER_NAME", userName)
                            putExtra("CALL_IS_VIDEO", isVideo)
                            putExtra("CALL_INCOMING", true)
                        }
                        startActivity(mainIntent)
                        CatLoverService.cancelCallNotification(this)
                        finish()
                    },
                    onDecline = {
                        CatLoverService.cancelCallNotification(this)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun IncomingCallScreen(
    userName: String,
    isVideo: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Информация о звонке
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Аватар
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = userName.take(2).uppercase()
                    Text(
                        text = if (initials.isEmpty()) "?" else initials,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    text = userName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = if (isVideo) "Входящий видеозвонок..." else "Входящий звонок...",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Кнопка отклонить
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onDecline,
                        modifier = Modifier.size(72.dp),
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Отклонить",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Отклонить",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                // Кнопка принять
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onAccept,
                        modifier = Modifier.size(72.dp),
                        containerColor = Color.Green,
                        contentColor = Color.White
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Принять",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Принять",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
