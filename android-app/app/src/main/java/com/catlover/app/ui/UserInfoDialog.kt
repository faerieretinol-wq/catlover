package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.catlover.app.R
import com.catlover.app.network.ApiClient
import com.catlover.app.network.UserSummary
import com.catlover.app.network.SocialStatsResponse
import com.catlover.app.data.TokenStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun UserInfoDialog(user: UserSummary, isMe: Boolean, onDismiss: () -> Unit, onMessage: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    var stats by remember { mutableStateOf<SocialStatsResponse?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    
    var bio by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }

    LaunchedEffect(user.id) {
        try {
            val publicProfile = withContext(Dispatchers.IO) { api.getPublicProfile(user.id) }
            bio = publicProfile.bio ?: ""
            statusText = publicProfile.status ?: ""
            
            stats = withContext(Dispatchers.IO) { api.getSocialStats(user.id) }
        } catch (e: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("@${user.username}", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatarUrl.isNullOrBlank()) {
                        Text(user.username.take(1).uppercase(), fontSize = 32.sp, color = Color.White)
                    } else {
                        val fullUrl = remember(user.avatarUrl) {
                            if (user.avatarUrl!!.startsWith("http")) user.avatarUrl 
                            else {
                                val base = com.catlover.app.BuildConfig.BASE_URL.trimEnd('/')
                                val path = user.avatarUrl!!.trimStart('/')
                                "$base/$path"
                            }
                        }
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SmallStatItem(stats?.posts?.toString() ?: "-", "Posts")
                    SmallStatItem(stats?.followers?.toString() ?: "-", "Fans")
                    SmallStatItem(stats?.following?.toString() ?: "-", "Following")
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (statusText.isNotBlank()) {
                    Text(statusText, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (bio.isNotBlank()) {
                    Text(bio, textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            if (!isMe) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onMessage) { Text(stringResource(R.string.action_message)) }
                    Button(
                        onClick = { showReportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) { Text(stringResource(R.string.action_report)) }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
    
    // Диалог жалобы
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(stringResource(R.string.title_report_user)) },
            text = {
                Column {
                    Text(stringResource(R.string.hint_report_user, user.username), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text(stringResource(R.string.label_reason)) },
                        placeholder = { Text(stringResource(R.string.hint_spam_harassment)) },
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
                                    api.reportContent(user.id, "USER", reportReason) 
                                }
                                android.widget.Toast.makeText(ctx, ctx.getString(R.string.toast_report_submitted), android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(ctx, ctx.getString(R.string.toast_report_failed), android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                showReportDialog = false
                                onDismiss()
                            }
                        }
                    }
                ) { Text(stringResource(R.string.action_submit)) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
fun SmallStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}
