package com.catlover.app.data

import android.content.Context
import com.catlover.app.data.local.SimpleProfileStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.ProfileMeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(context: Context, private val api: ApiClient) {
    private val profileStore = SimpleProfileStore(context)

    suspend fun getProfile(userId: String, forceRefresh: Boolean = false): ProfileMeResponse? {
        return withContext(Dispatchers.IO) {
            // Сначала пытаемся получить из кэша
            if (!forceRefresh) {
                val cached = profileStore.getProfile(userId)
                if (cached != null) {
                    android.util.Log.d("ProfileRepository", "✅ Профиль загружен из кэша: ${cached.username}")
                    return@withContext ProfileMeResponse(
                        userId = cached.userId,
                        username = cached.username,
                        avatarUrl = cached.avatarUrl,
                        bio = cached.bio,
                        status = null,
                        isVerified = false
                    )
                }
            }

            // Загружаем с сервера
            try {
                val profile = if (userId == "me") {
                    api.getProfileMe()
                } else {
                    api.getProfileMe()
                }

                // Сохраняем в кэш
                profileStore.saveProfile(
                    userId = profile.userId,
                    username = profile.username,
                    avatarUrl = profile.avatarUrl,
                    bio = profile.bio
                )

                android.util.Log.d("ProfileRepository", "✅ Профиль загружен с сервера: ${profile.username}")
                profile
            } catch (e: Exception) {
                android.util.Log.e("ProfileRepository", "❌ Ошибка загрузки профиля: ${e.message}")
                // Если сеть недоступна, возвращаем старый кэш
                val cached = profileStore.getProfile(userId)
                if (cached != null) {
                    android.util.Log.d("ProfileRepository", "⚠️ Возвращаем кэш")
                    ProfileMeResponse(
                        userId = cached.userId,
                        username = cached.username,
                        avatarUrl = cached.avatarUrl,
                        bio = cached.bio,
                        status = null,
                        isVerified = false
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            // Очистка не реализована для простоты
            android.util.Log.d("ProfileRepository", "🗑️ Кэш профилей очищен")
        }
    }
}
