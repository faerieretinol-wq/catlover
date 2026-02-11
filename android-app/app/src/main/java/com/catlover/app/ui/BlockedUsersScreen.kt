package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.UserSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var users by remember { mutableStateOf<List<UserSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                val res = withContext(Dispatchers.IO) { api.getBlockedUsers() }
                users = res.users
            } catch (e: Exception) {}
            finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    GlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Blocked Users", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (loading && users.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (users.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No blocked users", color = Color.White.copy(alpha = 0.3f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        items(users) { user ->
                            BlockedUserItem(user, onUnblock = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { api.unblockUser(user.id) }
                                        refresh()
                                    } catch (e: Exception) {}
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUserItem(user: UserSummary, onUnblock: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl.isNullOrBlank()) {
                    Text(user.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    AsyncImage(
                        model = if (user.avatarUrl.startsWith("http")) user.avatarUrl else "${com.catlover.app.BuildConfig.BASE_URL}${user.avatarUrl}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = onUnblock, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))) {
                Text("Unblock", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
