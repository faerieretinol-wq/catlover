package com.catlover.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.catlover.app.MainActivity
import com.catlover.app.network.SocketManager

class NeuralLinkService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(101, NotificationCompat.Builder(this, "neural_channel")
            .setContentTitle("CatLover")
            .setContentText("Neural Link Active")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setOngoing(true)
            .build())
        return START_STICKY
    }
    override fun onBind(intent: Intent?) = null
}
