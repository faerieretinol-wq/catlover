package com.catlover.app.network

import com.catlover.app.BuildConfig
import com.catlover.app.data.TokenStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private val json = Json { ignoreUnknownKeys = true }
private val media = "application/json; charset=utf-8".toMediaType()

@Serializable
data class RegisterRequest(val email: String, val username: String, val password: String)
@Serializable
data class RegisterResponse(val userId: String, @SerialName("accessToken") val accessToken: String, @SerialName("refreshToken") val refreshToken: String)
@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class LoginResponse(val userId: String, @SerialName("accessToken") val accessToken: String, @SerialName("refreshToken") val refreshToken: String)
@Serializable
data class RefreshResponse(@SerialName("accessToken") val accessToken: String, @SerialName("refreshToken") val refreshToken: String)
@Serializable
data class ProfileMeResponse(
    @SerialName("userId") val userId: String, 
    val username: String, 
    val avatarUrl: String? = null, 
    val bio: String? = null, 
    val status: String? = null, 
    val isVerified: Boolean = false,
    val role: String = "user"
)
@Serializable
data class SettingsResponse(val settings: String, val updatedAt: String)
@Serializable
data class ProfileUpdateRequest(val avatarUrl: String? = null, val bio: String? = null, val status: String? = null)
@Serializable
data class PostItem(val id: String, val userId: String, val username: String? = null, val avatarUrl: String? = null, val body: String, val imageUrl: String? = null, val createdAt: String, val likes: Int, val comments: Int, val likedByMe: Boolean = false, val savedByMe: Boolean = false, val isFollowing: Int? = 0)
@Serializable
data class PostsResponse(val posts: List<PostItem>, val hasMore: Boolean = false)
@Serializable
data class CreatePostRequest(val body: String, val imageUrl: String? = null, val channelId: String? = null)
@Serializable
data class UserSummary(val id: String, val username: String, val avatarUrl: String? = null, val bio: String? = null, val status: String? = null, val isFollowing: Int? = 0)
@Serializable
data class SearchResponse(val users: List<UserSummary>, val hasMore: Boolean = false)
@Serializable
data class CreateChatRequest(val isGroup: Boolean, val type: String = "group", val memberIds: List<String>? = null)
@Serializable
data class CreateChatResponse(val chatId: String)
@Serializable
data class ChatDetails(val id: String, val title: String? = null, val isGroup: Boolean, val type: String = "group", val isVerified: Boolean = false, val myRole: String = "member", val memberIds: List<String>, val recipientName: String? = null, val recipientAvatar: String? = null, val username: String? = null)
@Serializable
data class ChatSummary(val id: String, val title: String? = null, val isGroup: Boolean = false, val type: String = "group", val isVerified: Boolean = false, val createdAt: String, val lastMessage: String? = null, val lastMessageAt: String? = null, val unreadCount: Int = 0, val avatarUrl: String? = null, val recipientName: String? = null, val recipientAvatar: String? = null)
@Serializable
data class ChatsResponse(val chats: List<ChatSummary>)

@Serializable
data class MessageReaction(val userId: String, val reaction: String)

@Serializable
data class MessageItem(
    val id: String, 
    val chatId: String, 
    val senderId: String, 
    val senderName: String? = null, 
    val senderAvatar: String? = null, 
    val body: String, 
    val attachmentPath: String? = null,
    val pollId: String? = null,
    val isRead: Int = 0, 
    val createdAt: String, 
    val expiresAt: String? = null,
    val editedAt: String? = null,
    val forwardedFrom: String? = null,
    val reactions: List<MessageReaction> = emptyList()
)

