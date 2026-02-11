package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSettingsScreen(
    channelId: String,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    
    var channelDetails by remember { mutableStateOf<com.catlover.app.network.ChatDetails?>(null) }
    var members by remember { mutableStateOf<List<com.catlover.app.network.ChatMember>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<com.catlover.app.network.ChatMember?>(null) }
    var showMemberActionsDialog by remember { mutableStateOf(false) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var channelPosts by remember { mutableStateOf<List<com.catlover.app.network.PostItem>>(emptyList()) }
    var loadingPosts by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 - Участники, 1 - Посты
    
    // Для редактирования
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    
    fun loadChannelPosts() {
        scope.launch {
            loadingPosts = true
            try {
                val res = withContext(Dispatchers.IO) { api.getPosts(f = false, channelId = channelId) }
                channelPosts = res.posts
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingPosts = false
            }
        }
    }

    fun loadChannelInfo() {
        scope.launch {
            loading = true
            try {
                val details = withContext(Dispatchers.IO) { api.getChatDetails(channelId) }
                channelDetails = details
                
                val membersResponse = withContext(Dispatchers.IO) { api.getChatMembers(channelId) }
                members = membersResponse.members
                
                loadChannelPosts()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }
    
    // Для загрузки аватара
    val avatarLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                loading = true
                try {
                    android.util.Log.d("ChannelSettings", "Starting avatar upload for URI: $it")
                    
                    val stream = ctx.contentResolver.openInputStream(it)
                    val bytes = stream?.readBytes()
                    stream?.close()
                    
                    if (bytes == null || bytes.isEmpty()) {
                        android.widget.Toast.makeText(ctx, "Не удалось прочитать файл", android.widget.Toast.LENGTH_SHORT).show()
                        loading = false
                        return@launch
                    }
                    
                    android.util.Log.d("ChannelSettings", "File size: ${bytes.size} bytes")
                    
                    // Определяем тип файла
                    val mimeType = ctx.contentResolver.getType(it) ?: "image/jpeg"
                    android.util.Log.d("ChannelSettings", "MIME type: $mimeType")
                    
                    val extension = when {
                        mimeType.contains("gif") -> "gif"
                        mimeType.contains("png") -> "png"
                        mimeType.contains("webp") -> "webp"
                        mimeType.contains("video") -> "mp4"
                        else -> "jpg"
                    }
                    
                    android.util.Log.d("ChannelSettings", "Uploading as: channel_avatar.$extension")
                    
                    val uploadRes = withContext(Dispatchers.IO) { 
                        api.uploadImage(bytes, "channel_avatar.$extension") 
                    }
                    
                    android.util.Log.d("ChannelSettings", "Upload successful, URL: ${uploadRes.url}")
                    
                    // Обновляем аватар канала через API
                    withContext(Dispatchers.IO) {
                        api.updateChannel(
                            channelId,
                            com.catlover.app.network.UpdateChannelRequest(avatarUrl = uploadRes.url)
                        )
                    }
                    
                    android.util.Log.d("ChannelSettings", "Channel updated successfully")
                    android.widget.Toast.makeText(ctx, "Аватар обновлён", android.widget.Toast.LENGTH_SHORT).show()
                    loadChannelInfo()
                } catch (e: Exception) {
                    android.util.Log.e("ChannelSettings", "Avatar upload error", e)
                    val errorMsg = when {
                        e.message?.contains("FILE_TOO_LARGE") == true -> "Файл слишком большой (макс 50MB)"
                        e.message?.contains("INVALID_MIME") == true -> "Неподдерживаемый тип файла"
                        e.message?.contains("timeout") == true -> "Превышено время ожидания. Попробуйте файл меньшего размера"
                        else -> "Ошибка: ${e.message}"
                    }
                    android.widget.Toast.makeText(ctx, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    loading = false
                }
            }
        }
    }
    
    LaunchedEffect(Unit) { loadChannelInfo() }
    
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Настройки канала", color = Color.White) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                        }
                    }
                )
            },
            floatingActionButton = {
                if (channelDetails?.myRole == "admin") {
                    FloatingActionButton(
                        onClick = { showCreatePostDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Создать пост")
                    }
                }
            }
        ) { padding ->
            if (loading && channelDetails == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Информация о канале
                    GlassCard {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Аватар
                            AnimatedAvatar(
                                avatarUrl = channelDetails?.recipientAvatar,
                                fallbackText = channelDetails?.title ?: "?",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .clickable { 
                                        if (channelDetails?.myRole == "admin") {
                                            avatarLauncher.launch("*/*")
                                        }
                                    },
                                contentScale = ContentScale.Crop
                            )
                            
                            // Название
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = channelDetails?.title ?: "Канал",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (channelDetails?.isVerified == true) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            // Username канала
                            if (!channelDetails?.username.isNullOrBlank()) {
                                Text(
                                    text = "@${channelDetails?.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Подписчики
                            Text(
                                text = "${members.size} подписчиков",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // Действия
                    if (channelDetails?.myRole == "admin") {
                        GlassCard {
                            Column(modifier = Modifier.padding(8.dp)) {
                                ListItem(
                                    headlineContent = { Text("Редактировать канал", color = Color.White) },
                                    leadingContent = { Icon(Icons.Default.Edit, null, tint = Color.White) },
                                    modifier = Modifier.clickable { 
                                        editTitle = channelDetails?.title ?: ""
                                        editDescription = ""
                                        showEditDialog = true 
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                
                                Divider(color = Color.White.copy(alpha = 0.1f))
                                
                                ListItem(
                                    headlineContent = { Text("Добавить участника", color = Color.White) },
                                    leadingContent = { Icon(Icons.Default.PersonAdd, null, tint = Color.White) },
                                    modifier = Modifier.clickable { showAddMemberDialog = true },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                
                                Divider(color = Color.White.copy(alpha = 0.1f))
                                
                                ListItem(
                                    headlineContent = { Text("Участники", color = Color.White) },
                                    leadingContent = { Icon(Icons.Default.People, null, tint = Color.White) },
                                    trailingContent = { Text("${members.size}", color = Color.White.copy(alpha = 0.7f)) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                
                                Divider(color = Color.White.copy(alpha = 0.1f))
                                
                                ListItem(
                                    headlineContent = { Text("Удалить канал", color = Color.Red) },
                                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                                    modifier = Modifier.clickable { showDeleteDialog = true },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                    
                    // Табы: Участники и Посты
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Участники", color = Color.White) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Посты", color = Color.White) }
                        )
                    }

                    if (selectedTab == 0) {
                        // Список участников
                        GlassCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Участники",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                members.forEach { member ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                if (channelDetails?.myRole == "admin") {
                                                    selectedMember = member
                                                    showMemberActionsDialog = true
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AnimatedAvatar(
                                            avatarUrl = member.avatarUrl,
                                            fallbackText = member.username,
                                            modifier = Modifier.size(40.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = member.username,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = when (member.role) {
                                                    "admin" -> "Администратор"
                                                    "moderator" -> "Модератор"
                                                    else -> "Участник"
                                                },
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                        
                                        if (channelDetails?.myRole == "admin" && member.role != "admin") {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Список постов
                        if (loadingPosts) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (channelPosts.isEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("В канале пока нет постов", color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        } else {
                            channelPosts.forEach { post ->
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = post.createdAt.take(10),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (channelDetails?.myRole == "admin") {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            try {
                                                                withContext(Dispatchers.IO) { api.deletePost(post.id) }
                                                                android.widget.Toast.makeText(ctx, "Пост удален", android.widget.Toast.LENGTH_SHORT).show()
                                                                loadChannelPosts()
                                                            } catch (e: Exception) {
                                                                android.widget.Toast.makeText(ctx, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = post.body, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                        if (!post.imageUrl.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val fullUrl = ApiClient.formatMediaUrl(post.imageUrl) ?: ""
                                            AsyncImage(
                                                model = fullUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить канал?") },
            text = { Text("Это действие нельзя отменить. Все сообщения будут удалены.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { api.deleteChat(channelId) }
                                onBack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог редактирования
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать канал", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                if (editTitle.isNotBlank()) {
                                    withContext(Dispatchers.IO) { 
                                        api.updateChatTitle(channelId, editTitle)
                                    }
                                }
                                if (editDescription.isNotBlank()) {
                                    withContext(Dispatchers.IO) {
                                        api.updateChannel(
                                            channelId,
                                            com.catlover.app.network.UpdateChannelRequest(description = editDescription)
                                        )
                                    }
                                }
                                showEditDialog = false
                                loadChannelInfo()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(ctx, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог добавления участника
    if (showAddMemberDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<List<com.catlover.app.network.UserSummary>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Добавить участника", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            if (it.length >= 2) {
                                scope.launch {
                                    searching = true
                                    try {
                                        val res = withContext(Dispatchers.IO) { api.searchUsers(it, limit = 10) }
                                        searchResults = res.users
                                    } catch (e: Exception) {}
                                    finally { searching = false }
                                }
                            }
                        },
                        label = { Text("Поиск пользователя") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else if (searchResults.isNotEmpty()) {
                        Column(modifier = Modifier.heightIn(max = 200.dp)) {
                            searchResults.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        api.addChatMember(channelId, user.id)
                                                    }
                                                    android.widget.Toast.makeText(ctx, "Участник добавлен", android.widget.Toast.LENGTH_SHORT).show()
                                                    showAddMemberDialog = false
                                                    loadChannelInfo()
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(ctx, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AnimatedAvatar(
                                        avatarUrl = user.avatarUrl,
                                        fallbackText = user.username,
                                        modifier = Modifier.size(32.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(user.username, color = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
    
    // Диалог действий с участником
    if (showMemberActionsDialog && selectedMember != null) {
        AlertDialog(
            onDismissRequest = { showMemberActionsDialog = false },
            title = { Text(selectedMember!!.username, color = Color.White) },
            text = {
                Column {
                    if (selectedMember!!.role != "admin") {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            api.updateMemberRole(channelId, selectedMember!!.userId, "admin")
                                        }
                                        android.widget.Toast.makeText(ctx, "Назначен администратором", android.widget.Toast.LENGTH_SHORT).show()
                                        showMemberActionsDialog = false
                                        loadChannelInfo()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(ctx, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Назначить администратором", color = Color.White)
                        }
                    }
                    
                    if (selectedMember!!.role == "admin") {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            api.updateMemberRole(channelId, selectedMember!!.userId, "member")
                                        }
                                        android.widget.Toast.makeText(ctx, "Снят с должности", android.widget.Toast.LENGTH_SHORT).show()
                                        showMemberActionsDialog = false
                                        loadChannelInfo()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(ctx, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonRemove, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Снять с должности", color = Color.White)
                        }
                    }
                    
                    if (selectedMember!!.role != "admin") {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            api.removeChatMember(channelId, selectedMember!!.userId)
                                        }
                                        android.widget.Toast.makeText(ctx, "Участник удален", android.widget.Toast.LENGTH_SHORT).show()
                                        showMemberActionsDialog = false
                                        loadChannelInfo()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(ctx, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Удалить из канала", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMemberActionsDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
    
    // Диалог создания поста в канале
    if (showCreatePostDialog) {
        var postBody by remember { mutableStateOf("") }
        var mediaBytes by remember { mutableStateOf<ByteArray?>(null) }
        var mediaType by remember { mutableStateOf("image/jpeg") }
        var uploading by remember { mutableStateOf(false) }
        
        val mediaLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val type = ctx.contentResolver.getType(it) ?: "image/jpeg"
                mediaType = type
                val stream = ctx.contentResolver.openInputStream(it)
                mediaBytes = stream?.readBytes()
                stream?.close()
            }
        }
        
        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = { Text("Новый пост в канале", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = postBody,
                        onValueChange = { postBody = it },
                        label = { Text("Текст поста") },
                        placeholder = { Text("Что нового?") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    
                    Button(
                        onClick = { mediaLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (mediaBytes != null) {
                                if (mediaType.contains("video")) "Видео выбрано" else "Фото выбрано"
                            } else {
                                "Добавить фото/видео"
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = postBody.isNotBlank() && !uploading,
                    onClick = {
                        scope.launch {
                            uploading = true
                            try {
                                                var imageUrl: String? = null
                                if (mediaBytes != null && mediaBytes!!.isNotEmpty()) {
                                    val ext = if (mediaType.contains("video")) "mp4" else "jpg"
                                    val uploadRes = withContext(Dispatchers.IO) {
                                        api.uploadImage(mediaBytes!!, "channel_post.$ext")
                                    }
                                    imageUrl = uploadRes.url
                                }
                                
                                withContext(Dispatchers.IO) {
                                    api.createPost(postBody, imageUrl, channelId)
                                }
                                
                                android.widget.Toast.makeText(ctx, "Пост опубликован", android.widget.Toast.LENGTH_SHORT).show()
                                showCreatePostDialog = false
                            } catch (e: Exception) {
                                android.util.Log.e("ChannelSettings", "Post creation error", e)
                                android.widget.Toast.makeText(ctx, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                uploading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Опубликовать")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
