package com.catlover.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catlover.app.data.ProfileRepository
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseTestScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = remember { TokenStore(ctx) }
    val api = remember { ApiClient(tokens) }
    val repository = remember { ProfileRepository(ctx, api) }
    
    var log by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    
    fun addLog(msg: String) {
        log += "${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}: $msg\n"
        android.util.Log.d("DatabaseTest", msg)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тест базы данных") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Проверка работы Room Database", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        addLog("🔄 Загрузка профиля с сервера...")
                        try {
                            val profile = repository.getProfile("me", forceRefresh = true)
                            if (profile != null) {
                                addLog("✅ Профиль загружен: ${profile.username}")
                                addLog("   ID: ${profile.userId}")
                                addLog("   Bio: ${profile.bio ?: "нет"}")
                            } else {
                                addLog("❌ Профиль не загружен")
                            }
                        } catch (e: Exception) {
                            addLog("❌ Ошибка: ${e.message}")
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("1. Загрузить профиль с сервера")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        addLog("📦 Загрузка профиля из кэша...")
                        try {
                            val profile = repository.getProfile("me", forceRefresh = false)
                            if (profile != null) {
                                addLog("✅ Профиль из кэша: ${profile.username}")
                            } else {
                                addLog("⚠️ Кэш пуст")
                            }
                        } catch (e: Exception) {
                            addLog("❌ Ошибка: ${e.message}")
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("2. Загрузить из кэша")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        addLog("🔍 Проверка кэша...")
                        try {
                            val profile = repository.getProfile("me", forceRefresh = false)
                            if (profile != null) {
                                addLog("✅ В кэше найден профиль:")
                                addLog("   Username: ${profile.username}")
                            } else {
                                addLog("⚠️ Кэш пуст")
                            }
                        } catch (e: Exception) {
                            addLog("❌ Ошибка: ${e.message}")
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("3. Проверить БД напрямую")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        addLog("🗑️ Очистка кэша...")
                        try {
                            repository.clearCache()
                            addLog("✅ Кэш очищен")
                        } catch (e: Exception) {
                            addLog("❌ Ошибка: ${e.message}")
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("4. Очистить кэш")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (loading) {
                CircularProgressIndicator()
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Лог:", style = MaterialTheme.typography.titleSmall)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Text(
                    text = log.ifEmpty { "Нажмите кнопки для тестирования..." },
                    modifier = Modifier
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