@Serializable
data class MessagesResponse(val messages: List<MessageItem>)
@Serializable
data class SendMessageRequest(val body: String, val attachmentPath: String? = null, val pollId: String? = null, val ttl: Int? = null)
@Serializable
data class CommentItem(val id: String, val postId: String, val userId: String, val username: String? = null, val avatarUrl: String? = null, val body: String, val createdAt: String)
@Serializable
data class CommentsResponse(val comments: List<CommentItem>)
@Serializable
data class UploadResponse(val url: String)
@Serializable
data class SocialStatsResponse(val followers: Int, val following: Int, val posts: Int, val isFollowing: Boolean)
@Serializable
data class PublicKeyResponse(val publicKey: String)
@Serializable
data class NotificationItem(val id: String, val fromUserId: String, val fromUsername: String, val fromAvatar: String? = null, val type: String, val targetId: String? = null, val body: String? = null, val isRead: Int = 0, val createdAt: String)
@Serializable
data class NotificationsResponse(val notifications: List<NotificationItem>)
@Serializable
data class UserStories(val userId: String, val username: String, val avatarUrl: String? = null, val stories: List<StoryItem>)
@Serializable
data class StoryItem(val id: String, val imageUrl: String, val createdAt: String)
@Serializable
data class StoriesResponse(val feed: List<UserStories>)
@Serializable
data class CreateStoryRequest(val imageUrl: String)
@Serializable
data class UserLike(val id: String, val username: String, val avatarUrl: String? = null)
@Serializable
data class LikesResponse(val likes: List<UserLike>)
@Serializable
data class PublicProfileResponse(val username: String, val avatarUrl: String? = null, val bio: String? = null, val status: String? = null, val shareableStyle: Boolean = false, val latestShareId: String? = null)
@Serializable
data class ChatMember(val userId: String, val username: String, val avatarUrl: String? = null, val role: String, val joinedAt: String)
@Serializable
data class ChatMembersResponse(val members: List<ChatMember>)

@Serializable
data class StickerItem(val id: String, val packName: String, val stickerName: String, val filePath: String, val createdAt: String)
@Serializable
data class StickersResponse(val stickers: List<StickerItem>)
@Serializable
data class StickerPack(val packName: String, val stickerCount: Int)
@Serializable
data class StickerPacksResponse(val packs: List<StickerPack>)
@Serializable
data class GifItem(val gifUrl: String, val addedAt: String)
@Serializable
data class FavoriteGifsResponse(val gifs: List<GifItem>)

@Serializable
data class CreatePollResponse(val pollId: String)
@Serializable
data class PollOption(val id: String, val optionText: String, val optionOrder: Int, val voteCount: Int, val votedByMe: Int? = 0)
@Serializable
data class PollResponse(
    val id: String,
    val postId: String? = null,
    val chatId: String? = null,
    val creatorId: String,
    val question: String,
    val multipleChoice: Boolean,
    val anonymous: Boolean,
    val expiresAt: String? = null,
    val createdAt: String,
    val options: List<PollOption>,
    val totalVotes: Int
)

@Serializable
data class ChannelInfo(val id: String, val title: String, val username: String? = null, val description: String? = null, val subscribers: Int, val isVerified: Boolean = false, val isSubscribed: Boolean = false, val avatarUrl: String? = null)
@Serializable
data class ChannelsSearchResponse(val channels: List<ChannelInfo>)
@Serializable
data class CreateChannelRequest(val title: String, val username: String, val description: String? = null, val isPublic: Boolean = true, val commentsEnabled: Boolean = false, val avatarUrl: String? = null)
@Serializable
data class CreateChannelResponse(val channelId: String, val username: String, val title: String, val description: String? = null, val isPublic: Boolean, val commentsEnabled: Boolean, val avatarUrl: String? = null)
@Serializable
data class UpdateChannelRequest(val title: String? = null, val description: String? = null, val commentsEnabled: Boolean? = null, val avatarUrl: String? = null)

@Serializable
data class Setup2FAResponse(val secret: String, val qrCode: String, val backupCodes: List<String>)
@Serializable
data class AuthStatusResponse(val twoFactorEnabled: Boolean, val emailVerified: Boolean)

@Serializable
data class ForgotPasswordResponse(val ok: Boolean, val message: String, @SerialName("_dev_code") val devCode: String? = null)
@Serializable
data class ResetPasswordResponse(val ok: Boolean, val message: String)

@Serializable
data class StickerBotChatResponse(val chatId: String, val botId: String, val botName: String)

class ApiClient(private val tokens: TokenStore) {
    private val base: String
        get() = ConfigManager.getBaseUrl().trimEnd('/')
    
