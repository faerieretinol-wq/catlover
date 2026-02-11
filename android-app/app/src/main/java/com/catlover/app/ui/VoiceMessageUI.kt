package com.catlover.app.ui

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.min

// Кнопка записи голосового (как в Telegram - долгое нажатие)
@Composable
fun VoiceRecordButton(
    onRecordingStart: (File) -> Unit,
    onRecordingStop: (File) -> Unit,
    onRecordingCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    
    // Анимация пульсации при записи
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Таймер записи
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }
    
    fun startRecording() {
        try {
            val file = File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            audioFile = file
            
            recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            onRecordingStart(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            audioFile?.let { onRecordingStop(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            audioFile?.delete()
            onRecordingCancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Box(modifier = modifier) {
        if (isRecording) {
            // UI во время записи
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.scale(scale).size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%02d:%02d", recordingTime / 60, recordingTime % 60),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Запись...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Кнопка отмены
                    IconButton(onClick = { cancelRecording() }) {
                        Icon(Icons.Default.Close, contentDescription = "Отменить", tint = Color.White)
                    }
                    // Кнопка отправки
                    IconButton(onClick = { stopRecording() }) {
                        Icon(Icons.Default.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            // Кнопка микрофона (долгое нажатие для записи)
            IconButton(
                onClick = { /* Обычное нажатие - ничего */ },
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { startRecording() }
                        )
                    }
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Удерживайте для записи",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Компонент для отображения голосового сообщения
@Composable
fun VoiceMessageBubble(
    audioUrl: String,
    duration: Int = 0,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    
    // Привязываем MediaPlayer к audioUrl для правильной очистки
    val player = remember(audioUrl) {
        mutableStateOf<MediaPlayer?>(null)
    }
    
    // Обновление прогресса
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && player.value != null) {
                currentPosition = player.value?.currentPosition ?: 0
                delay(100)
            }
        }
    }
    
    fun togglePlayback() {
        if (isPlaying) {
            player.value?.pause()
            isPlaying = false
        } else {
            if (player.value == null) {
                player.value = MediaPlayer().apply {
                    try {
                        setDataSource(audioUrl)
                        prepare()
                        setOnCompletionListener {
                            isPlaying = false
                            currentPosition = 0
                        }
                        start()
                        isPlaying = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                player.value?.start()
                isPlaying = true
            }
        }
    }
    
    DisposableEffect(audioUrl) {
        onDispose {
            player.value?.release()
            player.value = null
        }
    }
    
    Row(
        modifier = modifier
            .background(
                if (isMyMessage) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Кнопка play/pause
        IconButton(
            onClick = { togglePlayback() },
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                tint = Color.White
            )
        }
        
        // Прогресс-бар
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = if (player.value != null && player.value!!.duration > 0) {
                    currentPosition.toFloat() / player.value!!.duration.toFloat()
                } else 0f,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Время
            Text(
                text = if (player.value != null && player.value!!.duration > 0) {
                    val pos = currentPosition / 1000
                    val dur = player.value!!.duration / 1000
                    String.format("%02d:%02d / %02d:%02d", pos / 60, pos % 60, dur / 60, dur % 60)
                } else if (duration > 0) {
                    String.format("%02d:%02d", duration / 60, duration % 60)
                } else {
                    "00:00"
                },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
