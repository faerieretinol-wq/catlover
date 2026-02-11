package com.catlover.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.messenger.network.ApiClient
import com.catlover.messenger.network.Chat
import com.catlover.messenger.network.User
import com.catlover.messenger.network.UserProfile
import com.catlover.messenger.network.Message
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Убираем белую полоску сверху
        window.statusBarColor = android.graphics.Color.parseColor("#0A0A0A")
        window.navigationBarColor = android.graphics.Color.parseColor("#0A0A0A")
        
        setContent {
            MaterialTheme {
                AppScreen()
            }
        }
    }
}

@Composable
fun AppScreen() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    
    if (!isLoggedIn) {
        if (isRegistering) {
            RegisterScreen(
                onRegisterSuccess = { isLoggedIn = true },
                onBackToLogin = { isRegistering = false }
            )
        } else {
            LoginScreen(
                onLoginSuccess = { isLoggedIn = true },
                onGoToRegister = { isRegistering = true }
            )
        }
    } else {
        MainScreen()
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Стеклянная карточка
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = Color(0x30FFFFFF),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "CatLover",
                    fontSize = 36.sp,
                    color = Color(0xFFBD00FF),
                    style = MaterialTheme.typography.headlineLarge
                )
                
                Text(
                    "Messenger",
                    fontSize = 20.sp,
                    color = Color(0xFF00F3FF)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Почта", color = Color(0xCCFFFFFF)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xCCFFFFFF),
                        focusedBorderColor = Color(0xFFBD00FF),
                        unfocusedBorderColor = Color(0x80FFFFFF),
                        cursorColor = Color(0xFFBD00FF)
                    )
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль", color = Color(0xCCFFFFFF)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xCCFFFFFF),
                        focusedBorderColor = Color(0xFFBD00FF),
                        unfocusedBorderColor = Color(0x80FFFFFF),
                        cursorColor = Color(0xFFBD00FF)
                    )
                )
                
                if (error.isNotEmpty()) {
                    Text(error, color = Color(0xFFFF5555))
                }
                
                Button(
                    onClick = {
                        isLoading = true
                        error = ""
                        scope.launch {
                            ApiClient.login(email, password).fold(
                                onSuccess = { onLoginSuccess() },
                                onFailure = { 
                                    error = "Ошибка входа"
                                    isLoading = false
                                }
                            )
                        }
                    },
                    enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBD00FF),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isLoading) "Загрузка..." else "Войти", fontSize = 16.sp)
                }
                
                TextButton(onClick = onGoToRegister) {
                    Text("Создать аккаунт", color = Color(0xFF00F3FF))
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Стеклянная карточка
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = Color(0x30FFFFFF),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Регистрация",
                    fontSize = 32.sp,
                    color = Color(0xFFBD00FF),
                    style = MaterialTheme.typography.headlineMedium
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Почта", color = Color(0xCCFFFFFF)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xCCFFFFFF),
                        focusedBorderColor = Color(0xFFBD00FF),
                        unfocusedBorderColor = Color(0x80FFFFFF),
                        cursorColor = Color(0xFFBD00FF)
                    )
                )
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Имя пользователя", color = Color(0xCCFFFFFF)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xCCFFFFFF),
                        focusedBorderColor = Color(0xFFBD00FF),
                        unfocusedBorderColor = Color(0x80FFFFFF),
                        cursorColor = Color(0xFFBD00FF)
                    )
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль", color = Color(0xCCFFFFFF)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xCCFFFFFF),
                        focusedBorderColor = Color(0xFFBD00FF),
                        unfocusedBorderColor = Color(0x80FFFFFF),
                        cursorColor = Color(0xFFBD00FF)
                    )
                )
                
                if (error.isNotEmpty()) {
                    Text(error, color = Color(0xFFFF5555))
                }
                
                Button(
                    onClick = {
                        isLoading = true
                        error = ""
                        scope.launch {
                            ApiClient.register(email, username, password).fold(
                                onSuccess = { onRegisterSuccess() },
                                onFailure = { 
                                    error = "Ошибка регистрации"
                                    isLoading = false
                                }
                            )
                        }
                    },
                    enabled = !isLoading && email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBD00FF),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isLoading) "Загрузка..." else "Зарегистрироваться", fontSize = 16.sp)
                }
                
                TextButton(onClick = onBackToLogin) {
                    Text("Назад к входу", color = Color(0xFF00F3FF))
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    
    if (selectedChatId != null) {
        ChatDetailScreen(
            chatId = selectedChatId!!,
            onBack = { selectedChatId = null }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
        ) {
            // Верхняя панель
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0x30FFFFFF),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CatLover",
                        fontSize = 24.sp,
                        color = Color(0xFFBD00FF),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            
            // Контент
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ChatsListScreen(onChatClick = { chatId -> selectedChatId = chatId })
                    1 -> SearchUsersScreen()
                    2 -> ProfileScreen()
                }
            }
            
            // Нижняя панель навигации
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0x30FFFFFF),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomNavItem(
                        icon = "💬",
                        label = "Чаты",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    BottomNavItem(
                        icon = "🔍",
                        label = "Поиск",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    BottomNavItem(
                        icon = "👤",
                        label = "Профиль",
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatsListScreen(onChatClick: (String) -> Unit) {
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            ApiClient.getChats().fold(
                onSuccess = { 
                    chats = it
                    isLoading = false
                },
                onFailure = { isLoading = false }
            )
        }
    }
    
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Загрузка...", color = Color.White)
        }
    } else if (chats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Пока нет чатов", color = Color(0x80FFFFFF))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chats.size) { index ->
                val chat = chats[index]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0x20FFFFFF),
                    onClick = { onChatClick(chat.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFBD00FF), shape = MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                chat.title.firstOrNull()?.toString() ?: "?",
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        }
                        
                        Column {
                            Text(
                                chat.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                chat.lastMessage,
                                color = Color(0xAAFFFFFF),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchUsersScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                if (it.length >= 2) {
                    isLoading = true
                    scope.launch {
                        ApiClient.searchUsers(it).fold(
                            onSuccess = { 
                                users = it
                                isLoading = false
                            },
                            onFailure = { isLoading = false }
                        )
                    }
                } else {
                    users = emptyList()
                }
            },
            label = { Text("Поиск пользователей...", color = Color(0xCCFFFFFF)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xCCFFFFFF),
                focusedBorderColor = Color(0xFFBD00FF),
                unfocusedBorderColor = Color(0x80FFFFFF),
                cursorColor = Color(0xFFBD00FF)
            )
        )
        
        if (isLoading) {
            Text("Поиск...", color = Color(0x80FFFFFF), modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (users.isEmpty() && searchQuery.isEmpty()) {
            Text("Начните вводить для поиска...", color = Color(0x80FFFFFF), modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (users.isEmpty()) {
            Text("Пользователи не найдены", color = Color(0x80FFFFFF), modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn {
                items(users.size) { index ->
                    val user = users[index]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = Color(0x20FFFFFF),
                        onClick = { }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF00F3FF), shape = MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user.username.firstOrNull()?.toString() ?: "?",
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            }
                            
                            Column {
                                Text(
                                    user.username,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                if (user.status.isNotEmpty()) {
                                    Text(
                                        user.status,
                                        color = Color(0xAAFFFFFF),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            ApiClient.getProfile().fold(
                onSuccess = { 
                    profile = it
                    isLoading = false
                },
                onFailure = { isLoading = false }
            )
        }
    }
    
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Загрузка...", color = Color.White)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFBD00FF), shape = MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile?.username?.firstOrNull()?.toString() ?: "П",
                    color = Color.White,
                    fontSize = 48.sp
                )
            }
            
            Text(
                profile?.username ?: "Пользователь",
                fontSize = 24.sp,
                color = Color.White
            )
            
            Text(
                profile?.email ?: "",
                fontSize = 16.sp,
                color = Color(0xAAFFFFFF)
            )
            
            if (!profile?.bio.isNullOrEmpty()) {
                Text(
                    profile?.bio ?: "",
                    fontSize = 14.sp,
                    color = Color(0x80FFFFFF)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Информация о функциях
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color(0x20FFFFFF)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Доступные функции:",
                        fontSize = 16.sp,
                        color = Color(0xFFBD00FF),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    FeatureItem("💬", "Текстовые сообщения")
                    FeatureItem("📞", "Голосовые звонки (WebRTC)")
                    FeatureItem("📹", "Видео звонки (WebRTC)")
                    FeatureItem("👥", "Групповые чаты")
                    FeatureItem("🔒", "E2E шифрование")
                    FeatureItem("📎", "Отправка файлов")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBD00FF)
                )
            ) {
                Text("Редактировать профиль")
            }
            
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x30FF5555)
                )
            ) {
                Text("Выйти", color = Color(0xFFFF5555))
            }
        }
    }
}

