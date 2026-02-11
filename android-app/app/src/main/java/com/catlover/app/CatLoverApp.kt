package com.catlover.app

import android.app.Application
import android.content.res.Configuration
import com.catlover.app.data.LocaleStore
import com.catlover.app.utils.CrashHandler
import org.webrtc.PeerConnectionFactory
import java.util.Locale

class CatLoverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        
        // Initialize WebRTC once
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        applyLocale(LocaleStore(this).getLocaleCode())
    }

    private fun applyLocale(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}