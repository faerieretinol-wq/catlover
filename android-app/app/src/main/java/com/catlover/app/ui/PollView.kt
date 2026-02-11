package com.catlover.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catlover.app.network.ApiClient
import com.catlover.app.network.PollResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PollView(pollId: String, api: ApiClient) {
    var poll by remember { mutableStateOf<PollResponse?>(null) }
    val scope = rememberCoroutineScope()
    var votingOptionId by remember { mutableStateOf<String?>(null) }

    fun loadPoll() {
        scope.launch {
            try {
                poll = withContext(Dispatchers.IO) { api.getPoll(pollId) }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(pollId) { loadPoll() }

    poll?.let { p ->
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = p.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (p.anonymous) {
                    Text("Anonymous poll", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Spacer(modifier = Modifier.height(12.dp))

                p.options.forEach { option ->
                    val percentage = if (p.totalVotes > 0) (option.voteCount.toFloat() / p.totalVotes.toFloat()) else 0f
                    val animatedProgress by animateFloatAsState(targetValue = percentage, label = "pollProgress")
                    val isVoted = option.votedByMe == 1

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                if (votingOptionId == null) {
                                    votingOptionId = option.id
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { api.votePoll(p.id, listOf(option.id)) }
                                            loadPoll()
                                        } catch (e: Exception) {}
                                        finally { votingOptionId = null }
                                    }
                                }
                            }
                    ) {
                        // Progress Bar Fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .matchParentSize()
                                .background(
                                    if (isVoted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else Color.White.copy(alpha = 0.1f)
                                )
                        )

                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option.optionText,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (votingOptionId == option.id) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "${(percentage * 100).toInt()}%",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${p.totalVotes} votes",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
