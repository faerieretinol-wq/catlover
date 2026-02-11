package com.catlover.app.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun ZoomableImage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    Box(modifier = modifier.pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = maxOf(1f, minOf(scale * zoom, 3f)); val maxX = (size.width * (scale - 1)) / 2; val maxY = (size.height * (scale - 1)) / 2; offsetX = maxOf(-maxX, minOf(maxX, offsetX + pan.x)); offsetY = maxOf(-maxY, minOf(maxY, offsetY + pan.y)) } }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)) { content() }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare() } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView(factory = { PlayerView(context).apply { player = exoPlayer; useController = true; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } }, modifier = modifier)
}

@OptIn(UnstableApi::class)
@Composable
fun VideoCirclePlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember { 
        ExoPlayer.Builder(context).build().apply { 
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        } 
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    
    Box(
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { 
                PlayerView(context).apply { 
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                } 
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AudioPlayer(url: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentMillis by remember { mutableStateOf(0L) }
    var totalMillis by remember { mutableStateOf(0L) }
    
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { 
        setMediaItem(MediaItem.fromUri(url))
        prepare()
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) { 
                if (state == Player.STATE_READY) { totalMillis = duration }
                if (state == Player.STATE_ENDED) { 
                    seekTo(0); pause(); isPlaying = false; progress = 0f; currentMillis = 0 
                } 
            }
        })
    } }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentMillis = exoPlayer.currentPosition
            if (totalMillis > 0) progress = currentMillis.toFloat() / totalMillis.toFloat()
            delay(200)
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    fun formatTime(ms: Long): String {
        val s = (ms / 1000) % 60
        val m = (ms / 1000) / 60
        return "$m:${s.toString().padStart(2, '0')}"
    }

    Row(
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
            IconButton(
                onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentMillis), fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                Text(formatTime(totalMillis), fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

