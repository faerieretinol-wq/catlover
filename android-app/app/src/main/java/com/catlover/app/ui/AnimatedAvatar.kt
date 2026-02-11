package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.catlover.app.network.ApiClient

/**
 * Универсальный компонент для отображения аватаров с поддержкой:
 * - Статических изображений (JPG, PNG)
 * - Анимированных GIF
 * - Видео (MP4, WEBM, MOV)
 * 
 * @param avatarUrl URL аватара или null
 * @param fallbackText Текст для отображения если аватар отсутствует (обычно первая буква имени)
 * @param modifier Модификатор для кастомизации размера и формы
 * @param contentScale Масштабирование контента (по умолчанию Crop)
 */
@Composable
fun AnimatedAvatar(
    avatarUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val ctx = LocalContext.current
    
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNullOrBlank()) {
            // Показываем первую букву если аватара нет
            Text(
                text = fallbackText.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        } else {
            // Определяем тип файла по расширению
            val isVideo = avatarUrl.endsWith(".mp4", ignoreCase = true) || 
                         avatarUrl.endsWith(".webm", ignoreCase = true) || 
                         avatarUrl.endsWith(".mov", ignoreCase = true)
            
            if (isVideo) {
                // Использование ExoPlayer для видео-аватаров (более производительно)
                VideoPlayer(
                    url = ApiClient.formatMediaUrl(avatarUrl) ?: "",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Для изображений и GIF используем Coil (поддерживает GIF автоматически)
                // Отключаем кеш чтобы всегда загружать свежие аватары
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(ctx)
                        .data(ApiClient.formatMediaUrl(avatarUrl))
                        .crossfade(true)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED) // Отключаем кеш в памяти
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED) // Отключаем кеш на диске
                        .build(),
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
