package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catlover.app.ui.AnimatedAvatar
import com.catlover.app.ui.VideoPlayer
import com.catlover.app.ui.GlassBackground
import com.catlover.app.ui.GlassCard
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.CreateChannelRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(onBack: () -> Unit, onChannelCreated: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var commentsEnabled by remember { mutableStateOf(false) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val avatarLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val stream = ctx.contentResolver.openInputStream(it)
                    val bytes = stream?.readBytes()
                    stream?.close()
                    if (bytes != null && bytes.isNotEmpty()) {
                        // Определяем тип файла
                        val mimeType = ctx.contentResolver.getType(it) ?: "image/jpeg"
                        val extension = when {
                            mimeType.contains("gif") -> "gif"
                            mimeType.contains("video") -> "mp4"
                            else -> "jpg"
                        }
                        // Загружаем сразу чтобы получить URL
                        loading = true
                        try {
                            val uploadRes = withContext(Dispatchers.IO) { api.uploadImage(bytes, "channel_avatar.$extension") }
                            avatarUrl = uploadRes.url
                            avatarBytes = bytes
                        } catch (e: Exception) {
                            avatarBytes = null
                            avatarUrl = null
                            throw e
                        } finally {
                            loading = false
                        }
                    }
                } catch (e: Exception) {
                    error = "Ошибка загрузки: ${e.message}"
                    avatarBytes = null
                    loading = false
                }
            }
        }
    }

    fun createChannel() {
        if (title.isBlank()) {
            error = "Название обязательно"
            return
        }
        if (title.length < 3) {
            error = "Название должно быть не менее 3 символов"
            return
        }
        if (username.isBlank()) {
            error = "Имя пользователя обязательно"
            return
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            error = "Имя может содержать только буквы, цифры и подчёркивания"
            return
        }
        if (username.length < 3) {
            error = "Имя должно быть не менее 3 символов"
            return
        }

        scope.launch {
            loading = true
            error = null
            try {
                val response = withContext(Dispatchers.IO) {
                    api.createChannel(
                        CreateChannelRequest(
                            title = title,
                            username = username,
                            description = description.ifBlank { null },
                            isPublic = isPublic,
                            commentsEnabled = commentsEnabled,
                            avatarUrl = avatarUrl
                        )
                    )
                }
                onChannelCreated(response.channelId)
            } catch (e: Exception) {
                error = e.message ?: "Не удалось создать канал"
            } finally {
                loading = false
            }
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Создать канал", color = Color.White) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Аватар канала
                GlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Аватар канала",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                .clickable { avatarLauncher.launch("*/*") }, // Принимаем все типы файлов
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUrl != null) {
                                AnimatedAvatar(
                                    avatarUrl = avatarUrl,
                                    fallbackText = title.ifBlank { "?" },
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = "Add Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        
                        Text(
                            text = "Нажмите чтобы выбрать фото",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Информация о канале",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Название канала") },
                            placeholder = { Text("Мой крутой канал") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
                            label = { Text("Имя пользователя") },
                            placeholder = { Text("moykrutoykanal") },
                            prefix = { Text("@", color = Color.White.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Описание (необязательно)") },
                            placeholder = { Text("Расскажите, о чём ваш канал") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                GlassCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Настройки",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Публичный канал", color = Color.White)
                                Text(
                                    "Любой может найти и подписаться",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isPublic,
                                onCheckedChange = { isPublic = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        Divider(color = Color.White.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Включить комментарии", color = Color.White)
                                Text(
                                    "Подписчики смогут комментировать посты",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = commentsEnabled,
                                onCheckedChange = { commentsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                if (error != null) {
                    GlassCard {
                        Text(
                            text = error!!,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Button(
                    onClick = { createChannel() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !loading && title.isNotBlank() && username.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Создать канал", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Text(
                    text = "Примечание: Только администраторы могут публиковать в каналах. Подписчики могут только читать сообщения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
