package com.incleanhome.mobile.messaging.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.messaging.data.Conversation
import com.incleanhome.mobile.messaging.data.Message
import com.incleanhome.mobile.messaging.data.MessagingRepository
import com.incleanhome.mobile.messaging.data.MessagingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val errorMessage: String? = null
)

class ConversationsViewModel(
    private val repository: MessagingRepository = MessagingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getConversations()) {
                is MessagingResult.Success -> _uiState.update {
                    it.copy(isLoading = false, conversations = result.data)
                }
                is MessagingResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ConversationsViewModel::class.java)) {
                    return ConversationsViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

data class ChatUiState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val errorMessage: String? = null
)

class ChatViewModel(
    private val otherUserId: Int,
    private val repository: MessagingRepository = MessagingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun updateDraft(value: String) {
        _uiState.update { it.copy(draft = value.take(MAX_CONTENT_LENGTH), errorMessage = null) }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getThread(otherUserId)) {
                is MessagingResult.Success -> _uiState.update {
                    it.copy(isLoading = false, messages = result.data.distinctBy(Message::id))
                }
                is MessagingResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun send() {
        val content = _uiState.value.draft
        if (content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El mensaje no puede estar vacío.") }
            return
        }
        if (_uiState.value.isSending) return

        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.sendMessage(otherUserId, content)) {
                is MessagingResult.Success -> _uiState.update { state ->
                    state.copy(
                        isSending = false,
                        draft = "",
                        messages = (state.messages + result.data).distinctBy(Message::id)
                    )
                }
                is MessagingResult.Error -> _uiState.update {
                    it.copy(isSending = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        const val MAX_CONTENT_LENGTH = 4000

        fun Factory(otherUserId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    return ChatViewModel(otherUserId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
