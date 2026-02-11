package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.app.BuildConfig
import com.catlover.app.data.E2EKeyManager
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.MessageItem
import com.catlover.app.network.PostItem
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ListItem
import com.catlover.app.ui.StickerPicker
import com.catlover.app.ui.PollView
import com.catlover.app.ui.VoiceMessageBubble
import com.catlover.app.ui.VideoPlayer
import com.catlover.app.ui.GlassBackground
import com.catlover.app.ui.GlassCard
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chatId: String, 
    onBack: () -> Unit, 
    onCall: (String, Boolean) -> Unit = { _, _ -> }, 
    onImageClick: (String) -> Unit = {}, 
    onGroupCall: (String) -> Unit = {}, 
    onChannelSettings: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenStore = remember { TokenStore(ctx) }
    val e2e = remember { E2EKeyManager(ctx) }
    val api = remember { ApiClient(tokenStore) }
    
    var currentUserId by remember { mutableStateOf("") }
    var recipientId by remember { mutableStateOf<String?>(null) }
    var recipientPublicKey by remember { mutableStateOf<String?>(null) }
    var chatDetails by remember { mutableStateOf<com.catlover.app.network.ChatDetails?>(null) }
    var messages by remember { mutableStateOf<List<MessageItem>>(emptyList()) }
    var channelPosts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var body by remember { mutableStateOf("") }
    var socket by remember { mutableStateOf<Socket?>(null) }
    var isTyping by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    var isRecording by remember { mutableStateOf(false) }
    var voiceRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var voiceFile by remember { mutableStateOf<java.io.File?>(null) }
    
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }
    
    var replyToMessageId by remember { mutableStateOf<String?>(null) }
    var replyToText by remember { mutableStateOf("") }
    
    var pinnedMessages by remember { mutableStateOf<List<MessageItem>>(emptyList()) }
    var showPinnedBanner by remember { mutableStateOf(false) }
    
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showStickerPicker by remember { mutableStateOf(false) }
    var showCreatePollDialog by remember { mutableStateOf(false) }
    var showMessageOptionsMenu by remember { mutableStateOf(false) }
    var messageOptionsTarget by remember { mutableStateOf<MessageItem?>(null) }

    val isChannel = chatDetails?.type == "channel"

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val stream = ctx.contentResolver.openInputStream(it)
                    val bytes = stream?.readBytes()
                    stream?.close()
                    if (bytes != null) {
                        val type = ctx.contentResolver.getType(it) ?: "image/jpeg"
                        val ext = if (type.contains("video")) "mp4" else "jpg"
                        val uploadRes = withContext(Dispatchers.IO) { api.uploadImage(bytes, "chat_attach.jpg") }
                        if (isChannel) {
                            withContext(Dispatchers.IO) { api.createPost(if (type.contains("video")) "🎥 идео" else "🖼️ ото", uploadRes.url, chatId) }
                            val res = withContext(Dispatchers.IO) { api.getPosts(f = false, channelId = chatId) }
                            channelPosts = res.posts
                        } else {
                            withContext(Dispatchers.IO) { api.sendMessage(chatId, if (type.contains("video")) "🎥 идео" else "🖼️ ото", uploadRes.url) }
                        }
                    }
                } catch (ex: Exception) {}
            }
        }
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("оиск в чате") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ведите текст...") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(messages.filter { it.body.contains(searchQuery, ignoreCase = true) }) { msg ->
                            Text(
                                text = msg.body,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSearchDialog = false }
                                    .padding(8.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSearchDialog = false }) { Text("акрыть") } }
        )
    }

    if (editingMessageId != null) {
        AlertDialog(
            onDismissRequest = { editingMessageId = null },
            title = { Text("едактировать") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { api.editMessage(chatId, editingMessageId!!, editingText) }
                            editingMessageId = null
                        } catch (ex: Exception) {}
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { editingMessageId = null }) { Text("тмена") } }
        )
    }

    if (showMessageOptionsMenu && messageOptionsTarget != null) {
        val msg = messageOptionsTarget!!
        val isMe = msg.senderId == currentUserId
        val isAdmin = chatDetails?.myRole == "admin" || chatDetails?.myRole == "owner"
        val isDM = chatDetails?.type == "dm"
        AlertDialog(
            onDismissRequest = { showMessageOptionsMenu = false },
            title = { Text("пции сообщения") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("тветить") },
                        leadingContent = { Icon(Icons.Default.Reply, null) },
                        modifier = Modifier.clickable { 
                            replyToMessageId = msg.id
                            replyToText = msg.body
                            showMessageOptionsMenu = false 
                        }
                    )
                    if (isMe || (chatDetails?.type == "channel" || chatDetails?.type == "group") && isAdmin) {
                        ListItem(
                            headlineContent = { Text("едактировать") },
                            leadingContent = { Icon(Icons.Default.Edit, null) },
                            modifier = Modifier.clickable { 
                                editingMessageId = msg.id
                                editingText = msg.body
                                showMessageOptionsMenu = false 
                            }
                        )
                    }
                    if (isMe || isDM || isAdmin) {
                        ListItem(
                            headlineContent = { Text("далить") },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                            modifier = Modifier.clickable { 
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { api.deleteMessage(chatId, msg.id) }
                                        messages = messages.filter { it.id != msg.id }
                                    } catch (ex: Exception) {
                                        android.widget.Toast.makeText(ctx, "Ошибка", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                showMessageOptionsMenu = false 
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("акрепить") },
                        leadingContent = { Icon(Icons.Default.PushPin, null) },
                        modifier = Modifier.clickable { 
                            scope.launch {
                                try { withContext(Dispatchers.IO) { api.pinMessage(chatId, msg.id) } } catch (ex: Exception) {}
                            }
                            showMessageOptionsMenu = false 
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    LaunchedEffect(chatId) {
        try {
            val me = withContext(Dispatchers.IO) { api.getProfileMe() }
            currentUserId = me.userId
            val details = withContext(Dispatchers.IO) { api.getChatDetails(chatId) }
            chatDetails = details
            
            if (details.type == "channel") {
                val res = withContext(Dispatchers.IO) { api.getPosts(f = false, channelId = chatId) }
                channelPosts = res.posts
            } else {
                if (!details.isGroup) {
                    recipientId = details.memberIds.find { it != currentUserId }
                    recipientId?.let { other ->
                        recipientPublicKey = withContext(Dispatchers.IO) { api.getUserPublicKey(other) }
                    }
                }
                val res = withContext(Dispatchers.IO) { api.getMessages(chatId) }
                messages = res.messages.map { msg ->
                    if (recipientPublicKey != null && msg.body.startsWith("e2e:")) {
                        val decrypted = e2e.decrypt(msg.body.removePrefix("e2e:"), recipientPublicKey!!)
                        msg.copy(body = decrypted)
                    } else msg
                }
            }
            
            if (messages.isNotEmpty() || channelPosts.isNotEmpty()) {
                val lastIdx = if (details.type == "channel") channelPosts.lastIndex else messages.lastIndex
                listState.scrollToItem(maxOf(0, lastIdx))
            }
            
            try {
                val pinned = withContext(Dispatchers.IO) { api.getPinnedMessages(chatId) }
                pinnedMessages = pinned.messages
                showPinnedBanner = pinnedMessages.isNotEmpty()
            } catch (ex: Exception) {}
        } catch (ex: Exception) {}
    }

    LaunchedEffect(body) {
        socket?.emit("typing", mapOf("chatId" to chatId, "isTyping" to body.isNotEmpty()))
    }

    LaunchedEffect(chatId) {
        val opts = IO.Options()
        opts.auth = mapOf("token" to tokenStore.getAccessToken())
        val s = IO.socket(BuildConfig.BASE_URL, opts)
        s.on(Socket.EVENT_CONNECT) { s.emit("join", chatId) }
        s.on("message") { args ->
            val data = args[0] as JSONObject
            val msg = MessageItem(
                id = data.getString("id"),
                chatId = data.getString("chatId"),
                senderId = data.getString("senderId"),
                body = data.getString("body"),
                createdAt = data.getString("createdAt")
            )
            if (msg.chatId == chatId) {
                messages = messages + msg
                scope.launch { 
                    delay(50)
                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) 
                }
            }
        }
        s.on("messageDeleted") { args ->
            val data = args[0] as JSONObject
            val deletedId = data.getString("messageId")
            messages = messages.filter { it.id != deletedId }
        }
        s.on("postDeleted") { args ->
            val data = args[0] as JSONObject
            val deletedId = data.getString("postId")
            channelPosts = channelPosts.filter { it.id != deletedId }
        }
        s.on("postCreated") { args ->
            val data = args[0] as JSONObject
            val post = PostItem(
                id = data.getString("id"),
                userId = data.getString("userId"),
                username = data.optString("username", ""),
                avatarUrl = data.optString("avatarUrl", null),
                body = data.getString("body"),
                imageUrl = data.optString("imageUrl", null),
                createdAt = data.getString("createdAt"),
                likes = data.optInt("likes", 0),
                comments = data.optInt("comments", 0),
                likedByMe = data.optBoolean("likedByMe", false)
            )
            if (data.getString("channelId") == chatId) {
                channelPosts = channelPosts + post
                scope.launch { 
                    delay(50)
                    if (channelPosts.isNotEmpty()) listState.animateScrollToItem(channelPosts.lastIndex) 
                }
            }
        }
        s.on("online") { args -> isOnline = true }
        s.on("offline") { args -> isOnline = false }
        s.connect()
        socket = s
    }

    DisposableEffect(Unit) { onDispose { socket?.disconnect() } }

    GlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column(modifier = Modifier.clickable { 
                            if (chatDetails?.type == "channel" || chatDetails?.type == "group") onChannelSettings(chatId)
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = chatDetails?.title ?: chatDetails?.recipientName ?: "ат", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                if (recipientPublicKey != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = Color(0xFF00C853))
                                }
                            }
                            if (isOnline) Text(" сети", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00C853))
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        if (chatDetails?.type == "dm") {
                            IconButton(onClick = { recipientId?.let { onCall(it, false) } }) { Icon(Icons.Default.Call, null, tint = Color.White) }
                            IconButton(onClick = { recipientId?.let { onCall(it, true) } }) { Icon(Icons.Default.Videocam, null, tint = Color.White) }
                        }
                        IconButton(onClick = { showSearchDialog = true }) { Icon(Icons.Default.Search, null, tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (if (isChannel) channelPosts.isEmpty() else messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { Text(if (isChannel) " канале нет постов" else "ат пуст", color = Color.White.copy(0.3f)) }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                            if (isChannel) {
                                items(channelPosts, key = { it.id }) { post ->
                                    val isAdmin = chatDetails?.myRole == "admin" || chatDetails?.myRole == "owner"
                                    PostCardGlass(
                                        post = post,
                                        isMyPost = post.userId == currentUserId || isAdmin,
                                        onLike = { scope.launch { try { api.toggleLike(post.id); val r = withContext(Dispatchers.IO) { api.getPosts(f = false, channelId = chatId) }; channelPosts = r.posts } catch(ex:Exception){} } },
                                        onCommentClick = { onCommentClick(post.id) },
                                        onReport = { },
                                        onFollow = { },
                                        onDelete = { 
                                            scope.launch { 
                                                try { 
                                                    withContext(Dispatchers.IO) { api.deletePost(post.id) }
                                                    channelPosts = channelPosts.filter { it.id != post.id }
                                                } catch(ex:Exception){
                                                    android.widget.Toast.makeText(ctx, "Ошибка", android.widget.Toast.LENGTH_SHORT).show()
                                                } 
                                            } 
                                        },
                                        onEdit = { },
                                        onSave = { scope.launch { try { api.toggleSavePost(post.id) } catch(ex:Exception){} } },
                                        onAuthorClick = { },
                                        onLikesClick = { },
                                        onImageClick = { onImageClick(it) }
                                    )
                                }
                            } else {
                                items(messages, key = { it.id }) { msg ->
                                    MessageBubble(msg = msg, isMe = msg.senderId == currentUserId, chatType = chatDetails?.type, chatTitle = chatDetails?.title, onLongClick = { messageOptionsTarget = msg; showMessageOptionsMenu = true })
                                }
                            }
                        }
                    }
                }
                
                Surface(color = Color.White.copy(alpha = 0.05f)) {
                    Column {
                        if (replyToMessageId != null) {
                            Row(Modifier.fillMaxWidth().background(Color.White.copy(0.1f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Reply, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp))
                                Text(replyToText, maxLines = 1, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
                                IconButton(onClick = { replyToMessageId = null }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showStickerPicker = !showStickerPicker }) { Icon(Icons.Default.SentimentSatisfiedAlt, null, tint = Color.White.copy(0.7f)) }
                            IconButton(onClick = { photoLauncher.launch("*/*") }) { Icon(Icons.Default.Add, null, tint = Color.White.copy(0.7f)) }
                            OutlinedTextField(value = body, onValueChange = { body = it }, modifier = Modifier.weight(1f), placeholder = { Text(if (isChannel) "Новый пост..." else "Сообщение...", color = Color.White.copy(0.5f)) }, shape = CircleShape)
                            Spacer(modifier = Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    if (body.isNotBlank()) {
                                        scope.launch {
                                            try {
                                                if (isChannel) {
                                                    withContext(Dispatchers.IO) { api.createPost(body, null, chatId) }
                                                    val r = withContext(Dispatchers.IO) { api.getPosts(f = false, channelId = chatId) }
                                                    channelPosts = r.posts
                                                } else {
                                                    val finalBody = if (recipientPublicKey != null) "e2e:" + e2e.encrypt(body, recipientPublicKey!!) else body
                                                    withContext(Dispatchers.IO) { api.sendMessage(chatId, finalBody) }
                                                }
                                                body = ""
                                            } catch (ex: Exception) {
                                                android.widget.Toast.makeText(ctx, "Ошибка", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else if (!isRecording) {
                                        // This is handled via audioPermissionLauncher defined above
                                    } else {
                                        if (voiceRecorder != null && voiceFile != null) {
                                            try {
                                                voiceRecorder!!.stop()
                                                voiceRecorder!!.release()
                                                scope.launch {
                                                    try {
                                                        val uploaded = withContext(Dispatchers.IO) { api.uploadImage(voiceFile!!.readBytes(), voiceFile!!.name).url }
                                                        withContext(Dispatchers.IO) { api.sendMessage(chatId, "🎤 Голосовое сообщение", uploaded) }
                                                        voiceFile!!.delete()
                                                    } catch (ex: Exception) { ex.printStackTrace() }
                                                    finally { isRecording = false; voiceRecorder = null; voiceFile = null }
                                                }
                                            } catch (ex: Exception) { ex.printStackTrace(); isRecording = false; voiceRecorder = null; voiceFile = null }
                                        }
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, modifier = Modifier.size(48.dp), shape = CircleShape
                            ) { Icon(if (body.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic, null, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(msg: MessageItem, isMe: Boolean, chatType: String? = null, chatTitle: String? = null, onLongClick: () -> Unit = {}) {
    val isChannel = chatType == "channel"
    Box(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick), contentAlignment = if (isChannel) Alignment.Center else if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
        GlassCard(shape = if (isChannel) RoundedCornerShape(12.dp) else if (isMe) RoundedCornerShape(18.dp, 18.dp, 18.dp, 2.dp) else RoundedCornerShape(18.dp, 18.dp, 2.dp, 18.dp), modifier = Modifier.fillMaxWidth(if (isChannel) 0.95f else 0.85f)) {
            Column(Modifier.padding(14.dp, 10.dp)) {
                if (isChannel && !chatTitle.isNullOrBlank()) Text(chatTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (msg.forwardedFrom != null) Text("Переслано", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (msg.body == "🎤 Голосовое сообщение" && msg.attachmentPath != null) VoiceMessageBubble(msg.attachmentPath!!, isMyMessage = isMe)
                else if (msg.body == "🎥 Видеосообщение" && msg.attachmentPath != null) VideoCirclePlayer(ApiClient.formatMediaUrl(msg.attachmentPath) ?: "")
                else if (msg.body.startsWith("STKR:") && msg.attachmentPath != null) {
                    val url = ApiClient.formatMediaUrl(msg.attachmentPath) ?: ""
                    coil.compose.AsyncImage(url, null, modifier = Modifier.size(120.dp))
                } else if (!msg.attachmentPath.isNullOrBlank()) {
                    val url = ApiClient.formatMediaUrl(msg.attachmentPath) ?: ""
                    if (msg.attachmentPath!!.endsWith(".mp4")) VideoPlayer(url, Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)))
                    else coil.compose.AsyncImage(url, null, Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)))
                    if (msg.body.isNotBlank() && msg.body != "🖼️ Фото" && msg.body != "🎥 Видео") Text(msg.body, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                } else Text(msg.body, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (msg.editedAt != null) Text("изм. ", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.3f))
                    Text(msg.createdAt.take(16).takeLast(5), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                }
            }
        }
    }
}

@Composable
fun StickerPicker(onStickerSelect: (com.catlover.app.network.StickerItem) -> Unit, onGifSelect: (String) -> Unit, api: ApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var stickers by remember { mutableStateOf<List<com.catlover.app.network.StickerItem>>(emptyList()) }
    LaunchedEffect(Unit) { try { stickers = withContext(Dispatchers.IO) { api.getStickers().stickers } } catch (ex: Exception) {} }
    Column(Modifier.fillMaxWidth().height(300.dp).background(Color.Black.copy(alpha = 0.9f))) {
        TabRow(0, containerColor = Color.Transparent) {
            Tab(true, {}, text = { Text("Стикеры", color = Color.White) })
            Tab(false, {}, text = { Text("GIF", color = Color.White) })
        }
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize().padding(8.dp)) {
            items(stickers) { s -> coil.compose.AsyncImage(ApiClient.formatMediaUrl(s.filePath) ?: "", null, Modifier.size(70.dp).clickable { onStickerSelect(s) }.padding(4.dp)) }
        }
    }
}
