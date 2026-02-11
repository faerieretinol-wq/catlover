package com.catlover.app.data

import android.content.Context
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.Timer
import java.util.TimerTask

class WebRTCManager(
    private val context: Context,
    private val eglContext: EglBase.Context,
    private val onLocalIceCandidate: (userId: String?, IceCandidate) -> Unit,
    private val onSdpGenerated: (userId: String?, SessionDescription) -> Unit,
    private val onRemoteTrack: (userId: String, VideoTrack) -> Unit,
    private val onConnectionDead: (userId: String) -> Unit = {}
) {
    private val factory: PeerConnectionFactory
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val dataChannels = ConcurrentHashMap<String, DataChannel>()
    private val lastPongTime = ConcurrentHashMap<String, Long>()
    
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var pingTimer: Timer? = null

    init {
        val encoderFactory = SoftwareVideoEncoderFactory()
        val decoderFactory = SoftwareVideoDecoderFactory()

        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
            
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setAudioAttributes(audioAttributes)
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
            
        audioDeviceModule.release()
        startPingTimer()
    }

    private fun startPingTimer() {
        pingTimer = Timer()
        pingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val now = System.currentTimeMillis()
                dataChannels.forEach { (userId, dc) ->
                    if (dc.state() == DataChannel.State.OPEN) {
                        val buffer = DataChannel.Buffer(java.nio.ByteBuffer.wrap("ping".toByteArray()), false)
                        dc.send(buffer)
                    }
                    
                    val lastPong = lastPongTime[userId] ?: now
                    if (now - lastPong > 15000) { // 15 seconds threshold
                        onConnectionDead(userId)
                    }
                }
            }
        }, 5000, 5000)
    }

    private val rtcConfig = PeerConnection.RTCConfiguration(
        listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
    ).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }

    fun initLocalStream(localRenderer: SurfaceViewRenderer?) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        if (localRenderer != null) {
            val videoSource = factory.createVideoSource(false)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext)
            videoCapturer = createCameraCapturer(context)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            videoCapturer?.startCapture(640, 480, 30)

            localVideoTrack = factory.createVideoTrack("VIDEO_" + UUID.randomUUID().toString(), videoSource)
            localVideoTrack?.addSink(localRenderer)
        }
    }

    private fun getOrCreateConnection(userId: String): PeerConnection {
        return peerConnections[userId] ?: createPeerConnection(userId).also { peerConnections[userId] = it }
    }

    private fun createPeerConnection(userId: String): PeerConnection {
        val pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) { onLocalIceCandidate(userId, candidate) }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.DISCONNECTED || state == PeerConnection.IceConnectionState.FAILED) {
                    onConnectionDead(userId)
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel) { setupDataChannel(userId, dc) }
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                if (receiver.track() is VideoTrack) {
                    onRemoteTrack(userId, receiver.track() as VideoTrack)
                }
            }
        })!!

        // Create Data Channel for ping-pong
        val dcInit = DataChannel.Init()
        val dc = pc.createDataChannel("ping-pong", dcInit)
        setupDataChannel(userId, dc)

        localVideoTrack?.let { pc.addTrack(it, listOf("group_call")) }
        
        val audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("AUDIO_" + UUID.randomUUID().toString(), audioSource)
        localAudioTrack?.let { pc.addTrack(it, listOf("group_call")) }

        return pc
    }

    private fun setupDataChannel(userId: String, dc: DataChannel) {
        dataChannels[userId] = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                val msg = String(data)
                if (msg == "ping") {
                    dc.send(DataChannel.Buffer(java.nio.ByteBuffer.wrap("pong".toByteArray()), false))
                } else if (msg == "pong") {
                    lastPongTime[userId] = System.currentTimeMillis()
                }
            }
        })
    }

    fun startCallWith(userId: String) {
        val pc = getOrCreateConnection(userId)
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc.setLocalDescription(SimpleSdpObserver { onSdpGenerated(userId, desc) }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    fun handleRemoteSdp(userId: String, sdp: String, isOffer: Boolean) {
        val pc = getOrCreateConnection(userId)
        val type = if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        val desc = SessionDescription(type, sdp)
        
        pc.setRemoteDescription(SimpleSdpObserver {
            if (isOffer) {
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription) {
                        pc.setLocalDescription(SimpleSdpObserver { onSdpGenerated(userId, answer) }, answer)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints())
            }
        }, desc)
    }

    fun addIceCandidate(userId: String, candidate: IceCandidate) {
        peerConnections[userId]?.addIceCandidate(candidate)
    }

    private fun createCameraCapturer(context: Context): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        for (name in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(name)) return enumerator.createCapturer(name, null)
        }
        return null
    }

    fun close() {
        pingTimer?.cancel()
        dataChannels.values.forEach { it.close() }
        dataChannels.clear()
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        factory.dispose()
    }
    
    // Управление аудио (микрофон)
    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }
    
    // Управление видео (камера)
    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        if (enabled) {
            videoCapturer?.startCapture(640, 480, 30)
        } else {
            try {
                videoCapturer?.stopCapture()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private class SimpleSdpObserver(val onDone: () -> Unit) : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() { onDone() }
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
