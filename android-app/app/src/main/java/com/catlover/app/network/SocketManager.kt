package com.catlover.app.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

object SocketManager {
    private var socket: Socket? = null

    fun connect(token: String) {
        try {
            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                // Настройка переподключения
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                
                // Настройка Heartbeat (Ping/Pong)
                // Railway/Cloudflare таймауты обычно 60 сек, ставим 30 сек
                timeout = 20000
            }
            socket = IO.socket(ConfigManager.getBaseUrl(), opts)
            
            socket?.on(Socket.EVENT_CONNECT) {
                android.util.Log.d("SocketManager", "✅ Neural Link Established")
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                android.util.Log.d("SocketManager", "🔌 Neural Link Severed - Reconnecting...")
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                android.util.Log.e("SocketManager", "❌ Connection Error: ${args.getOrNull(0)}")
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            android.util.Log.e("SocketManager", "❌ URI Syntax Error", e)
        }
    }

    socket?.on("call_offer") { args ->
        if (args.isNotEmpty()) {
            callback(args[0] as JSONObject)
        }
    }

    fun sendCallOffer(toUserId: String, offer: JSONObject) {
        socket?.emit("call_offer", JSONObject().apply {
            put("to", toUserId)
            put("offer", offer)
        })
    }

    fun isConnected(): Boolean = socket?.connected() ?: false

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
}