@Composable
fun ChatDetailScreen(chatId: String, onBack: () -> Unit) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Загрузка сообщений
    LaunchedEffect(chatId) {
        scope.launch {
            ApiClient.getMessages(chatId).fold(
                onSuccess = { 
                    messages = it
                    isLoading = false
                },
                onFailure = { isLoading = false }
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // Верхняя панель с кнопкой назад
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0x30FFFFFF),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
                Text(
                    "Чат",
                    fontSize = 20.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Кнопки звонков
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { /* TODO: Голосовой звонок */ },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x30FFFFFF), shape = RoundedCornerShape(20.dp))
                    ) {
                        Text("📞", fontSize = 20.sp)
                    }
                    
                    IconButton(
                        onClick = { /* TODO: Видео звонок */ },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x30FFFFFF), shape = RoundedCornerShape(20.dp))
                    ) {
                        Text("📹", fontSize = 20.sp)
                    }
                }
            }
        }
        
        // Список сообщений
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка сообщений...", color = Color(0x80FFFFFF))
                }
            } else if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет сообщений", color = Color(0x80FFFFFF))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.size) { index ->
                        val message = messages[index]
                        MessageBubble(message)
                    }
                }
            }
        }
        
        // Поле ввода сообщения
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0x30FFFFFF),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Сообщение...", color = Color(0x80FFFFFF)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xCCFFFFFF),
                        focusedBorderColor = Color(0xFFBD00FF),
                        unfocusedBorderColor = Color(0x60FFFFFF),
                        cursorColor = Color(0xFFBD00FF)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank() && !isSending) {
                            isSending = true
                            val textToSend = messageText
                            messageText = ""
                            scope.launch {
                                ApiClient.sendMessage(chatId, textToSend).fold(
                                    onSuccess = { newMessage ->
                                        messages = messages + newMessage
                                        isSending = false
                                    },
                                    onFailure = {
                                        messageText = textToSend
                                        isSending = false
                                    }
                                )
                            }
                        }
                    },
                    enabled = messageText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (messageText.isNotBlank()) Color(0xFFBD00FF) else Color(0x40FFFFFF),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Отправить",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    // Получаем ID текущего пользователя из токена (упрощенно)
    val isOwnMessage = false // TODO: определить по sender_id
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 16.dp
            ),
            color = if (isOwnMessage) Color(0xFFBD00FF) else Color(0x30FFFFFF),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    message.body,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    formatTime(message.createdAt),
                    color = Color(0x80FFFFFF),
                    fontSize = 11.sp
                )
            }
        }
    }
}

fun formatTime(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(timestamp)
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        timestamp.substring(11, 16)
    }
}

@Composable
fun FeatureItem(icon: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Text(
            text,
            color = Color(0xCCFFFFFF),
            fontSize = 14.sp
        )
    }
}

@Composable
fun BottomNavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            label,
            fontSize = 12.sp,
            color = if (selected) Color(0xFFBD00FF) else Color(0x80FFFFFF)
        )
    }
}
