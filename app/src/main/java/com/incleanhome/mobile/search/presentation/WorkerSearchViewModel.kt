package com.incleanhome.mobile.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerSearchUiState(
    val serviceType: String = "",
    val zone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val workers: List<Worker> = emptyList(),
    val hasSearched: Boolean = false
)

class WorkerSearchViewModel(
    private val repository: WorkerRepository = WorkerRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkerSearchUiState())
    val uiState: StateFlow<WorkerSearchUiState> = _uiState.asStateFlow()

    init {
        search()
    }

    fun onServiceTypeChange(serviceType: String) {
        _uiState.update { it.copy(serviceType = serviceType, errorMessage = null) }
    }

    fun onZoneChange(zone: String) {
        _uiState.update { it.copy(zone = zone, errorMessage = null) }
    }

    fun search() {
        val state = _uiState.value
        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.searchWorkers(state.serviceType, state.zone)) {
                is WorkerResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            workers = result.data,
                            hasSearched = true
                        )
                    }
                }

                is WorkerResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            workers = emptyList(),
                            hasSearched = true
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
                if (modelClass.isAssignableFrom(WorkerSearchViewModel::class.java)) {
                    return WorkerSearchViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
