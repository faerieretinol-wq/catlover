package com.catlover.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.catlover.app.services.NeuralLinkService
import com.catlover.app.ui.RegistrationScreen
import com.catlover.app.ui.LoginScreen
import com.catlover.app.ui.ChatListScreen
import com.catlover.app.ui.ChatDetailScreen
import com.catlover.app.ui.SearchScreen
import com.catlover.app.network.TokenStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Автоматический запуск сервиса при наличии токена
        val tokenStore = TokenStore(this)
        if (tokenStore.getAccessToken() != null) {
            startNeuralService()
        }

        setContent {
            var currentScreen by remember { mutableStateOf("login") }
            
            when (currentScreen) {
                "login" -> LoginScreen(
                    onLoginClick = { email, pass -> 
                        // Логика входа
                        startNeuralService()
                        currentScreen = "chat_list" 
                    },
                    onNavigateToRegister = { currentScreen = "register" }
                )
                "register" -> RegistrationScreen(
                    onRegisterClick = { email, user, pass -> 
                        currentScreen = "login" 
                    }
                )
                "chat_list" -> ChatListScreen(
                    onChatClick = { chatId -> currentScreen = "chat_detail" },
                    onNewChatClick = { currentScreen = "search" }
                )
                "chat_detail" -> ChatDetailScreen(
                    chatTitle = "Cyberfox",
                    messages = emptyList(),
                    onSendMessage = { /* Logic handled via SocketManager */ }
                )
                "search" -> SearchScreen(
                    onUserClick = { id, name -> currentScreen = "chat_detail" },
                    onSearch = { /* Search API */ },
                    searchResults = emptyList()
                )
            }
        }
    }

    private fun startNeuralService() {
        val intent = Intent(this, NeuralLinkService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
