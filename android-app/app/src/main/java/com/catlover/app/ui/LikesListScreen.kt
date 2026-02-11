package com.catlover.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.UserLike
import com.catlover.app.network.UserSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesListScreen(postId: String, onBack: () -> Unit, onNavigateToChat: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var likes by remember { mutableStateOf<List<UserLike>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var viewingUser by remember { mutableStateOf<UserSummary?>(null) }
    var myUserId by remember { mutableStateOf("") }

    LaunchedEffect(postId) {
        loading = true
        try {
            val me = withContext(Dispatchers.IO) { api.getProfileMe() }
            myUserId = me.userId
            val res = withContext(Dispatchers.IO) { api.getPostLikes(postId) }
            likes = res.likes
        } catch (e: Exception) {}
        finally { loading = false }
    }

    GlassBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Likes", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (loading && likes.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                } else if (likes.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.label_no_likes), color = Color.White.copy(alpha = 0.3f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(likes) { user ->
                            val summary = UserSummary(id = user.id, username = user.username, avatarUrl = user.avatarUrl, bio = null, isFollowing = 0)
                            UserListItem(
                                user = summary, 
                                onClick = {
                                    scope.launch {
                                        try {
                                            val full = withContext(Dispatchers.IO) { api.getPublicProfile(user.username) }
                                            viewingUser = UserSummary(user.id, full.username, full.avatarUrl, full.bio, full.status, 0)
                                        } catch (e: Exception) {}
                                    }
                                }, 
                                onFollowToggle = {
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { api.followUser(user.id) }
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
                                        } catch (e: Exception) {}
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
                    } catch (e: Exception) {}
                    finally { viewingUser = null }
                }
            }
        )
    }
}
