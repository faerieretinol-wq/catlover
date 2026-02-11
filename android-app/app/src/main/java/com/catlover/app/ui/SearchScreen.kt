package com.catlover.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.UserSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onNavigateToChat: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserSummary>>(emptyList()) }
    var channels by remember { mutableStateOf<List<com.catlover.app.network.ChannelInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0) }
    var viewingUser by remember { mutableStateOf<UserSummary?>(null) }
    var myUserId by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Users, 1 = Channels

    LaunchedEffect(Unit) {
        try {
            val me = withContext(Dispatchers.IO) { api.getProfileMe() }
            myUserId = me.userId
        } catch (e: Exception) {}
    }

    fun doSearch(q: String, loadMore: Boolean = false) {
        if (q.length < 2) {
            results = emptyList()
            channels = emptyList()
            hasMore = false
            offset = 0
            return
        }
        scope.launch {
            if (loadMore) loadingMore = true else loading = true
            try {
                if (selectedTab == 0) {
                    // Search users
                    val currentOffset = if (loadMore) offset else 0
                    val res = withContext(Dispatchers.IO) { api.searchUsers(q, limit = 20, offset = currentOffset) }
                    if (loadMore) {
                        results = results + res.users
                    } else {
                        results = res.users
                    }
                    hasMore = res.hasMore
                    offset = currentOffset + res.users.size
                } else {
                    // Search channels
                    val res = withContext(Dispatchers.IO) { api.searchChannels(q) }
                    channels = res.channels
                    hasMore = false
                }
            } catch (e: Exception) {}
            finally { 
                loading = false
                loadingMore = false
            }
        }
    }

    GlassBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.label_username), color = Color.White) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { 
                        query = it
                        doSearch(it) 
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text(stringResource(if (selectedTab == 0) R.string.label_search_users else R.string.label_search_channels), color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                // TabRow for Users/Channels
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; doSearch(query) },
                        text = { Text(stringResource(R.string.tab_users)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; doSearch(query) },
                        text = { Text(stringResource(R.string.tab_channels)) }
                    )
                }

                if (loading && results.isEmpty() && channels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (selectedTab == 0 && results.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(if (query.length < 2) R.string.label_search_hint else R.string.label_no_search_results),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                } else if (selectedTab == 1 && channels.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(if (query.length < 2) R.string.label_search_hint else R.string.label_no_channels_found),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                } else if (selectedTab == 0) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results) { user ->
                            UserListItem(user, 
                                onClick = { viewingUser = user },
                                onFollowToggle = {
                                    scope.launch {
                                        try {
                                            if (user.isFollowing == 1) {
                                                withContext(Dispatchers.IO) { api.unfollowUser(user.id) }
                                            } else {
                                                withContext(Dispatchers.IO) { api.followUser(user.id) }
                                            }
                                            doSearch(query)
                                        } catch (e: Exception) {}
                                    }
                                },
                                onMessageClick = {
                                    scope.launch {
                                        try {
                                            val res = withContext(Dispatchers.IO) { 
                                                api.createChat(com.catlover.app.network.CreateChatRequest(isGroup = false, memberIds = listOf(user.id))) 
                                            }
                                            onNavigateToChat(res.chatId)
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        
                        // НОВОЕ: Загрузка следующей страницы
                        if (hasMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    if (loadingMore) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    } else {
                                        TextButton(onClick = { doSearch(query, loadMore = true) }) {
                                            Text("Load more", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // Список каналов
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(channels) { channel ->
                            ChannelListItem(
                                channel = channel,
                                api = api,
                                onNavigateToChat = onNavigateToChat,
                                onSubscribeToggle = {
                                    scope.launch {
                                        try {
                                            if (channel.isSubscribed) {
                                                withContext(Dispatchers.IO) { api.unsubscribeFromChannel(channel.id) }
                                                Toast.makeText(ctx, "Unsubscribed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                withContext(Dispatchers.IO) { api.subscribeToChannel(channel.id) }
                                                Toast.makeText(ctx, "Subscribed!", Toast.LENGTH_SHORT).show()
                                            }
                                            doSearch(query)
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (viewingUser != null) {
        UserInfoDialog(
            user = viewingUser!!,
            isMe = viewingUser!!.id == myUserId,
            onDismiss = { viewingUser = null },
            onMessage = {
                val u = viewingUser ?: return@UserInfoDialog
                scope.launch {
                    try {
                        val res = withContext(Dispatchers.IO) { 
                            api.createChat(com.catlover.app.network.CreateChatRequest(isGroup = false, memberIds = listOf(u.id))) 
                        }
                        onNavigateToChat(res.chatId)
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally { 
                        viewingUser = null 
                    }
                }
            }
        )
    }
}

@Composable
fun UserListItem(user: UserSummary, onClick: () -> Unit, onFollowToggle: () -> Unit, onMessageClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedAvatar(
                avatarUrl = user.avatarUrl,
                fallbackText = user.username,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, color = Color.White)
                if (!user.status.isNullOrBlank()) {
                    Text(user.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontStyle = FontStyle.Italic)
                }
                if (!user.bio.isNullOrBlank()) {
                    Text(user.bio, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                }
                Row {
                    Text(
                        text = stringResource(if (user.isFollowing == 1) R.string.action_following else R.string.action_follow),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onFollowToggle() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.action_message),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onMessageClick() }
                    )
                }
            }
        }
    }
}


@Composable
fun ChannelListItem(
    channel: com.catlover.app.network.ChannelInfo,
    api: ApiClient,
    onNavigateToChat: (String) -> Unit,
    onSubscribeToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToChat(channel.id) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel icon
            AnimatedAvatar(
                avatarUrl = channel.avatarUrl,
                fallbackText = channel.title,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (channel.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (channel.username != null) {
                    Text(
                        text = "@${channel.username}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
                
                if (channel.description != null && channel.description.isNotBlank()) {
                    Text(
                        text = channel.description,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
                
                Text(
                    text = "${channel.subscribers} ${stringResource(R.string.label_subscribers)}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Subscribe button
            Button(
                onClick = onSubscribeToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (channel.isSubscribed) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(if (channel.isSubscribed) R.string.label_subscribed else R.string.label_subscribe),
                    fontSize = 12.sp
                )
            }
        }
    }
}
