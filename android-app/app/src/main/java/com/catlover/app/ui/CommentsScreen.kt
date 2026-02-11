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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.QuestionAnswer
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
import com.catlover.app.network.CommentItem
import com.catlover.app.network.UserSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(postId: String, onBack: () -> Unit, onNavigateToChat: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var myUserId by remember { mutableStateOf("") }
    var viewingUser by remember { mutableStateOf<UserSummary?>(null) }
    var body by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportingCommentId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                if (myUserId.isEmpty()) {
                    myUserId = withContext(Dispatchers.IO) { api.getProfileMe().userId }
                }
                val res = withContext(Dispatchers.IO) { api.getComments(postId) }
                comments = res.comments
            } catch (e: Exception) {}
            finally { loading = false }
        }
    }

    LaunchedEffect(postId) { refresh() }

    GlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.section_comments), color = Color.White) },
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
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    if (loading && comments.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                    } else if (comments.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No comments yet", color = Color.White.copy(alpha = 0.3f))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(comments) { comment ->
                                val isMe = comment.userId == myUserId
                                CommentItemGlass(
                                    comment = comment,
                                    isMe = isMe,
                                    onAuthorClick = {
                                        scope.launch {
                                            try {
                                                val full = withContext(Dispatchers.IO) { api.getPublicProfile(comment.username ?: "") }
                                                viewingUser = UserSummary(comment.userId, full.username, full.avatarUrl, full.bio, full.status, 0)
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { api.deleteComment(postId, comment.id) }
                                                refresh()
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    onReport = {
                                        reportingCommentId = comment.id
                                        showReportDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                Surface(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.label_message_placeholder), color = Color.White.copy(alpha = 0.5f)) },
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (body.isNotBlank()) {
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { api.addComment(postId, body) }
                                            body = ""
                                            refresh()
                                        } catch (e: Exception) {}
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
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
    
    // Диалог жалобы на комментарий
    if (showReportDialog && reportingCommentId != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false; reportingCommentId = null },
            title = { Text("Report Comment") },
            text = {
                Column {
                    Text("Why are you reporting this comment?", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Reason") },
                        placeholder = { Text("Spam, harassment, inappropriate content, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = reportReason.isNotBlank(),
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { 
                                    api.reportContent(reportingCommentId!!, "COMMENT", reportReason) 
                                }
                                Toast.makeText(ctx, "Report submitted", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Failed to report", Toast.LENGTH_SHORT).show()
                            } finally {
                                showReportDialog = false
                                reportingCommentId = null
                                reportReason = ""
                            }
                        }
                    }
                ) { Text(stringResource(R.string.action_submit)) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false; reportingCommentId = null; reportReason = "" }) { 
                    Text(stringResource(R.string.action_cancel)) 
                }
            }
        )
    }
}

@Composable
fun CommentItemGlass(comment: CommentItem, isMe: Boolean, onDelete: () -> Unit, onReport: () -> Unit, onAuthorClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { onAuthorClick() },
            contentAlignment = Alignment.Center
        ) {
            if (comment.avatarUrl.isNullOrBlank()) {
                Text((comment.username ?: comment.userId).take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            } else {
                val fullUrl = remember(comment.avatarUrl) {
                    if (comment.avatarUrl!!.startsWith("http")) comment.avatarUrl 
                    else {
                        val base = com.catlover.app.BuildConfig.BASE_URL.trimEnd('/')
                        val path = comment.avatarUrl!!.trimStart('/')
                        "$base/$path"
                    }
                }
                AsyncImage(model = fullUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        GlassCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)) {
            Column(modifier = Modifier.padding(12.dp).clickable { onAuthorClick() }) {
                Text(text = comment.username ?: "User ${comment.userId.take(4)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = comment.body, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = comment.createdAt.take(10), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
            }
        }
        IconButton(onClick = if (isMe) onDelete else onReport) {
            Icon(
                imageVector = if (isMe) Icons.Default.Delete else Icons.Default.Flag,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
