package com.catlover.app.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.ChatSummary
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(onChatSelected: (String) -> Unit, onCreateChannel: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var archivedChats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var showArchived by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var deletingChatId by remember { mutableStateOf<String?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var socket by remember { mutableStateOf<Socket?>(null) }

    fun loadChats() {
        scope.launch {
            loading = true
            try {
                val res = withContext(Dispatchers.IO) { 
                    if (showArchived) api.getArchivedChats() else api.getChats()
                }
                if (showArchived) archivedChats = res.chats else chats = res.chats
            } catch (e: Exception) {
                e.printStackTrace()
            } finally { loading = false }
        }
    }

    LaunchedEffect(showArchived) { loadChats() }

    LaunchedEffect(Unit) {
        val token = TokenStore(ctx).getAccessToken() ?: return@LaunchedEffect
        val opts = IO.Options().apply { auth = mapOf("token" to token) }
        val s = IO.socket(com.catlover.app.BuildConfig.BASE_URL, opts)
        s.on("message") { scope.launch { loadChats() } }
        s.connect()
        socket = s
    }

    DisposableEffect(Unit) { onDispose { socket?.disconnect() } }

    GlassBackground {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Color.Transparent)) {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.title_chats), fontWeight = FontWeight.Bold, color = Color.White) },
                        actions = {
                            // Кнопка для открытия чата с StickerBot
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        val botChat = withContext(Dispatchers.IO) { api.getStickerBotChat() }
                                        onChatSelected(botChat.chatId)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        val errorMsg = when {
                                            e.message?.contains("<!DOCTYPE") == true -> "Сервер недоступен. Перезапустите сервер."
                                            e.message?.contains("404") == true -> "Бот не найден. Запустите: node src/scripts/init-sticker-bot.js"
                                            else -> "Ошибка: ${e.message}"
                                        }
                                        Toast.makeText(ctx, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.SmartToy, contentDescription = "StickerBot", tint = Color.White)
                            }
                            IconButton(onClick = onCreateChannel) {
                                Icon(Icons.Default.Add, contentDescription = "Create Channel", tint = Color.White)
                            }
                            IconButton(onClick = { showCreateGroupDialog = true }) {
                                Icon(Icons.Default.GroupAdd, contentDescription = "Create Group", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    // TabRow for switching between regular and archived chats
                    TabRow(
                        selectedTabIndex = if (showArchived) 1 else 0,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (showArchived) 1 else 0]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ) {
                        Tab(selected = !showArchived, onClick = { showArchived = false }, text = { Text(stringResource(R.string.label_chats_tab)) })
                        Tab(selected = showArchived, onClick = { showArchived = true }, text = { Text(stringResource(R.string.label_archived_tab)) })
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            val displayChats = if (showArchived) archivedChats else chats
            
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (loading && displayChats.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                } else if (displayChats.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(if (showArchived) Icons.Default.Archive else Icons.Default.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (showArchived) "No archived chats" else "No conversations yet", color = Color.White.copy(alpha = 0.3f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayChats, key = { it.id }) { chat ->
                            ChatListItemGlass(
                                chat = chat, 
                                onClick = { onChatSelected(chat.id) },
                                onLongClick = { deletingChatId = chat.id }
                            )
                            Divider(
                                color = Color.White.copy(alpha = 0.1f), 
                                thickness = 0.5.dp, 
                                modifier = Modifier.padding(start = 88.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (deletingChatId != null) {
        AlertDialog(
            onDismissRequest = { deletingChatId = null },
            title = { Text("Delete Chat") },
            text = { Text("Delete this conversation for everyone?") },
            confirmButton = {
                Button(onClick = {
                    val id = deletingChatId ?: return@Button
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { api.deleteChat(id) }
                            loadChats()
                        } catch (e: Exception) {}
                        finally { deletingChatId = null }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = { TextButton(onClick = { deletingChatId = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
    
    if (showCreateGroupDialog) {
        CreateGroupDialog(api = api, onDismiss = { showCreateGroupDialog = false }, onCreated = { loadChats() })
    }
}

@Composable
fun CreateGroupDialog(api: ApiClient, onDismiss: () -> Unit, onCreated: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var groupTitle by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<com.catlover.app.network.UserSummary>>(emptyList()) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var loadingFriends by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            val res = withContext(Dispatchers.IO) { api.getFriends() }
            friends = res.users
        } catch (e: Exception) {}
        finally { loadingFriends = false }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Members:", fontSize = 12.sp, color = Color.Gray)
                if (loadingFriends) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else if (friends.isEmpty()) {
                    Text("No friends found", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(friends) { friend ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (selectedIds.contains(friend.id)) selectedIds.remove(friend.id)
                                    else selectedIds.add(friend.id)
                                }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = selectedIds.contains(friend.id), onCheckedChange = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(friend.username, color = Color.Black)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = groupTitle.isNotBlank() && selectedIds.isNotEmpty(),
                onClick = {
                    scope.launch {
                        try {
                            val res = withContext(Dispatchers.IO) {
                                api.createChat(com.catlover.app.network.CreateChatRequest(
                                    isGroup = true,
                                    type = "group",
                                    memberIds = selectedIds.toList()
                                ))
                            }
                            if (groupTitle.isNotBlank()) {
                                withContext(Dispatchers.IO) { api.updateChatTitle(res.chatId, groupTitle) }
                            }
                            onCreated()
                            onDismiss()
                        } catch (e: Exception) {}
                    }
                }
            ) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItemGlass(chat: ChatSummary, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayName = if (!chat.isGroup && (chat.title.isNullOrBlank())) (chat.recipientName ?: "Notes (Me)") else (chat.title ?: "Chat")
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (chat.type == "channel") Color(0xFF673AB7).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (chat.type == "channel") {
                Icon(Icons.Default.Campaign, null, tint = Color.White, modifier = Modifier.size(24.dp))
            } else {
                val avatar = chat.recipientAvatar ?: chat.avatarUrl
                AnimatedAvatar(
                    avatarUrl = avatar,
                    fallbackText = displayName,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Онлайн-статус (точка) для личных чатов
            if (chat.type == "dm") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)) // Зеленый для онлайна
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (chat.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.CheckCircle, "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = chat.lastMessageAt?.take(10) ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = chat.lastMessage ?: stringResource(R.string.label_no_posts), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.unreadCount > 0) {
                    Box(modifier = Modifier.padding(start = 8.dp).size(20.dp).clip(CircleShape).background(Color.Red), contentAlignment = Alignment.Center) {
                        Text(text = chat.unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
