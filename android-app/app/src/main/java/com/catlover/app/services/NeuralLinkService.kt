package com.catlover.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.catlover.app.MainActivity
import com.catlover.app.network.SocketManager
import com.catlover.app.network.TokenStore

class NeuralLinkService : Service() {
    private val CHANNEL_ID = "neural_link_channel"
    private val NOTIFICATION_ID = 101
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CatLover::NeuralLinkWakeLock")
        wakeLock?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = TokenStore(this).getAccessToken()
        if (token != null) {
            SocketManager.connect(token)
            SocketManager.onNewMessage { json ->
                showIncomingMessageNotification(
                    json.optString("sender_name", "Unknown Entity"),
                    json.optString("body", "Incoming encrypted payload...")
                )
            }
        }

        startForeground(NOTIFICATION_ID, createNotification("Neural Link Active | Connected"))
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
        SocketManager.disconnect()
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CatLover Network")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Не мешает звуками, но висит в шторке
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showIncomingMessageNotification(sender: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(sender)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Neural Link Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
