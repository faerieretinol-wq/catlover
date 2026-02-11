package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit, onCommentClick: (String) -> Unit, onLikesClick: (String) -> Unit, onSettingsClick: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = remember { TokenStore(ctx) }
    val api = remember { ApiClient(tokens) }
    val repository = remember { com.catlover.app.data.ProfileRepository(ctx, api) }
    
    var username by remember { mutableStateOf("...") }
    var bio by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var isVerified by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<com.catlover.app.network.SocialStatsResponse?>(null) }
    var myPosts by remember { mutableStateOf<List<com.catlover.app.network.PostItem>>(emptyList()) }
    var myUserId by remember { mutableStateOf("") }
    var isAppAdmin by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<com.catlover.app.network.PostItem?>(null) }
    // РЎРѕСЃС‚РѕСЏРЅРёРµ РґР»СЏ С€РµСЂРёРЅРіР° РїСЂРѕС„РёР»СЏ
    var showShareSheet by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    fun refresh(loadMore: Boolean = false) {
        scope.launch {
            if (loadMore) loadingMore = true else loading = true
            try {
                // РСЃРїРѕР»СЊР·СѓРµРј СЂРµРїРѕР·РёС‚РѕСЂРёР№ СЃ РєСЌС€РёСЂРѕРІР°РЅРёРµРј, РЅРѕ РїСЂРё РїРµСЂРІРѕР№ Р·Р°РіСЂСѓР·РєРµ РїСЂРёРЅСѓРґРёС‚РµР»СЊРЅРѕ РѕР±РЅРѕРІР»СЏРµРј
                val me = repository.getProfile("me", forceRefresh = true)
                if (me != null) {
                    username = me.username
                    myUserId = me.userId
                    isAppAdmin = me.role == "admin"
                    bio = me.bio ?: ""
                    statusText = me.status ?: ""
                    avatarUrl = me.avatarUrl
                    isVerified = me.isVerified
                    
                    val s = withContext(Dispatchers.IO) { api.getSocialStats("me") }
                    stats = s
                    val beforeId = if (loadMore && myPosts.isNotEmpty()) myPosts.last().id else null
                    val p = withContext(Dispatchers.IO) { api.getPosts(false, beforeId = beforeId, u = me.userId) }
                    if (loadMore) {
                        myPosts = myPosts + p.posts
                    } else {
                        myPosts = p.posts
                    }
                    hasMore = p.hasMore
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileScreen", "вќЊ РћС€РёР±РєР° Р·Р°РіСЂСѓР·РєРё РїСЂРѕС„РёР»СЏ: ${e.message}", e)
            }
            finally { 
                loading = false
                loadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                loading = true
                try {
                    val stream = ctx.contentResolver.openInputStream(it)
                    val bytes = stream?.readBytes()
                    stream?.close()
                    if (bytes != null) {
                        // РћРїСЂРµРґРµР»СЏРµРј С‚РёРї С„Р°Р№Р»Р°
                        val mimeType = ctx.contentResolver.getType(it) ?: "image/jpeg"
                        val extension = when {
                            mimeType.contains("gif") -> "gif"
                            mimeType.contains("video") -> "mp4"
                            else -> "jpg"
                        }
                        
                        val uploadRes = withContext(Dispatchers.IO) { 
                            api.uploadImage(bytes, "avatar.$extension") 
                        }
                        withContext(Dispatchers.IO) {
                            api.updateProfile(com.catlover.app.network.ProfileUpdateRequest(avatarUrl = uploadRes.url))
                            refresh()
                        }
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(ctx, "РћС€РёР±РєР° Р·Р°РіСЂСѓР·РєРё: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
                finally { loading = false }
            }
        }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_profile), color = Color.White) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = { 
                            scope.launch {
                                tokens.clear()
                                ctx.stopService(android.content.Intent(ctx, com.catlover.app.services.CatLoverService::class.java))
                                onLogout()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable { launcher.launch("*/*") }, // РџСЂРёРЅРёРјР°РµРј РІСЃРµ С‚РёРїС‹ С„Р°Р№Р»РѕРІ
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNullOrBlank()) {
                        Text(text = username.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        AnimatedAvatar(
                            avatarUrl = avatarUrl,
                            fallbackText = username,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "@$username", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    if (isVerified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                if (statusText.isNotBlank()) {
                    Text(text = statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontStyle = FontStyle.Italic)
                }

                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = bio, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(stats?.posts?.toString() ?: "-", stringResource(R.string.label_posts))
                    StatItem(stats?.followers?.toString() ?: "-", stringResource(R.string.label_followers))
                    StatItem(stats?.following?.toString() ?: "-", stringResource(R.string.label_following))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { showEditDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_edit_profile))
                    }
                    OutlinedButton(onClick = { showShareSheet = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share Profile", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(onClick = { onSettingsClick() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (myPosts.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Icon(Icons.Default.Pets, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(64.dp))
                                 Spacer(modifier = Modifier.height(8.dp))
                                 Text(stringResource(R.string.label_no_posts), color = Color.White.copy(alpha = 0.5f))
                             }
                        }
                    }
                } else {
                    myPosts.forEach { post ->
                        PostCardGlass(
                            post = post,
                            isMyPost = post.userId == myUserId || isAppAdmin,
                            onLike = { scope.launch { try { api.toggleLike(post.id); refresh() } catch (e: Exception) {} } },
                            onCommentClick = { onCommentClick(post.id) },
                            onReport = { },
                            onFollow = { },
                            onDelete = { scope.launch { try { api.deletePost(post.id); refresh() } catch (e: Exception) {} } },
                            onEdit = { editingPost = post },
                            onSave = { },
                            onLikesClick = { onLikesClick(post.id) },
                            onImageClick = { onCommentClick("STORY:$it") },
                            onAuthorClick = { }
                        )
                    }
                    
                    // РќРћР’РћР•: РџР°РіРёРЅР°С†РёСЏ РїРѕСЃС‚РѕРІ
                    if (hasMore) {
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (loadingMore) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                } else {
                                    TextButton(onClick = { refresh(loadMore = true) }) {
                                        Text("Load more posts", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(onClick = { showDeleteAccountDialog = true }) {
                    Text(stringResource(R.string.action_delete) + " Account", color = Color.Red.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // РЎРѕРІСЂРµРјРµРЅРЅС‹Р№ РґРёР°Р»РѕРі СЂРµРґР°РєС‚РёСЂРѕРІР°РЅРёСЏ РїСЂРѕС„РёР»СЏ
    ModernDialog(
        visible = showEditDialog,
        onDismiss = { showEditDialog = false },
        title = stringResource(R.string.action_edit_profile)
    ) {
        OutlinedTextField(
            value = statusText,
            onValueChange = { statusText = it },
            label = { Text("Status", color = Color.White.copy(0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
            )
        )
        
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio", color = Color.White.copy(0.7f)) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showEditDialog = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    scope.launch {
                        try {
                            api.updateProfile(com.catlover.app.network.ProfileUpdateRequest(bio = bio, status = statusText))
                            showEditDialog = false
                            refresh()
                        } catch (e: Exception) {}
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    // РЎРѕРІСЂРµРјРµРЅРЅС‹Р№ РґРёР°Р»РѕРі СѓРґР°Р»РµРЅРёСЏ Р°РєРєР°СѓРЅС‚Р°
    ModernDialog(
        visible = showDeleteAccountDialog,
        onDismiss = { showDeleteAccountDialog = false },
        title = stringResource(R.string.title_delete_account)
    ) {
        Text(
            text = stringResource(R.string.confirm_delete_account),
            color = Color.White.copy(0.8f),
            style = MaterialTheme.typography.bodyLarge
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showDeleteAccountDialog = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    scope.launch {
                        try {
                            api.deleteAccount()
                            tokens.clear()
                            onLogout()
                        } catch (e: Exception) {}
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
    
    // РЎРѕРІСЂРµРјРµРЅРЅС‹Р№ РґРёР°Р»РѕРі СЂРµРґР°РєС‚РёСЂРѕРІР°РЅРёСЏ РїРѕСЃС‚Р°
    if (editingPost != null) {
        var editBody by remember { mutableStateOf(editingPost!!.body) }
        ModernDialog(
            visible = true,
            onDismiss = { editingPost = null },
            title = "Edit Post"
        ) {
            OutlinedTextField(
                value = editBody,
                onValueChange = { editBody = it },
                placeholder = { Text("What's on your mind?", color = Color.White.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { editingPost = null },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val postId = editingPost!!.id
                        scope.launch {
                            try {
                                api.editPost(postId, editBody)
                                editingPost = null
                                refresh()
                            } catch (e: Exception) {}
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
    // Р”РёР°Р»РѕРі С€РµСЂРёРЅРіР° РїСЂРѕС„РёР»СЏ
    if (showShareSheet) {
        AlertDialog(
            onDismissRequest = { showShareSheet = false },
            title = { Text("Share Profile") },
            text = { Text("Share your profile link: catlover://user/$username") },
            confirmButton = {
                Button(onClick = {
                    val clipboard = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("CatLover Profile", "Check out my CatLover profile: catlover://user/$username")
                    clipboard.setPrimaryClip(clip)
                    showShareSheet = false
                    android.widget.Toast.makeText(ctx, "Link copied!", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Copy Link") }
            },
            dismissButton = { TextButton(onClick = { showShareSheet = false }) { Text("Close") } }
        )
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
    }
}

