package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.catlover.app.BuildConfig
import com.catlover.app.data.TokenStore
import com.catlover.app.data.WebRTCManager
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*
import kotlinx.coroutines.delay

@Composable
fun GroupCallScreen(chatId: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val tokenStore = remember { TokenStore(ctx) }
    val eglBase = remember { EglBase.create() }
    
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    
    // Remote tracks: userId -> VideoTrack
    val remoteTracks = remember { mutableStateMapOf<String, VideoTrack>() }
    val localRenderer = remember { SurfaceViewRenderer(ctx) }
    
    val socket = remember {
        val opts = IO.Options().apply { auth = mapOf("token" to tokenStore.getAccessToken()) }
        IO.socket(BuildConfig.BASE_URL, opts)
    }

    val rtc = remember {
        WebRTCManager(ctx, eglBase.eglBaseContext,
            onLocalIceCandidate = { uid, candidate ->
                socket.emit("group_call_signal", JSONObject().apply {
                    put("chatId", chatId)
                    put("toUserId", uid)
                    put("type", "candidate")
                    put("candidate", JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("sdp", candidate.sdp)
                    })
                })
            },
            onSdpGenerated = { uid, desc ->
                socket.emit("group_call_signal", JSONObject().apply {
                    put("chatId", chatId)
                    put("toUserId", uid)
                    put("type", if (desc.type == SessionDescription.Type.OFFER) "offer" else "answer")
                    put("sdp", desc.description)
                })
            },
            onRemoteTrack = { uid, track ->
                remoteTracks[uid] = track
            }
        )
    }

    LaunchedEffect(Unit) {
        localRenderer.init(eglBase.eglBaseContext, null)
        rtc.initLocalStream(localRenderer)
        
        socket.on(Socket.EVENT_CONNECT) {
            socket.emit("join", chatId)
            socket.emit("group_call_request", JSONObject().apply {
                put("chatId", chatId)
                put("type", "join_request")
            })
        }

        socket.on("incoming_group_call") { args ->
            val data = args[0] as JSONObject
            val fromUid = data.getString("fromUserId")
            // When someone asks to join, we (current participants) start call with them
            if (data.getString("type") == "join_request") {
                rtc.startCallWith(fromUid)
            }
        }

        socket.on("group_call_signal") { args ->
            val data = args[0] as JSONObject
            val fromUid = data.getString("fromUserId")
            when (data.getString("type")) {
                "offer" -> rtc.handleRemoteSdp(fromUid, data.getString("sdp"), true)
                "answer" -> rtc.handleRemoteSdp(fromUid, data.getString("sdp"), false)
                "candidate" -> {
                    val cand = data.getJSONObject("candidate")
                    rtc.addIceCandidate(fromUid, IceCandidate(cand.getString("sdpMid"), cand.getInt("sdpMLineIndex"), cand.getString("sdp")))
                }
            }
        }

        socket.connect()
    }

    DisposableEffect(Unit) {
        onDispose {
            socket.disconnect()
            rtc.close()
            localRenderer.release()
            eglBase.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        // VIDEO GRID
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Local User
            item {
                VideoTile("Me", localRenderer)
            }
            // Remote Users
            items(remoteTracks.toList()) { (uid, track) ->
                val renderer = remember(uid) { SurfaceViewRenderer(ctx).apply { init(eglBase.eglBaseContext, null) } }
                DisposableEffect(uid) {
                    track.addSink(renderer)
                    onDispose { renderer.release() }
                }
                VideoTile(uid.take(5), renderer)
            }
        }

        // CONTROLS
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Кнопка микрофона
            IconButton(
                onClick = { 
                    isMuted = !isMuted
                    rtc.toggleAudio(!isMuted)
                }, 
                modifier = Modifier.size(56.dp).background(
                    if (isMuted) Color.Red else Color.White.copy(alpha = 0.2f), 
                    CircleShape
                )
            ) { 
                Icon(
                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, 
                    null, 
                    tint = Color.White
                ) 
            }
            
            // Кнопка камеры
            IconButton(
                onClick = { 
                    isCameraOff = !isCameraOff
                    rtc.toggleVideo(!isCameraOff)
                }, 
                modifier = Modifier.size(56.dp).background(
                    if (isCameraOff) Color.Red else Color.White.copy(alpha = 0.2f), 
                    CircleShape
                )
            ) { 
                Icon(
                    if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam, 
                    null, 
                    tint = Color.White
                ) 
            }
            
            // Кнопка завершения звонка
            IconButton(onClick = onDismiss, modifier = Modifier.size(64.dp).background(Color.Red, CircleShape)) {
                Icon(Icons.Default.CallEnd, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun VideoTile(label: String, renderer: SurfaceViewRenderer) {
    Box(modifier = Modifier.aspectRatio(0.75f).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
        AndroidView(factory = { renderer }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) {
            Text(label, color = Color.White, fontSize = 10.sp)
        }
    }
}
