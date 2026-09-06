package com.incleanhome.mobile.messaging.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.messaging.data.Conversation
import com.incleanhome.mobile.messaging.data.Message
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onBack: () -> Unit,
    onConversationClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        MessagingHeader("Conversaciones", onBack)
        Spacer(Modifier.height(12.dp))
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar")
        }
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = viewModel::refresh) { Text("Reintentar") }
            }

            state.conversations.isEmpty() -> Text("Aún no tienes conversaciones.")

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.conversations, key = Conversation::userId) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentUserId: Int,
    otherUserName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        MessagingHeader(otherUserName, onBack)
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar conversación")
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading && state.messages.isEmpty() -> Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) { CircularProgressIndicator() }

            state.errorMessage != null && state.messages.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::refresh) { Text("Reintentar") }
            }

            else -> {
                if (state.messages.isEmpty()) {
                    Text(
                        "No hay mensajes en esta conversación.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages, key = Message::id) { message ->
                            MessageBubble(
                                message = message,
                                isMine = message.senderId == currentUserId
                            )
                        }
                    }
                }
            }
        }

        state.errorMessage?.takeIf { state.messages.isNotEmpty() }?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(6.dp))
        }
        OutlinedTextField(
            value = state.draft,
            onValueChange = viewModel::updateDraft,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mensaje") },
            supportingText = { Text("${state.draft.length}/${ChatViewModel.MAX_CONTENT_LENGTH}") },
            enabled = !state.isSending,
            maxLines = 4
        )
        Button(
            onClick = viewModel::send,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSending && state.draft.isNotBlank()
        ) {
            if (state.isSending) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text("Enviar")
        }
    }
}

@Composable
private fun ConversationCard(conversation: Conversation, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(conversation.userName, style = MaterialTheme.typography.titleMedium)
                if (conversation.unreadCount > 0) {
                    Text(
                        "${conversation.unreadCount} sin leer",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(conversation.lastMessage, maxLines = 2)
            conversation.lastMessageAt?.let {
                Text(formatTimestamp(it), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            color = if (isMine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = MaterialTheme.shapes.medium
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(message.content)
                message.createdAt?.let {
                    Text(formatTimestamp(it), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MessagingHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onBack) { Text("Volver") }
        Text(
            title,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

private fun formatTimestamp(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DISPLAY_DATE_TIME)
}.getOrDefault(value)

private val DISPLAY_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
