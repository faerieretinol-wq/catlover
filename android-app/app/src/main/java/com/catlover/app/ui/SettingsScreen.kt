package com.catlover.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Divider as HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.app.R
import com.catlover.app.data.TokenStore
import com.catlover.app.network.ApiClient
import com.catlover.app.network.AuthStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onBlockedClick: () -> Unit, onMyChannelsClick: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient(TokenStore(ctx)) }
    
    var settingsJson by remember { mutableStateOf<JsonObject?>(null) }
    var authStatus by remember { mutableStateOf<AuthStatusResponse?>(null) }
    var loading by remember { mutableStateOf(false) }

    // Security Dialog States
    var show2FADialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var qrCode by remember { mutableStateOf<String?>(null) }
    var backupCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var twoFAToken by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val res = withContext(Dispatchers.IO) { api.getSettings() }
            settingsJson = Json.parseToJsonElement(res.settings).jsonObject
            authStatus = withContext(Dispatchers.IO) { api.getAuthStatus() }
        } catch (e: Exception) {}
        finally { loading = false }
    }

    fun updateSetting(key: String, value: JsonElement) {
        val current = settingsJson?.toMutableMap() ?: mutableMapOf()
        current[key] = value
        val newObj = JsonObject(current)
        settingsJson = newObj
        scope.launch { try { withContext(Dispatchers.IO) { api.saveSettings(newObj.toString()) } } catch (e: Exception) {} }
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_settings), color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(8.dp))

                // SECTION: PRIVACY
                SettingsHeader(stringResource(R.string.section_privacy))
                GlassCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Column {
                        SettingNavigationRow(icon = Icons.Default.Campaign, title = "Мои каналы", onClick = onMyChannelsClick)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 56.dp))
                        SettingNavigationRow(icon = Icons.Default.Block, title = stringResource(R.string.title_blocked), onClick = onBlockedClick)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 56.dp))
                        val privacyLevel = settingsJson?.get("privacyLevel")?.jsonPrimitive?.content ?: "public"
                        SettingRow(icon = Icons.Default.Lock, title = stringResource(R.string.label_privacy_level), value = privacyLevel.replaceFirstChar { it.uppercase() }) {
                            val next = when(privacyLevel) { "public" -> "private"; "private" -> "hidden"; "hidden" -> "anonymous"; else -> "public" }
                            updateSetting("privacyLevel", JsonPrimitive(next))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECTION: SECURITY (NEW)
                SettingsHeader("Security")
                GlassCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Column {
                        // 2FA Toggle
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Two-Factor Authentication", color = Color.White) },
                            supportingContent = { Text(if (authStatus?.twoFactorEnabled == true) "Enabled ✓" else "Disabled", color = Color.White.copy(alpha = 0.5f)) },
                            leadingContent = { Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                Switch(
                                    checked = authStatus?.twoFactorEnabled == true,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            scope.launch {
                                                try {
                                                    val res = withContext(Dispatchers.IO) { api.setup2FA() }
                                                    qrCode = res.qrCode; backupCodes = res.backupCodes; show2FADialog = true
                                                } catch (e: Exception) { Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                            }
                                        } else {
                                            // Handle disable logic
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 56.dp))
                        // Email Verification
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Email Verification", color = Color.White) },
                            supportingContent = { Text(if (authStatus?.emailVerified == true) "Verified ✓" else "Not verified", color = Color.White.copy(alpha = 0.5f)) },
                            leadingContent = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                if (authStatus?.emailVerified != true) {
                                    Button(onClick = {
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { api.sendEmailVerification() }
                                                showEmailDialog = true
                                                Toast.makeText(ctx, "Verification code sent!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) { Toast.makeText(ctx, "Failed to send code", Toast.LENGTH_SHORT).show() }
                                        }
                                    }) { Text("Verify") }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECTION: DEVELOPER
                SettingsHeader("Разработчик")
                GlassCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Column {
                        var showDbTest by remember { mutableStateOf(false) }
                        SettingNavigationRow(
                            icon = Icons.Default.DataObject,
                            title = "Тест базы данных",
                            onClick = { showDbTest = true }
                        )
                        
                        if (showDbTest) {
                            DatabaseTestScreen(onBack = { showDbTest = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                                    // SECTION: APPEARANCE
                                    SettingsHeader(stringResource(R.string.section_appearance))
                                    GlassCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        Column {
                                            SettingToggle(icon = Icons.Default.Animation, title = stringResource(R.string.label_animations), checked = settingsJson?.get("animations")?.jsonPrimitive?.content != "none", onCheckedChange = { updateSetting("animations", JsonPrimitive(if (it) "full" else "none")) })
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(start = 56.dp))
                                            SettingNavigationRow(
                                                icon = Icons.Default.Share, 
                                                title = "Share Theme", 
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            // Сначала убедимся, что шеринг разрешен в настройках
                                                            updateSetting("shareableStyle", JsonPrimitive(true))
                                                            val res = withContext(Dispatchers.IO) { api.shareStyle() }
                                                            
                                                            // Копируем ссылку в буфер обмена
                                                            val clipboard = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            val clip = android.content.ClipData.newPlainText("CatLover Style", "Apply my theme: catlover://style/${res.shareId}")
                                                            clipboard.setPrimaryClip(clip)
                                                            
                                                            Toast.makeText(ctx, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(ctx, "Error sharing theme: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // 2FA Setup Dialog
    if (show2FADialog) {
        AlertDialog(
            onDismissRequest = { show2FADialog = false },
            title = { Text("Enable 2FA") },
            text = {
                Column {
                    Text("Scan this QR code or use the backup codes:")
                    Spacer(Modifier.height(16.dp))
                    Text("Backup codes (SAVE THESE!):", fontWeight = FontWeight.Bold)
                    backupCodes.forEach { Text(it, fontFamily = FontFamily.Monospace) }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = twoFAToken, onValueChange = { if(it.length <= 6) twoFAToken = it }, label = { Text("6-digit code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(enabled = twoFAToken.length == 6, onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { api.verify2FA(twoFAToken) }
                            authStatus = withContext(Dispatchers.IO) { api.getAuthStatus() }
                            show2FADialog = false; twoFAToken = ""
                            Toast.makeText(ctx, "2FA Enabled!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { Toast.makeText(ctx, "Invalid code", Toast.LENGTH_SHORT).show() }
                    }
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = { TextButton(onClick = { show2FADialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    // Email Verification Dialog
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text(stringResource(R.string.title_verify_email)) },
            text = {
                Column {
                    Text(stringResource(R.string.hint_enter_code))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = emailCode, onValueChange = { if(it.length <= 6) emailCode = it }, label = { Text(stringResource(R.string.label_verification_code)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(enabled = emailCode.length == 6, onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { api.verifyEmail(emailCode) }
                            authStatus = withContext(Dispatchers.IO) { api.getAuthStatus() }
                            showEmailDialog = false; emailCode = ""
                            Toast.makeText(ctx, ctx.getString(R.string.toast_email_verified), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { Toast.makeText(ctx, ctx.getString(R.string.toast_invalid_code), Toast.LENGTH_SHORT).show() }
                    }
                }) { Text(stringResource(R.string.action_verify)) }
            },
            dismissButton = { TextButton(onClick = { showEmailDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
fun SettingNavigationRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp)); Text(title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
}

@Composable
fun SettingToggle(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp)); Text(title, color = Color.White, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 15.sp); Text(value, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f))
    }
}
