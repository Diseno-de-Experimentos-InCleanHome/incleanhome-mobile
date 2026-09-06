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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.messaging.data.Conversation
import com.incleanhome.mobile.messaging.data.Message
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.formatDateTime

@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onBack: () -> Unit,
    onConversationClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(stringResource(R.string.title_conversations), onBack)
        Spacer(Modifier.height(12.dp))
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_refresh))
        }
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> LoadingState()

            state.errorMessage != null -> ErrorRetryState(state.errorMessage.orEmpty(), viewModel::refresh)

            state.conversations.isEmpty() -> EmptyState(stringResource(R.string.empty_conversations))

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
        ScreenHeader(otherUserName, onBack)
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_refresh_conversation))
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading && state.messages.isEmpty() -> Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) { LoadingState() }

            state.errorMessage != null && state.messages.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErrorRetryState(state.errorMessage.orEmpty(), viewModel::refresh)
            }

            else -> {
                if (state.messages.isEmpty()) {
                    EmptyState(stringResource(R.string.empty_messages), modifier = Modifier.weight(1f))
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
            label = { Text(stringResource(R.string.label_message)) },
            supportingText = { Text("${state.draft.length}/${ChatViewModel.MAX_CONTENT_LENGTH}") },
            enabled = !state.isSending,
            maxLines = 4
        )
        PrimaryButton(
            text = stringResource(R.string.action_send),
            onClick = viewModel::send,
            enabled = state.draft.isNotBlank(),
            loading = state.isSending
        )
    }
}

@Composable
private fun ConversationCard(conversation: Conversation, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().semantics { role = Role.Button }.clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(conversation.userName, style = MaterialTheme.typography.titleMedium)
                if (conversation.unreadCount > 0) {
                    Text(
                        pluralStringResource(R.plurals.unread_messages, conversation.unreadCount, conversation.unreadCount),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(conversation.lastMessage, maxLines = 2)
            conversation.lastMessageAt?.let {
                Text(formatDateTime(it), style = MaterialTheme.typography.bodySmall)
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
                    Text(formatDateTime(it), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
