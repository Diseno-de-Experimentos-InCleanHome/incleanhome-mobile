package com.incleanhome.mobile.worker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.worker.data.WorkerProfile
import com.incleanhome.mobile.worker.data.WorkerSelfRepository
import com.incleanhome.mobile.worker.data.WorkerSelfResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profile: WorkerProfile? = null
)

class WorkerProfileViewModel(
    private val repository: WorkerSelfRepository = WorkerSelfRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkerProfileUiState())
    val uiState: StateFlow<WorkerProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (_uiState.value.isLoading && _uiState.value.profile != null) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.getMyProfile()) {
                is WorkerSelfResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, profile = result.data)
                    }
                }

                is WorkerSelfResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            profile = null
                        )
                    }
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(WorkerProfileViewModel::class.java)) {
                    return WorkerProfileViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
