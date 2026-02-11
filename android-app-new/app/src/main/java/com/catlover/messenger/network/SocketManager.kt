package com.catlover.messenger.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object SocketManager {
    private var socket: Socket? = null

    fun connect(token: String) {
        try {
            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                timeout = 20000
            }
            socket = IO.socket(ConfigManager.getBaseUrl(), opts)
            socket?.on(Socket.EVENT_CONNECT) { android.util.Log.d("Socket", "Connected") }
            socket?.connect()
        } catch (e: Exception) { }
    }

    fun onNewMessage(callback: (JSONObject) -> Unit) {
        socket?.on("new_message") { args -> if (args.isNotEmpty()) callback(args[0] as JSONObject) }
    }

    fun disconnect() { socket?.disconnect(); socket = null }
}
