package com.catlover.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "catlover_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccessToken(token: String) {
        prefs.edit().putString("access", token).apply()
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh", token).apply()
    }

    fun saveUserId(id: String) {
        prefs.edit().putString("user_id", id).apply()
    }

    fun getAccessToken(): String? = prefs.getString("access", null)

    fun getRefreshToken(): String? = prefs.getString("refresh", null)
    
    fun getUserId(): String? = prefs.getString("user_id", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}