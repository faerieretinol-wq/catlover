package com.catlover.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class AppConfig(
    val server_url: String
)

object ConfigManager {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    // Ссылка на ваш raw файл на GitHub
    private const val CONFIG_URL = "https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/config.json"
    
    private var currentBaseUrl: String? = null

    suspend fun fetchBaseUrl(): String = withContext(Dispatchers.IO) {
        if (currentBaseUrl != null) return@withContext currentBaseUrl!!

        try {
            val request = Request.Builder().url(CONFIG_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to fetch config: ${response.code}")
                
                val body = response.body?.string() ?: throw Exception("Empty config body")
                val config = json.decodeFromString<AppConfig>(body)
                
                currentBaseUrl = config.server_url.trimEnd('/')
                currentBaseUrl!!
            }
        } catch (e: Exception) {
            android.util.Log.e("ConfigManager", "Error fetching base URL", e)
            // Fallback на дефолтный URL из BuildConfig если GitHub недоступен
            com.catlover.app.BuildConfig.BASE_URL
        }
    }

    fun getBaseUrl(): String {
        return currentBaseUrl ?: com.catlover.app.BuildConfig.BASE_URL
    }
}
