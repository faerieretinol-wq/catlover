package com.catlover.app.data.local

import android.content.Context
import android.content.SharedPreferences

// Простое хранилище профилей без Room Database
class SimpleProfileStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    
    fun saveProfile(userId: String, username: String, avatarUrl: String?, bio: String?) {
        prefs.edit().apply {
            putString("${userId}_username", username)
            putString("${userId}_avatar", avatarUrl)
            putString("${userId}_bio", bio)
            apply()
        }
    }
    
    fun getProfile(userId: String): ProfileData? {
        val username = prefs.getString("${userId}_username", null) ?: return null
        return ProfileData(
            userId = userId,
            username = username,
            avatarUrl = prefs.getString("${userId}_avatar", null),
            bio = prefs.getString("${userId}_bio", null)
        )
    }
    
    fun deleteProfile(userId: String) {
        prefs.edit().apply {
            remove("${userId}_username")
            remove("${userId}_avatar")
            remove("${userId}_bio")
            apply()
        }
    }
}

data class ProfileData(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?
)
