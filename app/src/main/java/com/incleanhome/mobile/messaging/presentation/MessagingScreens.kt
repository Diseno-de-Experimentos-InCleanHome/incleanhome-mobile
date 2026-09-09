package com.incleanhome.mobile.messaging.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.messaging.data.Conversation
import com.incleanhome.mobile.messaging.data.Message
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.components.RefreshButton
import com.incleanhome.mobile.ui.format.formatDateTime
import com.incleanhome.mobile.ui.format.localizedUiMessage
import com.incleanhome.mobile.ui.theme.Border
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color

@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onBack: () -> Unit,
    onConversationClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    ScreenBackground(modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader(stringResource(R.string.title_conversations), onBack, trailingContent = { RefreshButton(onClick = viewModel::refresh, enabled = !state.isLoading, showLabel = false) })
            Text(
                text = stringResource(R.string.conversations_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> LoadingState(Modifier.weight(1f))
                state.errorMessage != null -> ErrorRetryState(
                    state.errorMessage.orEmpty(),
                    viewModel::refresh,
                    Modifier.weight(1f)
                )
                state.conversations.isEmpty() -> EmptyState(
                    stringResource(R.string.empty_conversations),
                    Modifier.weight(1f)
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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

    ScreenBackground(modifier) {
        Column(modifier = Modifier.fillMaxSize().imePadding().padding(16.dp)) {
            ScreenHeader(otherUserName, onBack, trailingContent = { RefreshButton(onClick = viewModel::refresh, enabled = !state.isLoading, text = stringResource(R.string.action_refresh_conversation), showLabel = false) })
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
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
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
                Text(
                    text = localizedUiMessage(it),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
            }
            InCleanHomeCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InCleanHomeTextField(
                        value = state.draft,
                        onValueChange = viewModel::updateDraft,
                        label = stringResource(R.string.message_placeholder),
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            Text(
                                stringResource(
                                    R.string.message_character_count,
                                    state.draft.length,
                                    ChatViewModel.MAX_CONTENT_LENGTH
                                )
                            )
                        },
                        enabled = !state.isSending,
                        singleLine = false,
                        minLines = 1,
                        maxLines = 3
                    )
                    PrimaryButton(
                        text = stringResource(R.string.action_send),
                        onClick = viewModel::send,
                        modifier = Modifier.weight(0.42f),
                        enabled = state.draft.isNotBlank() && !state.isSending,
                        loading = state.isSending
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(conversation: Conversation, onClick: () -> Unit) {
    InCleanHomeCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    conversation.userName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = Navy
                )
                conversation.lastMessageAt?.let {
                    Text(
                        formatDateTime(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (conversation.unreadCount > 0) {
                    val unreadDescription = pluralStringResource(
                        R.plurals.unread_messages,
                        conversation.unreadCount,
                        conversation.unreadCount
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 28.dp, minHeight = 28.dp)
                            .background(PrimaryGreen, CircleShape)
                            .semantics { contentDescription = unreadDescription }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            conversation.unreadCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Text(
                conversation.lastMessage,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            color = if (isMine) PrimaryGreen else MaterialTheme.colorScheme.surface,
            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            border = if (isMine) null else BorderStroke(1.dp, Border)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
                message.createdAt?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatDateTime(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.78f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
