package com.catlover.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch

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
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { selectedTab = 1 }) {
                        Text("🔍", fontSize = 20.sp)
                    }
                    IconButton(onClick = { selectedTab = 2 }) {
                        Text("👤", fontSize = 20.sp)
                    }
                }
            }
        }
        
        // Контент
        when (selectedTab) {
            0 -> ChatsListScreen()
            1 -> SearchUsersScreen()
            2 -> ProfileScreen()
        }
    }
}

@Composable
fun ChatsListScreen() {
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
                    onClick = { }
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
