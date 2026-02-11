package com.catlover.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.PostItem
import com.catlover.app.network.StoriesResponse
import com.catlover.app.network.UserStories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(onCommentClick: (String) -> Unit, onStoryClick: (String) -> Unit, onNavigateToChat: (String) -> Unit, onLikesClick: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var posts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var storiesFeed by remember { mutableStateOf<List<UserStories>>(emptyList()) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    // Состояние для поиска
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Состояние для создания контента
    var showCreatePost by remember { mutableStateOf(false) }
    var showCreateStory by remember { mutableStateOf(false) }
    var showStoryCreator by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<PostItem?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportingPostId by remember { mutableStateOf<String?>(null) }
    var myUserId by remember { mutableStateOf("") }
    var viewingUserSummary by remember { mutableStateOf<com.catlover.app.network.UserSummary?>(null) }
    var isAppAdmin by remember { mutableStateOf(false) }

    // State for Tabs
    var followedOnly by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }

    fun refresh(loadMore: Boolean = false) {
        scope.launch {
            if (loadMore) loadingMore = true else refreshing = true
            try {
                if (myUserId.isEmpty()) {
                    val me = withContext(Dispatchers.IO) { api.getProfileMe() }
                    myUserId = me.userId
                    isAppAdmin = me.role == "admin"
                }
                val beforeId = if (loadMore && posts.isNotEmpty()) posts.last().id else null
                
                val p = withContext(Dispatchers.IO) {
                    if (showSaved) {
                        api.getSavedPosts(beforeId)
                    } else {
                        api.getPosts(followedOnly, beforeId = beforeId)
                    }
                }
                
                if (loadMore) {
                    posts = posts + p.posts
                } else {
                    posts = p.posts
                }
                hasMore = p.hasMore
                
                if (!loadMore && !showSaved) {
                    val s = withContext(Dispatchers.IO) { api.getStories() }
                    storiesFeed = s.feed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally { 
                refreshing = false
                loadingMore = false
            }
        }
    }

    LaunchedEffect(followedOnly, showSaved) { refresh() }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.background(Color.Transparent)) {
                    CenterAlignedTopAppBar(
                        title = { Text("CatLover", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                        navigationIcon = {
                            IconButton(onClick = { showSearchDialog = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCreateStory = true }) { Icon(Icons.Default.AddAPhoto, contentDescription = "Story", tint = Color.White) }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    // Tab Row for Feed filtering
                    val selectedTabIndex = if (showSaved) 2 else (if (followedOnly) 1 else 0)
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ) {
                        Tab(selected = !followedOnly && !showSaved, onClick = { followedOnly = false; showSaved = false }, text = { Text(stringResource(R.string.tab_all)) })
                        Tab(selected = followedOnly && !showSaved, onClick = { followedOnly = true; showSaved = false }, text = { Text(stringResource(R.string.tab_following)) })
                        Tab(selected = showSaved, onClick = { showSaved = true; followedOnly = false }, text = { Text(stringResource(R.string.tab_saved)) })
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreatePost = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape) {
                    Icon(Icons.Default.Add, contentDescription = "Post")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (!showSaved) {
                        item {
                            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(storiesFeed) { userStories ->
                                    StoryCircle(userStories) { onStoryClick(it) }
                                }
                            }
                        }
                    }
                    
                    if (posts.isEmpty() && !refreshing) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (showSaved) stringResource(R.string.label_no_saved_posts) else stringResource(R.string.label_no_posts_found), color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }

                    items(posts) { post ->
                        PostCardGlass(
                            post = post,
                            isMyPost = post.userId == myUserId || isAppAdmin,
                            onLike = { scope.launch { try { api.toggleLike(post.id); refresh() } catch(e:Exception){} } },
                            onCommentClick = { onCommentClick(post.id) },
                            onReport = { 
                                showReportDialog = true
                                reportingPostId = post.id
                            },
                            onFollow = { scope.launch { try { api.followUser(post.userId); refresh() } catch(e:Exception){} } },
                            onDelete = { scope.launch { try { api.deletePost(post.id); refresh() } catch(e:Exception){} } },
                            onEdit = { editingPost = post },
                            onSave = { 
                                scope.launch { 
                                    try { 
                                        api.toggleSavePost(post.id)
                                        // Update local state or refresh
                                        refresh() 
                                    } catch(e: Exception){} 
                                } 
                            },
                            onAuthorClick = { viewingUserSummary = it },
                            onLikesClick = { onLikesClick(post.id) },
                            onImageClick = { onStoryClick(it) }
                        )
                    }
                    
                    if (hasMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (loadingMore) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                } else {
                                    TextButton(onClick = { refresh(loadMore = true) }) {
                                        Text(stringResource(R.string.action_load_more), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreatePost) { CreatePostDialog(onDismiss = { showCreatePost = false }, onCreated = { showCreatePost = false; refresh() }) }
    if (showCreateStory) { CreateStoryDialog(onDismiss = { showCreateStory = false }, onCreated = { showCreateStory = false; refresh() }) }
    
    if (editingPost != null) {
        EditPostDialog(
            post = editingPost!!,
            onDismiss = { editingPost = null },
            onEdited = { editingPost = null; refresh() }
        )
    }
    
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Global Search") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search users or posts...") },
                        trailingIcon = {
                            IconButton(onClick = { /* trigger search */ }) {
                                Icon(Icons.Default.Search, null)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun StoryCircle(user: UserStories, onClick: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp).clickable { user.stories.firstOrNull()?.let { onClick(it.imageUrl) } }) {
        Box(modifier = Modifier.size(64.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
            AnimatedAvatar(
                avatarUrl = user.avatarUrl,
                fallbackText = user.username,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = user.username, fontSize = 10.sp, color = Color.White, maxLines = 1)
    }
}

@Composable
fun PostCardGlass(post: PostItem, isMyPost: Boolean, onLike: () -> Unit, onCommentClick: () -> Unit, onReport: () -> Unit, onFollow: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit, onSave: () -> Unit, onAuthorClick: (com.catlover.app.network.UserSummary) -> Unit, onLikesClick: () -> Unit, onImageClick: (String) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f).clickable { onAuthorClick(com.catlover.app.network.UserSummary(post.userId, post.username ?: "", post.avatarUrl, null, null, post.isFollowing)) }, verticalAlignment = Alignment.CenterVertically) {
                    AnimatedAvatar(
                        avatarUrl = post.avatarUrl,
                        fallbackText = post.username ?: post.userId,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = post.username ?: stringResource(R.string.label_user), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            if (!isMyPost && post.isFollowing == 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "• ${stringResource(R.string.action_follow)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { onFollow() })
                            }
                        }
                        Text(text = post.createdAt.take(10), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                
                // Save/Bookmark Button
                IconButton(onClick = onSave) {
                    // Assuming savedByMe is added to PostItem, otherwise use a placeholder check
                    Icon(if (post.id.hashCode() % 3 == 0) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Save", tint = Color.White.copy(alpha = 0.7f))
                }

                IconButton(onClick = if (isMyPost) onDelete else onReport) {
                    Icon(if (isMyPost) Icons.Default.Delete else Icons.Default.Flag, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                }
                if (isMyPost) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.body, style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold), lineHeight = 22.sp)
            
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                val fullUrl = ApiClient.formatMediaUrl(post.imageUrl) ?: ""
                
                if (post.imageUrl.endsWith(".mp4")) {
                    VideoPlayer(
                        url = fullUrl, 
                        modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black)
                    )
                } else {
                    AsyncImage(
                        model = fullUrl, 
                        contentDescription = null, 
                        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { onImageClick(post.imageUrl!!) }, 
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val likeColor by animateColorAsState(if (post.likedByMe) Color(0xFFFF4081) else Color.White.copy(alpha = 0.6f), label = "LikeColor")
                IconButton(onClick = onLike) { Icon(if (post.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = likeColor) }
                Text(text = "${post.likes}", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.clickable { onLikesClick() })
                Spacer(modifier = Modifier.width(24.dp))
                Row(modifier = Modifier.clickable { onCommentClick() }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${post.comments}", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var body by remember { mutableStateOf("") }
    var mediaBytes by remember { mutableStateOf<ByteArray?>(null) }
    var mediaType by remember { mutableStateOf("image/jpeg") }
    var loading by remember { mutableStateOf(false) }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            val type = ctx.contentResolver.getType(it) ?: "image/jpeg"
            mediaType = type
            val stream = ctx.contentResolver.openInputStream(it)
            mediaBytes = stream?.readBytes()
            stream?.close() 
        } 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text(stringResource(R.string.title_new_post)) }, 
        text = { 
            Column { 
                OutlinedTextField(value = body, onValueChange = { body = it }, placeholder = { Text(stringResource(R.string.hint_whats_on_mind)) }, modifier = Modifier.fillMaxWidth().height(120.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch("*/*") }) { 
                    Text(if (mediaBytes != null) (if (mediaType.contains("video")) stringResource(R.string.label_video_selected) else stringResource(R.string.label_photo_selected)) else stringResource(R.string.action_add_photo_video)) 
                } 
            } 
        }, 
        confirmButton = { 
            Button(enabled = body.isNotBlank() && !loading, onClick = { 
                scope.launch { 
                    loading = true
                    try { 
                        var url: String? = null
                        if (mediaBytes != null) {
                            val ext = if (mediaType.contains("video")) "mp4" else "jpg"
                            url = api.uploadImage(mediaBytes!!, "post.$ext").url
                        }
                        api.createPost(body, url)
                        onCreated() 
                    } catch (e: Exception) {} 
                    finally { loading = false } 
                } 
            }) { Text(stringResource(R.string.action_post)) } 
        }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun CreateStoryDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var mediaType by remember { mutableStateOf("image/jpeg") }
    var loading by remember { mutableStateOf(false) }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            val type = ctx.contentResolver.getType(it) ?: "image/jpeg"
            mediaType = type
            val stream = ctx.contentResolver.openInputStream(it)
            imageBytes = stream?.readBytes()
            stream?.close() 
        } 
    }
    
    LaunchedEffect(Unit) { launcher.launch("*/*") } 
    
    if (imageBytes != null) { 
        AlertDialog(
            onDismissRequest = onDismiss, 
            title = { Text(stringResource(R.string.title_new_story)) }, 
            text = { Text(if (mediaType.contains("video")) stringResource(R.string.hint_share_video_story) else stringResource(R.string.hint_share_story)) }, 
            confirmButton = { 
                Button(enabled = !loading, onClick = { 
                    scope.launch { 
                        loading = true
                        try { 
                            val ext = if (mediaType.contains("video")) "mp4" else "jpg"
                            val url = api.uploadImage(imageBytes!!, "story.$ext").url
                            api.createStory(url)
                            onCreated() 
                        } catch (e: Exception) {} 
                        finally { loading = false } 
                    } 
                }) { Text(stringResource(R.string.action_share)) } 
            }, 
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        ) 
    }
}

@Composable
fun EditPostDialog(post: PostItem, onDismiss: () -> Unit, onEdited: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var body by remember { mutableStateOf(post.body) }
    var loading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_edit_post)) },
        text = {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text(stringResource(R.string.hint_whats_on_mind)) },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        },
        confirmButton = {
            Button(enabled = body.isNotBlank() && !loading, onClick = {
                scope.launch {
                    loading = true
                    try {
                        api.editPost(post.id, body)
                        onEdited()
                    } catch (e: Exception) {}
                    finally { loading = false }
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun ReportPostDialog(postId: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var reason by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_report_post)) },
        text = {
            Column {
                Text(stringResource(R.string.hint_report_reason), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.label_reason)) },
                    placeholder = { Text(stringResource(R.string.hint_report_details)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(enabled = reason.isNotBlank() && !loading, onClick = {
                scope.launch {
                    loading = true
                    try {
                        api.reportContent(postId, "POST", reason)
                        android.widget.Toast.makeText(ctx, ctx.getString(R.string.toast_report_submitted), android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(ctx, ctx.getString(R.string.toast_report_failed), android.widget.Toast.LENGTH_SHORT).show()
                    } finally { loading = false }
                }
            }) { Text(stringResource(R.string.action_submit)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
