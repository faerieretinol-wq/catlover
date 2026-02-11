package com.catlover.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class AppConfig(val server_url: String)

object ConfigManager {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private const val CONFIG_URL = "https://raw.githubusercontent.com/faerieretinol-wq/catlover/main/config.json"
    private var currentBaseUrl: String? = null

    suspend fun fetchBaseUrl(): String = withContext(Dispatchers.IO) {
        if (currentBaseUrl != null) return@withContext currentBaseUrl!!
        try {
            val request = Request.Builder().url(CONFIG_URL).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: throw Exception("Empty")
                currentBaseUrl = json.decodeFromString<AppConfig>(body).server_url.trimEnd('/')
                currentBaseUrl!!
            }
        } catch (e: Exception) { "https://catlover-production.up.railway.app" }
    }

    fun getBaseUrl(): String = currentBaseUrl ?: "https://catlover-production.up.railway.app"
}
