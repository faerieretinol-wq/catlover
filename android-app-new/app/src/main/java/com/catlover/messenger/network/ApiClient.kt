package com.catlover.messenger.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiClient {
    private const val BASE_URL = "https://catlover-production.up.railway.app"
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    
    var authToken: String? = null
    
    suspend fun register(email: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("username", username)
                put("password", password)
            }
            
            val request = Request.Builder()
                .url("$BASE_URL/api/auth/register")
                .post(json.toString().toRequestBody(JSON))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(body)
                authToken = jsonResponse.getString("token")
                Result.success(authToken!!)
            } else {
                Result.failure(Exception(body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            
            val request = Request.Builder()
                .url("$BASE_URL/api/auth/login")
                .post(json.toString().toRequestBody(JSON))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(body)
                authToken = jsonResponse.getString("token")
                Result.success(authToken!!)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChats(): Result<List<Chat>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/chat")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonArray = org.json.JSONArray(body)
                val chats = mutableListOf<Chat>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    chats.add(Chat(
                        id = obj.getString("id"),
                        title = obj.optString("title", "Chat"),
                        lastMessage = obj.optString("last_message", ""),
                        isGroup = obj.optBoolean("is_group", false)
                    ))
                }
                Result.success(chats)
            } else {
                Result.failure(Exception("Failed to fetch chats"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/profile/search?q=$query")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonArray = org.json.JSONArray(body)
                val users = mutableListOf<User>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    users.add(User(
                        id = obj.getString("id"),
                        username = obj.getString("username"),
                        avatarUrl = obj.optString("avatar_url", null),
                        status = obj.optString("status", "")
                    ))
                }
                Result.success(users)
            } else {
                Result.failure(Exception("Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/profile/me")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val obj = JSONObject(body)
                Result.success(UserProfile(
                    username = obj.getString("username"),
                    email = obj.getString("email"),
                    avatarUrl = obj.optString("avatar_url", null),
                    bio = obj.optString("bio", ""),
                    status = obj.optString("status", "")
                ))
            } else {
                Result.failure(Exception("Failed to fetch profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMessages(chatId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/chat/$chatId/messages")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonArray = org.json.JSONArray(body)
                val messages = mutableListOf<Message>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    messages.add(Message(
                        id = obj.getString("id"),
                        chatId = obj.getString("chat_id"),
                        senderId = obj.getString("sender_id"),
                        body = obj.optString("body", ""),
                        createdAt = obj.getString("created_at"),
                        isRead = obj.optBoolean("is_read", false)
                    ))
                }
                Result.success(messages)
            } else {
                Result.failure(Exception("Failed to fetch messages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendMessage(chatId: String, body: String): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("body", body)
            }
            
            val request = Request.Builder()
                .url("$BASE_URL/api/chat/$chatId/messages")
                .header("Authorization", "Bearer $authToken")
                .post(json.toString().toRequestBody(JSON))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val obj = JSONObject(responseBody)
                Result.success(Message(
                    id = obj.getString("id"),
                    chatId = obj.getString("chat_id"),
                    senderId = obj.getString("sender_id"),
                    body = obj.optString("body", ""),
                    createdAt = obj.getString("created_at"),
                    isRead = obj.optBoolean("is_read", false)
                ))
            } else {
                Result.failure(Exception("Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class Chat(
    val id: String,
    val title: String,
    val lastMessage: String,
    val isGroup: Boolean
)

data class User(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val status: String
)

data class UserProfile(
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val bio: String,
    val status: String
)

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val body: String,
    val createdAt: String,
    val isRead: Boolean
)