    companion object {
        private val refreshMutex = Mutex()
        fun formatMediaUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            if (url.startsWith("http")) return url
            val base = ConfigManager.getBaseUrl().trimEnd('/')
            val path = url.trimStart('/')
            return "$base/$path"
        }
    }
    
    private fun buildClient(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Accept-Language", "ru")
                    .method(original.method, original.body)
                    .build()
                
                android.util.Log.d("ApiClient", "=== REQUEST ===")
                android.util.Log.d("ApiClient", "URL: ${request.url}")
                android.util.Log.d("ApiClient", "Method: ${request.method}")
                android.util.Log.d("ApiClient", "Headers: ${request.headers}")
                
                try {
                    val response = chain.proceed(request)
                    android.util.Log.d("ApiClient", "=== RESPONSE ===")
                    android.util.Log.d("ApiClient", "Code: ${response.code}")
                    android.util.Log.d("ApiClient", "Message: ${response.message}")
                    android.util.Log.d("ApiClient", "Success: ${response.isSuccessful}")
                    response
                } catch (e: Exception) {
                    android.util.Log.e("ApiClient", "=== ERROR ===")
                    android.util.Log.e("ApiClient", "Exception: ${e.javaClass.simpleName}")
                    android.util.Log.e("ApiClient", "Message: ${e.message}")
                    android.util.Log.e("ApiClient", "Stack trace:", e)
                    throw e
                }
            }
        
        // Если pin пустой - отключаем SSL проверку (для Cloudflare Tunnel)
        val pin = BuildConfig.CERT_PIN_SHA256
        if (pin.isBlank()) {
            android.util.Log.d("ApiClient", "Certificate pinning disabled - using unsafe SSL bypass")
            try {
                // Создаем TrustManager который доверяет всем сертификатам
                val trustAllCerts = object : javax.net.ssl.X509TrustManager {
                    @Throws(java.security.cert.CertificateException::class)
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                        android.util.Log.d("ApiClient", "checkClientTrusted called")
                    }
                    
                    @Throws(java.security.cert.CertificateException::class)
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                        android.util.Log.d("ApiClient", "checkServerTrusted called - accepting all certificates")
                    }
                    
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                        return arrayOf()
                    }
                }
                
                // Создаем SSLContext с нашим TrustManager
                val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(trustAllCerts), java.security.SecureRandom())
                
                // Применяем SSLSocketFactory и HostnameVerifier
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts)
                builder.hostnameVerifier { hostname, session ->
                    android.util.Log.d("ApiClient", "HostnameVerifier: accepting hostname=$hostname")
                    true
                }
                
                android.util.Log.d("ApiClient", "✅ SSL verification bypass configured successfully")
            } catch (e: Exception) {
                android.util.Log.e("ApiClient", "❌ Failed to configure SSL bypass", e)
                throw e
            }
        } else {
            android.util.Log.d("ApiClient", "Certificate pinning enabled for: ${base.toHttpUrl().host}")
            val pinner = okhttp3.CertificatePinner.Builder()
                .add(base.toHttpUrl().host, pin)
                .build()
            builder.certificatePinner(pinner)
        }
        
        return builder
    }
    
    private val client = buildClient().build()

    suspend fun withAuth(reqBuilder: Request.Builder): String = withContext(Dispatchers.IO) {
        val access = tokens.getAccessToken() ?: ""
        val req = reqBuilder.header("Authorization", "Bearer $access").build()
        val resp = client.newCall(req).execute()
        var responseText = resp.body?.string().orEmpty()
        if (resp.code == 401) {
            resp.close()
            val newToken = refreshMutex.withLock {
                val currentAccess = tokens.getAccessToken() ?: ""
                if (currentAccess != access && currentAccess.isNotEmpty()) currentAccess
                else {
                    try { val refreshed = refreshToken(); tokens.saveAccessToken(refreshed.accessToken); tokens.saveRefreshToken(refreshed.refreshToken); refreshed.accessToken }
                    catch (e: Exception) { tokens.clear(); throw e }
                }
            }
            val retryReq = reqBuilder.header("Authorization", "Bearer $newToken").build()
            client.newCall(retryReq).execute().use { retryResp ->
                val retryText = retryResp.body?.string().orEmpty()
                if (!retryResp.isSuccessful) throw Exception(retryText)
                return@withContext retryText
            }
        } else {
            resp.use { if (!it.isSuccessful) throw Exception(responseText); return@withContext responseText }
        }
    }

    private suspend fun refreshToken(): RefreshResponse = withContext(Dispatchers.IO) {
        val refresh = tokens.getRefreshToken() ?: throw Exception("No refresh token")
        val body = "{\"refreshToken\":\"$refresh\"}".toRequestBody(media)
        val request = Request.Builder().url("$base/api/auth/refresh").post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw Exception(text)
            return@withContext json.decodeFromString(RefreshResponse.serializer(), text)
        }
    }

    suspend fun register(r: RegisterRequest): RegisterResponse = withContext(Dispatchers.IO) {
        val body = json.encodeToString(RegisterRequest.serializer(), r).toRequestBody(media)
        val request = Request.Builder().url("$base/api/auth/register").post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception(text)
            return@withContext json.decodeFromString(RegisterResponse.serializer(), text)
        }
    }

    suspend fun login(r: LoginRequest): LoginResponse = withContext(Dispatchers.IO) {
        val body = json.encodeToString(LoginRequest.serializer(), r).toRequestBody(media)
        val request = Request.Builder().url("$base/api/auth/login").post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception(text)
            return@withContext json.decodeFromString(LoginResponse.serializer(), text)
        }
    }

    suspend fun getProfileMe(): ProfileMeResponse {
        val response = withAuth(Request.Builder().url("$base/api/profile/me").get())
        android.util.Log.d("ApiClient", "📥 Profile response: $response")
        return json.decodeFromString(ProfileMeResponse.serializer(), response)
    }
    suspend fun deleteAccount() { withAuth(Request.Builder().url("$base/api/profile/me").delete()) }
    suspend fun updateProfile(r: ProfileUpdateRequest) { withAuth(Request.Builder().url("$base/api/profile/me").post(json.encodeToString(ProfileUpdateRequest.serializer(), r).toRequestBody(media))) }
    suspend fun getPosts(f: Boolean, beforeId: String? = null, u: String? = null, channelId: String? = null): PostsResponse {
        val url = "$base/api/posts?followed=$f" + (if (beforeId != null) "&beforeId=$beforeId" else "") + (if (u != null) "&userId=$u" else "") + (if (channelId != null) "&channelId=$channelId" else "")
        return json.decodeFromString(PostsResponse.serializer(), withAuth(Request.Builder().url(url).get()))
    }
    suspend fun createPost(t: String, i: String?, channelId: String? = null) { withAuth(Request.Builder().url("$base/api/posts").post(json.encodeToString(CreatePostRequest.serializer(), CreatePostRequest(t, i, channelId)).toRequestBody(media))) }
    suspend fun editPost(postId: String, newBody: String) { withAuth(Request.Builder().url("$base/api/posts/$postId").put("{\"body\":\"$newBody\"}".toRequestBody(media))) }
    suspend fun toggleLike(id: String) { withAuth(Request.Builder().url("$base/api/posts/$id/like").post("".toRequestBody(media))) }
    suspend fun getPostLikes(id: String): LikesResponse = json.decodeFromString(LikesResponse.serializer(), withAuth(Request.Builder().url("$base/api/posts/$id/likes").get()))
    suspend fun deletePost(id: String) { withAuth(Request.Builder().url("$base/api/posts/$id").delete()) }
    suspend fun deleteComment(postId: String, commentId: String) { withAuth(Request.Builder().url("$base/api/posts/$postId/comments/$commentId").delete()) }
    suspend fun getComments(id: String): CommentsResponse = json.decodeFromString(CommentsResponse.serializer(), withAuth(Request.Builder().url("$base/api/posts/$id/comments").get()))
    suspend fun addComment(id: String, b: String) { withAuth(Request.Builder().url("$base/api/posts/$id/comments").post(json.encodeToString(SendMessageRequest.serializer(), SendMessageRequest(b)).toRequestBody(media))) }
    suspend fun searchUsers(q: String, limit: Int = 20, offset: Int = 0): SearchResponse = json.decodeFromString(SearchResponse.serializer(), withAuth(Request.Builder().url("$base/api/social/search?q=$q&limit=$limit&offset=$offset").get()))
    suspend fun followUser(id: String) { withAuth(Request.Builder().url("$base/api/social/follow/$id").post("".toRequestBody(media))) }
    suspend fun unfollowUser(id: String) { withAuth(Request.Builder().url("$base/api/social/unfollow/$id").post("".toRequestBody(media))) }
    suspend fun getSocialStats(id: String): SocialStatsResponse = json.decodeFromString(SocialStatsResponse.serializer(), withAuth(Request.Builder().url("$base/api/social/stats/$id").get()))
    suspend fun getPublicProfile(u: String): PublicProfileResponse = json.decodeFromString(PublicProfileResponse.serializer(), withAuth(Request.Builder().url("$base/api/profile/public/$u").get()))
    suspend fun getChats(): ChatsResponse = json.decodeFromString(ChatsResponse.serializer(), withAuth(Request.Builder().url("$base/api/chat").get()))
    suspend fun getChatDetails(id: String): ChatDetails = json.decodeFromString(ChatDetails.serializer(), withAuth(Request.Builder().url("$base/api/chat/$id").get()))
    suspend fun markChatAsRead(id: String) { withAuth(Request.Builder().url("$base/api/chat/$id/read").post("".toRequestBody())) }
    suspend fun addMembers(id: String, uids: List<String>) { withAuth(Request.Builder().url("$base/api/chat/$id/members").post("{\"userIds\":${uids.joinToString("\",\"", "[\"", "\"]")}}".toRequestBody(media))) }
    suspend fun addMember(chatId: String, userId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/members").post("{\"userId\":\"$userId\"}".toRequestBody(media))) }
    suspend fun removeMember(chatId: String, userId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/members/$userId").delete()) }
    suspend fun changeMemberRole(chatId: String, userId: String, role: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/members/$userId/role").post("{\"role\":\"$role\"}".toRequestBody(media))) }
    suspend fun updateChatTitle(chatId: String, title: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/title").put("{\"title\":\"$title\"}".toRequestBody(media))) }
    suspend fun getChatMembers(chatId: String): ChatMembersResponse = json.decodeFromString(ChatMembersResponse.serializer(), withAuth(Request.Builder().url("$base/api/chat/$chatId/members").get()))
    suspend fun addChatMember(chatId: String, userId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/members").post("{\"userId\":\"$userId\"}".toRequestBody(media))) }
    suspend fun removeChatMember(chatId: String, userId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/members/$userId").delete()) }
    suspend fun updateMemberRole(chatId: String, userId: String, role: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/members/$userId/role").put("{\"role\":\"$role\"}".toRequestBody(media))) }
    suspend fun editMessage(chatId: String, messageId: String, newBody: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/messages/$messageId").put("{\"body\":\"$newBody\"}".toRequestBody(media))) }
    suspend fun forwardMessage(chatId: String, messageId: String, targetChatId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/messages/$messageId/forward").post("{\"targetChatId\":\"$targetChatId\"}".toRequestBody(media))) }
    suspend fun deleteMessageReaction(chatId: String, msgId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/messages/$msgId/reaction").delete()) }
    suspend fun deleteChat(id: String) { withAuth(Request.Builder().url("$base/api/chat/$id").delete()) }
    suspend fun createChat(r: CreateChatRequest): CreateChatResponse = json.decodeFromString(CreateChatResponse.serializer(), withAuth(Request.Builder().url("$base/api/chat").post(json.encodeToString(CreateChatRequest.serializer(), r).toRequestBody(media))))
    suspend fun getMessages(id: String, b: String? = null): MessagesResponse {
        val url = "$base/api/chat/$id/messages" + (if (b != null) "?before=$b" else "")
        return json.decodeFromString(MessagesResponse.serializer(), withAuth(Request.Builder().url(url).get()))
    }
    suspend fun getBlockedUsers(): SearchResponse = json.decodeFromString(SearchResponse.serializer(), withAuth(Request.Builder().url("$base/api/social/blocked").get()))
    suspend fun getFriends(): SearchResponse = json.decodeFromString(SearchResponse.serializer(), withAuth(Request.Builder().url("$base/api/social/friends").get()))
    suspend fun sendMessage(id: String, b: String, attachmentPath: String? = null, pollId: String? = null, ttl: Int? = null) { 
        withAuth(Request.Builder().url("$base/api/chat/$id/messages").post(json.encodeToString(SendMessageRequest.serializer(), SendMessageRequest(b, attachmentPath, pollId, ttl)).toRequestBody(media))) 
    }
    suspend fun deleteMessage(cid: String, mid: String) { withAuth(Request.Builder().url("$base/api/chat/$cid/messages/$mid").delete()) }
    suspend fun deleteChannelMessage(channelId: String, messageId: String) { withAuth(Request.Builder().url("$base/api/chat/$channelId/messages/$messageId").delete()) }
    suspend fun setMessageReaction(chatId: String, msgId: String, reaction: String?) { withAuth(Request.Builder().url("$base/api/chat/$chatId/messages/$msgId/reaction").post("{\"reaction\":${if (reaction != null) "\"$reaction\"" else "null"}}".toRequestBody(media))) }
    suspend fun getUserPublicKey(id: String): String = json.decodeFromString(PublicKeyResponse.serializer(), withAuth(Request.Builder().url("$base/api/keys/identity/$id").get())).publicKey
    suspend fun uploadIdentityKey(k: String) { withAuth(Request.Builder().url("$base/api/keys/identity").post("{\"publicKey\":\"$k\"}".toRequestBody(media))) }
    suspend fun getNotifications(): NotificationsResponse = json.decodeFromString(NotificationsResponse.serializer(), withAuth(Request.Builder().url("$base/api/social/notifications").get()))
    suspend fun readNotifications() { withAuth(Request.Builder().url("$base/api/social/notifications/read").post("".toRequestBody())) }
    suspend fun readNotification(id: String) { withAuth(Request.Builder().url("$base/api/social/notifications/$id/read").post("".toRequestBody())) }
    suspend fun blockUser(id: String) { withAuth(Request.Builder().url("$base/api/social/block/$id").post("".toRequestBody(media))) }
    suspend fun unblockUser(id: String) { withAuth(Request.Builder().url("$base/api/social/unblock/$id").post("".toRequestBody(media))) }
    suspend fun reportContent(targetId: String, type: String, reason: String) { withAuth(Request.Builder().url("$base/api/social/report").post("{\"targetId\":\"$targetId\",\"type\":\"$type\",\"reason\":\"$reason\"}".toRequestBody(media))) }
    suspend fun getStories(): StoriesResponse = json.decodeFromString(StoriesResponse.serializer(), withAuth(Request.Builder().url("$base/api/stories").get()))
    suspend fun deleteStory(id: String) { withAuth(Request.Builder().url("$base/api/stories/$id").delete()) }
    suspend fun updateFcmToken(token: String) { withAuth(Request.Builder().url("$base/api/profile/fcm-token").post("{\"token\":\"$token\"}".toRequestBody(media))) }
    suspend fun createStory(i: String) { withAuth(Request.Builder().url("$base/api/stories").post("{\"imageUrl\":\"$i\"}".toRequestBody(media))) }
    suspend fun uploadImage(b: ByteArray, f: String): UploadResponse = withContext(Dispatchers.IO) {
        // Определяем MIME-тип по расширению файла
        val mimeType = when {
            f.endsWith(".gif", ignoreCase = true) -> "image/gif"
            f.endsWith(".png", ignoreCase = true) -> "image/png"
            f.endsWith(".webp", ignoreCase = true) -> "image/webp"
            f.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            f.endsWith(".webm", ignoreCase = true) -> "video/webm"
            f.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            f.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            f.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
            f.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            else -> "image/jpeg"
        }
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", f, b.toRequestBody(mimeType.toMediaType())).build()
        return@withContext json.decodeFromString(UploadResponse.serializer(), withAuth(Request.Builder().url("$base/api/upload").post(body)))
    }
    suspend fun getSettings(): SettingsResponse = json.decodeFromString(SettingsResponse.serializer(), withAuth(Request.Builder().url("$base/api/settings").get()))
    suspend fun saveSettings(settingsJson: String) { withAuth(Request.Builder().url("$base/api/settings").post("{\"settings\":$settingsJson}".toRequestBody(media))) }
    suspend fun uploadCrashLog(logContent: String) { withAuth(Request.Builder().url("$base/api/logs/crash").post("{\"log\":\"${logContent.replace("\"", "\\\"").replace("\n", "\\n")}\"}".toRequestBody(media))) }
    suspend fun getStickers(packName: String? = null): StickersResponse = json.decodeFromString(StickersResponse.serializer(), withAuth(Request.Builder().url(if (packName != null) "$base/api/stickers?pack=$packName" else "$base/api/stickers").get()))
    suspend fun getStickerPacks(): StickerPacksResponse = json.decodeFromString(StickerPacksResponse.serializer(), withAuth(Request.Builder().url("$base/api/stickers/packs").get()))
    suspend fun getStickerBotChat(): StickerBotChatResponse = json.decodeFromString(StickerBotChatResponse.serializer(), withAuth(Request.Builder().url("$base/api/sticker-bot/chat").get()))
    suspend fun addGifToFavorites(gifUrl: String) { withAuth(Request.Builder().url("$base/api/stickers/gifs/favorites").post("{\"gifUrl\":\"$gifUrl\"}".toRequestBody(media))) }
    suspend fun getFavoriteGifs(): FavoriteGifsResponse = json.decodeFromString(FavoriteGifsResponse.serializer(), withAuth(Request.Builder().url("$base/api/stickers/gifs/favorites").get()))
    suspend fun removeGifFromFavorites(gifUrl: String) { withAuth(Request.Builder().url("$base/api/stickers/gifs/favorites?url=$gifUrl").delete()) }
    suspend fun createPoll(postId: String?, chatId: String?, question: String, options: List<String>, multipleChoice: Boolean = false, anonymous: Boolean = false, expiresIn: Int? = null): CreatePollResponse {
        val body = buildString { append("{"); if (postId != null) append("\"postId\":\"$postId\","); if (chatId != null) append("\"chatId\":\"$chatId\","); append("\"question\":\"$question\",\"options\":${options.joinToString("\",\"", "[\"", "\"]")},\"multipleChoice\":$multipleChoice,\"anonymous\":$anonymous"); if (expiresIn != null) append(",\"expiresIn\":$expiresIn"); append("}") }.toRequestBody(media)
        return json.decodeFromString(CreatePollResponse.serializer(), withAuth(Request.Builder().url("$base/api/polls").post(body)))
    }
    suspend fun getPoll(pollId: String): PollResponse = json.decodeFromString(PollResponse.serializer(), withAuth(Request.Builder().url("$base/api/polls/$pollId").get()))
    suspend fun votePoll(pollId: String, optionIds: List<String>) { withAuth(Request.Builder().url("$base/api/polls/$pollId/vote").post("{\"optionIds\":${optionIds.joinToString("\",\"", "[\"", "\"]")}}".toRequestBody(media))) }
    suspend fun deletePoll(pollId: String) { withAuth(Request.Builder().url("$base/api/polls/$pollId").delete()) }
    suspend fun pinMessage(chatId: String, messageId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/messages/$messageId/pin").post("".toRequestBody())) }
    suspend fun unpinMessage(chatId: String, messageId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/messages/$messageId/pin").delete()) }
    suspend fun getPinnedMessages(chatId: String): MessagesResponse = json.decodeFromString(MessagesResponse.serializer(), withAuth(Request.Builder().url("$base/api/chat/$chatId/pinned").get()))
    suspend fun archiveChat(chatId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/archive").post("".toRequestBody())) }
    suspend fun unarchiveChat(chatId: String) { withAuth(Request.Builder().url("$base/api/chat/$chatId/archive").delete()) }
    suspend fun getArchivedChats(): ChatsResponse = json.decodeFromString(ChatsResponse.serializer(), withAuth(Request.Builder().url("$base/api/chat/archived").get()))
    suspend fun searchInChat(chatId: String, query: String): MessagesResponse = json.decodeFromString(MessagesResponse.serializer(), withAuth(Request.Builder().url("$base/api/chat/$chatId/search?q=$query").get()))
    suspend fun toggleSavePost(postId: String) { withAuth(Request.Builder().url("$base/api/posts/$postId/save").post("".toRequestBody())) }
    suspend fun getSavedPosts(beforeId: String? = null): PostsResponse = json.decodeFromString(PostsResponse.serializer(), withAuth(Request.Builder().url("$base/api/posts/saved" + (if (beforeId != null) "?beforeId=$beforeId" else "")).get()))
    suspend fun setup2FA(): Setup2FAResponse = json.decodeFromString(Setup2FAResponse.serializer(), withAuth(Request.Builder().url("$base/api/auth/2fa/setup").post("".toRequestBody())))
    suspend fun verify2FA(token: String) { withAuth(Request.Builder().url("$base/api/auth/2fa/verify").post("{\"token\":\"$token\"}".toRequestBody(media))) }
    suspend fun disable2FA(token: String) { withAuth(Request.Builder().url("$base/api/auth/2fa/disable").post("{\"token\":\"$token\"}".toRequestBody(media))) }
    suspend fun sendEmailVerification() { withAuth(Request.Builder().url("$base/api/auth/email/send-verification").post("".toRequestBody())) }
    suspend fun verifyEmail(code: String) { withAuth(Request.Builder().url("$base/api/auth/email/verify").post("{\"code\":\"$code\"}".toRequestBody(media))) }
    suspend fun getAuthStatus(): AuthStatusResponse = json.decodeFromString(AuthStatusResponse.serializer(), withAuth(Request.Builder().url("$base/api/auth/status").get()))

    // НОВОЕ: Каналы
    suspend fun subscribeToChannel(channelId: String) { withAuth(Request.Builder().url("$base/api/chat/$channelId/subscribe").post("".toRequestBody())) }
    suspend fun unsubscribeFromChannel(channelId: String) { withAuth(Request.Builder().url("$base/api/chat/$channelId/unsubscribe").post("".toRequestBody())) }
    suspend fun searchChannels(query: String): ChannelsSearchResponse = json.decodeFromString(ChannelsSearchResponse.serializer(), withAuth(Request.Builder().url("$base/api/chat/channels/search?q=$query").get()))
    suspend fun createChannel(request: CreateChannelRequest): CreateChannelResponse = json.decodeFromString(CreateChannelResponse.serializer(), withAuth(Request.Builder().url("$base/api/channels").post(json.encodeToString(CreateChannelRequest.serializer(), request).toRequestBody(media))))
    suspend fun updateChannel(channelId: String, request: UpdateChannelRequest) { withAuth(Request.Builder().url("$base/api/channels/$channelId").put(json.encodeToString(UpdateChannelRequest.serializer(), request).toRequestBody(media))) }
    suspend fun markMessageViewed(channelId: String, messageId: String) { withAuth(Request.Builder().url("$base/api/chat/$channelId/messages/$messageId/view").post("".toRequestBody())) }

    // НОВОЕ: Шеринг стилей
    @Serializable
    data class ShareStyleResponse(val shareId: String, val shareUrl: String)
    
    suspend fun shareStyle(): ShareStyleResponse {
        return json.decodeFromString(ShareStyleResponse.serializer(), withAuth(Request.Builder().url("$base/api/style/share").post("".toRequestBody())))
    }
    
    suspend fun getSharedStyle(shareId: String): SettingsResponse {
        // Ответ сервера возвращает JSON настроек, который мы мапим в SettingsResponse (поле settings строка)
        // ВАЖНО: сервер возвращает "сырой" объект настроек, а SettingsResponse ожидает { settings: "...", updatedAt: "..." }
        // Придется сделать хак или использовать JsonObject
        val text = withAuth(Request.Builder().url("$base/api/style/$shareId").get())
        return SettingsResponse(settings = text, updatedAt = "")
    }

    // ВОССТАНОВЛЕНИЕ ПАРОЛЯ
    suspend fun forgotPassword(email: String): ForgotPasswordResponse = withContext(Dispatchers.IO) {
        val body = "{\"email\":\"$email\"}".toRequestBody(media)
        val request = Request.Builder().url("$base/api/auth/forgot-password").post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception(text)
            return@withContext json.decodeFromString(ForgotPasswordResponse.serializer(), text)
        }
    }
    
    suspend fun resetPassword(email: String, code: String, newPassword: String): ResetPasswordResponse = withContext(Dispatchers.IO) {
        val body = "{\"email\":\"$email\",\"code\":\"$code\",\"newPassword\":\"$newPassword\"}".toRequestBody(media)
        val request = Request.Builder().url("$base/api/auth/reset-password").post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception(text)
            return@withContext json.decodeFromString(ResetPasswordResponse.serializer(), text)
        }
    }
}
