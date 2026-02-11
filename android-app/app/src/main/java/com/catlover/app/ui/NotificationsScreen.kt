package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.NotificationItem
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onPostClick: (String) -> Unit, onUserClick: (com.catlover.app.network.UserSummary) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var viewingUserSummary by remember { mutableStateOf<com.catlover.app.network.UserSummary?>(null) }
    var myUserId by remember { mutableStateOf("") }

    var socket by remember { mutableStateOf<Socket?>(null) }
    
    fun refresh() {
        scope.launch {
            loading = true
            try {
                if (myUserId.isEmpty()) {
                    val me = withContext(Dispatchers.IO) { api.getProfileMe() }
                    myUserId = me.userId
                }
                val res = withContext(Dispatchers.IO) { api.getNotifications() }
                notifications = res.notifications
            } catch (e: Exception) {}
            finally { loading = false }
        }
    }

    LaunchedEffect(Unit) {
        val token = TokenStore(ctx).getAccessToken() ?: return@LaunchedEffect
        val opts = IO.Options().apply { auth = mapOf("token" to token) }
        val s = IO.socket(com.catlover.app.BuildConfig.BASE_URL, opts)
        s.on("notification") { scope.launch { refresh() } }
        s.on("friend_request") { scope.launch { refresh() } }
        s.connect()
        socket = s
        refresh()
    }

    DisposableEffect(Unit) { 
        onDispose { 
            socket?.disconnect() 
        } 
    }

    GlassBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.section_notifications), color = Color.White, fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { api.readNotifications() }
                                    refresh()
                                } catch (e: Exception) {}
                            }
                        }) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                // PullToRefresh functionality replaced with basic Box for now to fix compile error
                // if you need actual pull-to-refresh, we need to add material3-pulltorefresh dependency
                if (notifications.isEmpty() && !loading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.label_no_notifications), color = Color.White.copy(alpha = 0.3f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(notifications) { notification ->
                            NotificationItemGlass(
                                item = notification, 
                                onClick = {
                                    scope.launch { try { api.readNotification(notification.id) } catch(e: Exception){} }
                                    if (notification.type == "LIKE" || notification.type == "COMMENT") {
                                        notification.targetId?.let { onPostClick(it) }
                                    } else if (notification.type == "FOLLOW") {
                                        onUserClick(com.catlover.app.network.UserSummary(notification.fromUserId, notification.fromUsername, notification.fromAvatar, null, null, 1))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (viewingUserSummary != null) {
        UserInfoDialog(
            user = viewingUserSummary!!,
            isMe = viewingUserSummary!!.id == myUserId,
            onDismiss = { viewingUserSummary = null },
            onMessage = {
                val u = viewingUserSummary ?: return@UserInfoDialog
                scope.launch {
                    try {
                        val res = withContext(Dispatchers.IO) { 
                            api.createChat(com.catlover.app.network.CreateChatRequest(isGroup = false, memberIds = listOf(u.id))) 
                        }
                        onPostClick("CHAT:${res.chatId}")
                    } catch (e: Exception) {}
                    finally { viewingUserSummary = null }
                }
            }
        )
    }
}

@Composable
fun NotificationItemGlass(item: NotificationItem, onClick: () -> Unit) {
    val icon = when(item.type) {
        "LIKE" -> Icons.Default.Favorite
        "COMMENT" -> Icons.Default.QuestionAnswer
        "FOLLOW" -> Icons.Default.PersonAdd
        "MESSAGE" -> Icons.Default.Chat
        "GROUP_INVITE" -> Icons.Default.GroupAdd
        "STORY_REPLY" -> Icons.Default.AutoAwesome
        else -> Icons.Default.Notifications
    }
    val iconColor = when(item.type) {
        "LIKE" -> Color(0xFFFF4081)
        "COMMENT" -> Color(0xFF00BFA5)
        "FOLLOW" -> Color(0xFF2979FF)
        "MESSAGE" -> Color(0xFFFFD600)
        "GROUP_INVITE" -> Color(0xFF7C4DFF)
        "STORY_REPLY" -> Color(0xFFF48FB1)
        else -> Color.White
    }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (item.fromAvatar.isNullOrBlank()) {
                    Text(item.fromUsername.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    coil.compose.AsyncImage(
                        model = com.catlover.app.network.ApiClient.formatMediaUrl(item.fromAvatar),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                
                // Small Type Icon Overlay
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(iconColor).padding(2.dp)) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fromUsername,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isRead == 1) Color.White.copy(alpha = 0.5f) else Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = item.body ?: "",
                    color = if (item.isRead == 1) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
            Text(
                text = item.createdAt.take(10),
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp
            )
        }
    }
}
