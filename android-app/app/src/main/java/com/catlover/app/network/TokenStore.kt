package com.catlover.app.network

import android.content.Context

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    fun getAccessToken(): String? = prefs.getString("token", null)
    fun saveAccessToken(token: String) = prefs.edit().putString("token", token).apply()
}
