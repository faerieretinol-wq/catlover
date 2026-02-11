package com.catlover.app.data

import android.content.Context
import java.util.Locale

class LocaleStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("catlover_prefs", Context.MODE_PRIVATE)

    fun setLocaleCode(code: String) {
        prefs.edit().putString("locale", code).apply()
    }

    fun getLocaleCode(): String {
        return prefs.getString("locale", "ru") ?: "ru"
    }
}