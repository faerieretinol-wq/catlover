package com.catlover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onUserClick: (String, String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<UserSummary>
) {
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { 
                            query = it
                            onSearch(it)
                        },
                        placeholder = { Text("SEARCH NETWORK...", color = NeonCyan.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = Color.Transparent,
                            textColor = Color.White
                        ),
                        singleLine = true
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBlack)
            )
        },
        containerColor = CyberBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(searchResults) { user ->
                UserSearchItem(user, onClick = { onUserClick(user.id, user.username) })
            }
        }
    }
}

@Composable
fun UserSearchItem(user: UserSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberGray)
            .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NeonPurple.copy(alpha = 0.2f))
                .border(1.dp, NeonPurple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(user.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(user.username, color = Color.White, fontWeight = FontWeight.Bold)
            Text(user.status ?: "Online", color = NeonCyan, fontSize = 10.sp)
        }
    }
}

data class UserSummary(
    val id: String,
    val username: String,
    val status: String? = null
)
