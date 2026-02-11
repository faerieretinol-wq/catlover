package com.catlover.app

import android.os.Bundle
import android.widget.Toast
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.catlover.app.data.TokenStore
import com.catlover.app.data.E2EKeyManager
import com.catlover.app.network.ApiClient
import com.catlover.app.ui.*
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    private var deepLinkShareId by mutableStateOf<String?>(null)
    private var callParams by mutableStateOf<Bundle?>(null)
    private var intentExtras by mutableStateOf<Bundle?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkShareId = extractShareId()
        callParams = intent.extras
        intentExtras = intent.extras
        setContent { AppRoot(deepLinkShareId, callParams, intentExtras) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkShareId = extractShareId()
        callParams = intent.extras
        intentExtras = intent.extras
    }

    private fun extractShareId(): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "catlover" || data.host != "style") return null
        return data.path?.trimStart('/')?.takeIf { it.isNotBlank() }
    }
}

@Composable
fun AppRoot(deepLinkShareId: String?, initialCallParams: android.os.Bundle? = null, intentExtras: android.os.Bundle? = null) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var banSocket by remember { mutableStateOf<io.socket.client.Socket?>(null) }
    
    var activeCallUserId by remember { mutableStateOf<String?>(null) }
    var otherCallName by remember { mutableStateOf("User") }
    var isVideoCall by remember { mutableStateOf(false) }
    var isIncomingCall by remember { mutableStateOf(false) }
    var isCallAcceptedByOther by remember { mutableStateOf(false) }
    var remoteSdp by remember { mutableStateOf<String?>(null) }
    var remoteCandidates by remember { mutableStateOf(emptyList<org.webrtc.IceCandidate>()) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { }

    val start = remember {
        val token = TokenStore(ctx).getAccessToken()
        if (token.isNullOrBlank()) "register" else "feed"
    }

    LaunchedEffect(initialCallParams, intentExtras) {
        // Обработка входящих звонков
        initialCallParams?.getString("CALL_USER_ID")?.let { uid ->
            isCallAcceptedByOther = false; remoteSdp = null; remoteCandidates = emptyList()
            activeCallUserId = uid
            otherCallName = initialCallParams.getString("CALL_USER_NAME", "User")
            isVideoCall = initialCallParams.getBoolean("CALL_IS_VIDEO", false)
            isIncomingCall = initialCallParams.getBoolean("CALL_INCOMING", false)
            nav.navigate("call")
            return@LaunchedEffect
        }
        
        // Обработка уведомлений - открытие чата или экрана уведомлений
        intentExtras?.getString("TARGET_SCREEN")?.let { screen ->
            when (screen) {
                "chat" -> {
                    val chatId = intentExtras.getString("CHAT_ID")
                    if (!chatId.isNullOrBlank()) {
                        nav.navigate("chat/$chatId")
                    }
                }
                "notifications" -> {
                    nav.navigate("notifications")
                }
            }
        }
    }

    // НОВОЕ: Обработка Shared Theme
    if (deepLinkShareId != null) {
        var showApplyDialog by remember { mutableStateOf(true) }
        val api = remember { ApiClient(TokenStore(ctx)) }
        val scope = rememberCoroutineScope()
        
        if (showApplyDialog) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                title = { Text("Apply Shared Theme?") },
                text = { Text("Do you want to apply the theme from this link?") },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            try {
                                val res = withContext(Dispatchers.IO) { api.getSharedStyle(deepLinkShareId) }
                                // res.settings содержит JSON. Сохраняем его.
                                withContext(Dispatchers.IO) { api.saveSettings(res.settings) }
                                Toast.makeText(ctx, "Theme applied!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Failed to apply theme", Toast.LENGTH_SHORT).show()
                            } finally {
                                showApplyDialog = false
                            }
                        }
                    }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = { showApplyDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        val token = TokenStore(ctx).getAccessToken()
        val perms = mutableListOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        permissionLauncher.launch(perms.toTypedArray())

        // Upload crash logs
        scope.launch(Dispatchers.IO) {
            try {
                val files = ctx.filesDir.listFiles { _, name -> name.startsWith("crash_") && name.endsWith(".txt") }
                if (!files.isNullOrEmpty() && !token.isNullOrBlank()) {
                    val api = ApiClient(TokenStore(ctx))
                    for (file in files) {
                        try {
                            val content = file.readText()
                            api.uploadCrashLog(content)
                            file.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        if (!token.isNullOrBlank()) {
            val api = ApiClient(TokenStore(ctx)); val e2e = E2EKeyManager(ctx)
            val opts = io.socket.client.IO.Options().apply { auth = mapOf("token" to token) }
            val s = io.socket.client.IO.socket(BuildConfig.BASE_URL, opts)
            
            s.on("banned") { 
                scope.launch(Dispatchers.Main) { 
                    if (ctx is android.app.Activity && !ctx.isDestroyed && !ctx.isFinishing) {
                        TokenStore(ctx).clear()
                        ctx.stopService(android.content.Intent(ctx, com.catlover.app.services.CatLoverService::class.java))
                        nav.navigate("register") { popUpTo(0) }
                        Toast.makeText(ctx, "Suspended", Toast.LENGTH_LONG).show()
                    }
                } 
            }
            
            // Глобальные уведомления
            s.on("notification") { args ->
                try {
                    val data = args[0] as JSONObject
                    val body = data.optString("body", "New notification")
                    scope.launch(Dispatchers.Main) {
                        if (ctx is android.app.Activity && !ctx.isDestroyed && !ctx.isFinishing) {
                            Toast.makeText(ctx, body, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {}
            }

            s.on("incoming_call") { args -> 
                try {
                    val data = args[0] as JSONObject
                    scope.launch(Dispatchers.Main) { 
                        isCallAcceptedByOther = false
                        remoteCandidates = emptyList()
                        activeCallUserId = data.optString("fromUserId")
                        if (activeCallUserId.isNullOrEmpty()) return@launch
                        
                        otherCallName = data.optString("senderName", "User")
                        isVideoCall = data.optString("type") == "video"
                        
                        val sdp = data.optString("sdp", "")
                        remoteSdp = if (sdp.isNotEmpty()) sdp else null
                        
                        isIncomingCall = true 
                        if (nav.currentBackStackEntry != null && nav.currentDestination?.route != "call") {
                            nav.navigate("call")
                        }
                    } 
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            s.on("call_response") { args -> 
                try {
                    val data = args[0] as JSONObject
                    scope.launch(Dispatchers.Main) { 
                        if (data.optBoolean("accepted", false)) { 
                            isCallAcceptedByOther = true
                            val answer = data.optString("answer", "")
                            remoteSdp = if (answer.isNotEmpty()) answer else null
                        } else { 
                            activeCallUserId = null
                            isCallAcceptedByOther = false
                            remoteCandidates = emptyList()
                            if (nav.currentBackStackEntry != null && nav.currentDestination?.route == "call") nav.popBackStack() 
                        } 
                    } 
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            s.on("ice_candidate") { args -> 
                try {
                    val data = args[0] as JSONObject
                    val mid = data.optString("sdpMid")
                    val index = data.optInt("sdpMLineIndex", -1)
                    val sdp = data.optString("sdp")
                    
                    if (mid != null && index != -1 && sdp != null) {
                        scope.launch(Dispatchers.Main) { 
                            remoteCandidates = remoteCandidates + org.webrtc.IceCandidate(mid, index, sdp)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            s.on("call_end") { 
                scope.launch(Dispatchers.Main) { 
                    activeCallUserId = null
                    isCallAcceptedByOther = false
                    remoteSdp = null
                    remoteCandidates = emptyList()
                    if (nav.currentDestination?.route == "call") nav.popBackStack() 
                } 
            }

            s.connect(); banSocket = s
            scope.launch(Dispatchers.IO) {
                try {
                    api.uploadIdentityKey(e2e.getOrCreateIdentityPublicKey())
                    val intent = android.content.Intent(ctx, com.catlover.app.services.CatLoverService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent) else ctx.startService(intent)
                } catch (e: Exception) {}
            }
        }
    }

    DisposableEffect(Unit) { onDispose { banSocket?.disconnect() } }

    CatLoverTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navBackStackEntry by nav.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            Scaffold(
                bottomBar = {
                    if (currentRoute in listOf("feed", "search", "notifications", "chats", "profile")) {
                        NavigationBar {
                            NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text(stringResource(R.string.title_feed)) }, selected = currentRoute == "feed", onClick = { nav.navigate("feed") { popUpTo("feed") { inclusive = true } } })
                            NavigationBarItem(icon = { Icon(Icons.Default.Search, null) }, label = { Text(stringResource(R.string.title_search)) }, selected = currentRoute == "search", onClick = { nav.navigate("search") { launchSingleTop = true } })
                            NavigationBarItem(icon = { Icon(Icons.Default.Notifications, null) }, label = { Text(stringResource(R.string.title_alerts)) }, selected = currentRoute == "notifications", onClick = { nav.navigate("notifications") { launchSingleTop = true } })
                            NavigationBarItem(icon = { Icon(Icons.Default.Email, null) }, label = { Text(stringResource(R.string.title_chats)) }, selected = currentRoute == "chats", onClick = { nav.navigate("chats") { launchSingleTop = true } })
                            NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text(stringResource(R.string.title_profile)) }, selected = currentRoute == "profile", onClick = { nav.navigate("profile") { launchSingleTop = true } })
                        }
                    }
                }
            ) { padding ->
                NavHost(navController = nav, startDestination = start, modifier = Modifier.padding(padding)) {
                    composable("register") { RegistrationScreen(onRegistered = { nav.navigate("wizard") }, onLogin = { nav.navigate("login") }) }
                    composable("login") { LoginScreen(onLoggedIn = { nav.navigate("feed") }, onBackToRegister = { nav.navigate("register") }, onForgotPassword = { nav.navigate("forgot_password") }) }
                    composable("forgot_password") { ForgotPasswordScreen(onBack = { nav.popBackStack() }, onSuccess = { nav.navigate("login") { popUpTo("login") { inclusive = false } } }) }
                    composable("wizard") { SettingsWizardScreen(onLoggedOut = { nav.navigate("login") }, onProfile = { nav.navigate("profile") }, initialShareId = deepLinkShareId) }
                    composable("feed") { FeedScreen(onCommentClick = { nav.navigate("comments/$it") }, onStoryClick = { nav.navigate("lightbox/${URLEncoder.encode(it, "UTF-8")}") }, onNavigateToChat = { nav.navigate("chat/$it") }, onLikesClick = { nav.navigate("likes/$it") }) }
                    composable("lightbox/{url}") { LightboxScreen(imageUrl = URLDecoder.decode(it.arguments?.getString("url") ?: "", "UTF-8"), onDismiss = { nav.popBackStack() }) }
                    composable("comments/{postId}") { CommentsScreen(postId = it.arguments?.getString("postId") ?: "", onBack = { nav.popBackStack() }, onNavigateToChat = { cid -> nav.navigate("chat/$cid") }) }
                    composable("likes/{postId}") { LikesListScreen(postId = it.arguments?.getString("postId") ?: "", onBack = { nav.popBackStack() }, onNavigateToChat = { cid -> nav.navigate("chat/$cid") }) }
                    composable("search") { SearchScreen(onNavigateToChat = { nav.navigate("chat/$it") }) }
                    composable("notifications") { NotificationsScreen(onPostClick = { if (it.startsWith("CHAT:")) nav.navigate("chat/${it.removePrefix("CHAT:")}") else nav.navigate("comments/$it") }, onUserClick = {}) }
                    composable("chats") { ChatListScreen(onChatSelected = { nav.navigate("chat/$it") }, onCreateChannel = { nav.navigate("create_channel") }) }
                    composable("create_channel") { CreateChannelScreen(onBack = { nav.popBackStack() }, onChannelCreated = { channelId -> nav.navigate("chat/$channelId") { popUpTo("chats") } }) }
                    composable("chat/{chatId}") { 
                        ChatDetailScreen(
                            chatId = it.arguments?.getString("chatId") ?: "",
                            onBack = { nav.popBackStack() },
                            onCall = { uid, video -> 
                                isCallAcceptedByOther = false
                                remoteSdp = null
                                remoteCandidates = emptyList()
                                activeCallUserId = uid
                                isVideoCall = video
                                isIncomingCall = false
                                nav.navigate("call")
                            },
                            onImageClick = { url -> nav.navigate("lightbox/${URLEncoder.encode(url, "UTF-8")}") },
                            onGroupCall = { chatId -> nav.navigate("group_call/$chatId") },
                            onChannelSettings = { channelId -> nav.navigate("channel_settings/$channelId") }
                        )
                    }
                    composable("channel_settings/{channelId}") { 
                        val channelId = it.arguments?.getString("channelId") ?: ""
                        ChannelSettingsScreen(channelId = channelId, onBack = { nav.popBackStack() })
                    }
                    composable("group_call/{chatId}") {
                        val chatId = it.arguments?.getString("chatId") ?: ""
                        GroupCallScreen(chatId = chatId, onDismiss = { nav.popBackStack() })
                    }
                    composable("call") {
                        com.catlover.app.services.CatLoverService.cancelCallNotification(ctx)
                        CallScreen(
                            otherUserId = activeCallUserId ?: "",
                            otherName = otherCallName,
                            isVideo = isVideoCall,
                            incoming = isIncomingCall,
                            isAcceptedByOther = isCallAcceptedByOther,
                            initialRemoteSdp = remoteSdp,
                            remoteCandidates = remoteCandidates,
                            socket = banSocket,
                            onDismiss = {
                                activeCallUserId = null
                                isCallAcceptedByOther = false
                                remoteSdp = null
                                remoteCandidates = emptyList()
                                nav.popBackStack()
                            }
                        )
                    }
                    composable("profile") { ProfileScreen(onBack = { nav.navigate("feed") }, onLogout = { nav.navigate("register") { popUpTo(0) } }, onCommentClick = { if (it.startsWith("STORY:")) nav.navigate("lightbox/${URLEncoder.encode(it.removePrefix("STORY:"), "UTF-8")}") else nav.navigate("comments/$it") }, onLikesClick = { nav.navigate("likes/$it") }, onSettingsClick = { nav.navigate("settings") }) }
                    composable("blocked") { BlockedUsersScreen(onBack = { nav.popBackStack() }) }
                    composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }, onBlockedClick = { nav.navigate("blocked") }, onMyChannelsClick = { nav.navigate("my_channels") }) }
                    composable("my_channels") { MyChannelsScreen(onBack = { nav.popBackStack() }, onChannelClick = { nav.navigate("chat/$it") }, onCreateChannel = { nav.navigate("create_channel") }) }
                }
            }
        }
    }
}
