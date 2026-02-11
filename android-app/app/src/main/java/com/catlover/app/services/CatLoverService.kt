package com.catlover.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.catlover.app.BuildConfig
import com.catlover.app.R
import com.catlover.app.MainActivity
import com.catlover.app.IncomingCallActivity
import com.catlover.app.data.TokenStore
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatLoverService : Service() {
    companion object {
        var activeChatId: String? = null
        const val ACTION_ACCEPT_CALL = "com.catlover.app.ACCEPT_CALL"
        
        fun cancelCallNotification(ctx: Context) {
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(202)
        }
    }

    private var socket: Socket? = null
    private val channelId = "catlover_bg_service"
    private val alertChannelId = "catlover_alerts"
    private var heartbeatJob: kotlinx.coroutines.Job? = null

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                if (socket?.connected() == true) {
                    socket?.emit("heartbeat", org.json.JSONObject().apply { put("userId", TokenStore(applicationContext).getUserId()) })
                }
                kotlinx.coroutines.delay(30000) // каждые 30 секунд
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Обработка действий из уведомлений
        when (intent?.action) {
            "DECLINE_CALL" -> {
                val userId = intent.getStringExtra("CALL_USER_ID")
                if (userId != null) {
                    socket?.emit("call_end", org.json.JSONObject().apply { put("toUserId", userId) })
                }
                cancelCallNotification(this)
                return START_STICKY
            }
            "REPLY_MESSAGE" -> {
                val chatId = intent.getStringExtra("CHAT_ID")
                val remoteInput = androidx.core.app.RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence("key_text_reply")?.toString()
                
                if (chatId != null && !replyText.isNullOrBlank()) {
                    // Отправляем сообщение в фоновом режиме
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val api = com.catlover.app.network.ApiClient(TokenStore(applicationContext))
                            api.sendMessage(chatId, replyText)
                            
                            // Отменяем уведомление после успешной отправки
                            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.cancel(chatId.hashCode())
                            
                            // Показываем уведомление об успешной отправке
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    applicationContext,
                                    "Сообщение отправлено",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CatLoverService", "Failed to send reply", e)
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    applicationContext,
                                    "Ошибка отправки: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                return START_STICKY
            }
        }
        
        createNotificationChannels()
        
        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = 0
            if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (hasCam) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            
            // Fallback if no permissions yet
            try {
                if (type == 0) {
                    startForeground(1, createForegroundNotification())
                } else {
                    startForeground(1, createForegroundNotification(), type)
                }
            } catch (e: Exception) {
                // Fallback for Android 14+ if permissions missing or background start restriction
                try {
                     startForeground(1, createForegroundNotification())
                } catch (e2: Exception) {}
            }
        } else {
            startForeground(1, createForegroundNotification())
        }
        connectSocket()
        return START_STICKY
    }

    private fun connectSocket() {
        if (socket?.connected() == true) return
        val token = TokenStore(applicationContext).getAccessToken() ?: return
        val opts = IO.Options()
        opts.auth = mapOf("token" to token)
        
        try {
            socket = IO.socket(BuildConfig.BASE_URL, opts).apply {
                on(Socket.EVENT_CONNECT) { }
                on("reconnect") { 
                    val newToken = TokenStore(applicationContext).getAccessToken()
                    opts.auth = mapOf("token" to newToken)
                }
                on("notification") { args ->
                    try {
                        val data = args[0] as JSONObject
                        showPopupNotification(data.optString("title", "CatLover"), data.optString("body", ""))
                    } catch (e: Exception) {}
                }
                on("incoming_call") { args ->
                    try {
                        val data = args[0] as JSONObject
                        showCallNotification(
                            data.getString("fromUserId"),
                            data.optString("senderName", "User"),
                            data.getString("type") == "video"
                        )
                    } catch (e: Exception) {}
                }
                on("message") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val cid = data.optString("chatId")
                        if (cid != activeChatId) {
                            val senderName = data.optString("senderName", "Пользователь")
                            val messageBody = data.optString("body", "")
                            showMessageNotification(cid, senderName, messageBody)
                        }
                    } catch (e: Exception) {}
                }
                on("new_like") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val likerName = data.optString("likerName", "Кто-то")
                        showPopupNotification("Новый лайк", "$likerName лайкнул ваш пост")
                    } catch (e: Exception) {}
                }
                on("new_comment") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val commenterName = data.optString("commenterName", "Кто-то")
                        val commentText = data.optString("commentText", "")
                        showPopupNotification("Новый комментарий", "$commenterName: $commentText")
                    } catch (e: Exception) {}
                }
                on("friend_request") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val requesterName = data.optString("requesterName", "Кто-то")
                        showPopupNotification("Запрос в друзья", "$requesterName отправил вам запрос в друзья")
                    } catch (e: Exception) {}
                }
                on("friend_accepted") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val accepterName = data.optString("accepterName", "Кто-то")
                        showPopupNotification("Запрос принят", "$accepterName принял ваш запрос в друзья")
                    } catch (e: Exception) {}
                }
                on("channel_post") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val channelName = data.optString("channelName", "Канал")
                        val postText = data.optString("postText", "Новый пост")
                        showPopupNotification("Новый пост в $channelName 📢", postText)
                    } catch (e: Exception) {}
                }
                connect()
                startHeartbeat()
            }
        } catch (e: Exception) {}
    }

    private fun showCallNotification(userId: String, userName: String, isVideo: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Intent для принятия звонка
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("CALL_USER_ID", userId)
            putExtra("CALL_USER_NAME", userName)
            putExtra("CALL_IS_VIDEO", isVideo)
            putExtra("CALL_INCOMING", true)
            putExtra("CALL_ACTION", "accept")
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this, 101, acceptIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Intent для отклонения звонка
        val declineIntent = Intent(this, CatLoverService::class.java).apply {
            action = "DECLINE_CALL"
            putExtra("CALL_USER_ID", userId)
        }
        val declinePendingIntent = PendingIntent.getService(
            this, 103, declineIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Открываем полноэкранную Activity для входящего звонка
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("CALL_USER_ID", userId)
            putExtra("CALL_USER_NAME", userName)
            putExtra("CALL_IS_VIDEO", isVideo)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 102, fullScreenIntent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(if (isVideo) "Входящий видеозвонок" else "Входящий звонок")
            .setContentText(userName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setSound(android.provider.Settings.System.DEFAULT_RINGTONE_URI)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отклонить", declinePendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Принять", acceptPendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
            
        manager.notify(202, notification)
    }

    private fun showMessageNotification(chatId: String, senderName: String, messageBody: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // VIBRATION
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }

        // Intent для открытия чата
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_SCREEN", "chat")
            putExtra("CHAT_ID", chatId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, chatId.hashCode(), openIntent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Intent для быстрого ответа (RemoteInput)
        val replyLabel = "Ответить"
        val remoteInput = androidx.core.app.RemoteInput.Builder("key_text_reply")
            .setLabel(replyLabel)
            .build()
        
        val replyIntent = Intent(this, CatLoverService::class.java).apply {
            action = "REPLY_MESSAGE"
            putExtra("CHAT_ID", chatId)
        }
        val replyPendingIntent = PendingIntent.getService(
            this, chatId.hashCode() + 1, replyIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            replyLabel,
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        // Обработка специальных типов сообщений
        val displayText = when {
            messageBody == "🎤 Голосовое сообщение" -> "🎤 Голосовое сообщение"
            messageBody == "🎨 Sticker" -> "🎨 Стикер"
            messageBody == "📊 Poll" -> "📊 Опрос"
            messageBody.startsWith("File") -> "📎 Файл"
            else -> messageBody
        }

        val notification = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(senderName)
            .setContentText(displayText)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
            .setGroup("messages")
            .addAction(replyAction)
            .build()
            
        manager.notify(chatId.hashCode(), notification)
    }

    private fun showPopupNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_SCREEN", "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("CatLover")
            .setContentText("Приложение работает в фоновом режиме")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val bgChannel = NotificationChannel(channelId, "Background Service", NotificationManager.IMPORTANCE_MIN)
            val alertChannel = NotificationChannel(alertChannelId, "Alerts", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(bgChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        socket?.disconnect()
        super.onDestroy()
    }
}
