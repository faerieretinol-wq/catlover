package com.catlover.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.catlover.app.data.WebRTCManager
import io.socket.client.Socket
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
fun CallScreen(
    otherUserId: String,
    otherName: String,
    isVideo: Boolean,
    incoming: Boolean,
    isAcceptedByOther: Boolean,
    initialRemoteSdp: String?,
    remoteCandidates: List<IceCandidate>,
    socket: Socket?,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var callDuration by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var callAccepted by remember { mutableStateOf(false) }
    var isSocketConnected by remember { mutableStateOf(socket?.connected() ?: false) }

    val currentOtherUserId by rememberUpdatedState(otherUserId)
    val currentIsVideo by rememberUpdatedState(isVideo)
    val currentSocket by rememberUpdatedState(socket)

    var rtcInitialized by remember { mutableStateOf(false) }
    val eglBase = remember { EglBase.create() }
    
    val remoteRenderer = remember { SurfaceViewRenderer(ctx) }
    val localRenderer = remember { SurfaceViewRenderer(ctx) }

    val rtc = remember {
        WebRTCManager(ctx, eglBase.eglBaseContext,
            onLocalIceCandidate = { uid, candidate ->
                currentSocket?.emit("ice_candidate", JSONObject().apply {
                    put("toUserId", uid ?: currentOtherUserId)
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                    put("sdp", candidate.sdp)
                })
            },
            onSdpGenerated = { uid, desc ->
                if (desc.type == SessionDescription.Type.OFFER) {
                    currentSocket?.emit("call_request", JSONObject().apply {
                        put("toUserId", uid ?: currentOtherUserId)
                        put("type", if (currentIsVideo) "video" else "voice")
                        put("sdp", desc.description)
                        put("senderName", "User")
                    })
                } else {
                    currentSocket?.emit("call_response", JSONObject().apply {
                        put("toUserId", uid ?: currentOtherUserId)
                        put("accepted", true)
                        put("answer", desc.description)
                    })
                }
            },
            onRemoteTrack = { _, track ->
                track.addSink(remoteRenderer)
            }
        )
    }

    LaunchedEffect(Unit) {
        remoteRenderer.init(eglBase.eglBaseContext, null)
        remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        if (isVideo) {
            localRenderer.init(eglBase.eglBaseContext, null)
            localRenderer.setZOrderMediaOverlay(true)
        }

        delay(500)
        withContext(Dispatchers.IO) {
            rtc.initLocalStream(if (isVideo) localRenderer else null)
            rtcInitialized = true
            if (!incoming) {
                rtc.startCallWith(currentOtherUserId)
            }
        }
    }

    LaunchedEffect(isAcceptedByOther, initialRemoteSdp, rtcInitialized) {
        if (rtcInitialized && isAcceptedByOther && initialRemoteSdp != null) {
            callAccepted = true
            rtc.handleRemoteSdp(currentOtherUserId, initialRemoteSdp, false)
        }
    }

    var lastCandidateCount by remember { mutableStateOf(0) }
    LaunchedEffect(remoteCandidates.size, rtcInitialized) {
        if (rtcInitialized && remoteCandidates.size > lastCandidateCount) {
            for (i in lastCandidateCount until remoteCandidates.size) {
                rtc.addIceCandidate(currentOtherUserId, remoteCandidates[i])
            }
            lastCandidateCount = remoteCandidates.size
        }
    }

    DisposableEffect(Unit) { 
        onDispose { 
            rtc.close()
            remoteRenderer.release()
            localRenderer.release()
            eglBase.release()
        } 
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        if (callAccepted && isVideo) {
            AndroidView(factory = { remoteRenderer }, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(120.dp, 160.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                 AndroidView(factory = { localRenderer }, modifier = Modifier.fillMaxSize())
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isVideo || !callAccepted) {
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text(otherName.take(1).uppercase(), fontSize = 40.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(otherName, color = Color.White, fontSize = 24.sp)
            }
            Text(if (!callAccepted) (if (incoming) "Incoming..." else "Calling...") else "${callDuration / 60}:${(callDuration % 60).toString().padStart(2, '0')}", color = Color.White.copy(alpha = 0.7f))
        }

        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // Кнопка принятия звонка (только для входящих)
            if (!callAccepted && incoming) {
                IconButton(
                    onClick = { 
                        callAccepted = true
                        if (initialRemoteSdp != null) {
                            rtc.handleRemoteSdp(currentOtherUserId, initialRemoteSdp, true)
                        }
                    }, 
                    modifier = Modifier.size(72.dp).background(Color(0xFF4CAF50), CircleShape)
                ) { 
                    Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(32.dp)) 
                }
            }
            
            // Кнопки управления (только когда звонок принят)
            if (callAccepted) {
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
                
                // Кнопка камеры (только для видеозвонков)
                if (isVideo) {
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
                }
            }
            
            // Кнопка завершения звонка (всегда видна)
            IconButton(
                onClick = { 
                    socket?.emit("call_end", JSONObject().apply { put("toUserId", otherUserId) })
                    onDismiss()
                }, 
                modifier = Modifier.size(64.dp).background(Color.Red, CircleShape)
            ) { 
                Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp)) 
            }
        }
    }
}
