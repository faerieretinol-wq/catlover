package com.catlover.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catlover.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWizardScreen(
    onLoggedOut: () -> Unit, 
    onProfile: () -> Unit,
    initialShareId: String? = null
) {
    GlassBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_settings), color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onProfile) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "App Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Full customization is coming in the next update. For now, enjoy the new Glass UI!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onProfile,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to Profile")
                        }
                    }
                }
            }
        }
    }
}
