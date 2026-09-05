package com.incleanhome.mobile.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.search.data.AvailabilitySlot
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerDetailUiState(
    val isWorkerLoading: Boolean = true,
    val workerErrorMessage: String? = null,
    val worker: Worker? = null,
    val isAvailabilityLoading: Boolean = true,
    val availabilityErrorMessage: String? = null,
    val availability: List<AvailabilitySlot> = emptyList()
)

class WorkerDetailViewModel(
    private val workerId: Int,
    private val repository: WorkerRepository = WorkerRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkerDetailUiState())
    val uiState: StateFlow<WorkerDetailUiState> = _uiState.asStateFlow()

    init {
        loadWorker()
    }

    fun loadWorker() {
        _uiState.update {
            it.copy(
                isWorkerLoading = true,
                workerErrorMessage = null,
                isAvailabilityLoading = true,
                availabilityErrorMessage = null
            )
        }

        viewModelScope.launch {
            val workerRequest = async { repository.getWorker(workerId) }
            val availabilityRequest = async { repository.getAvailability(workerId) }

            when (val result = workerRequest.await()) {
                is WorkerResult.Success -> {
                    _uiState.update {
                        it.copy(isWorkerLoading = false, worker = result.data)
                    }
                }

                is WorkerResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isWorkerLoading = false,
                            worker = null,
                            workerErrorMessage = result.message
                        )
                    }
                }
            }

            when (val result = availabilityRequest.await()) {
                is WorkerResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAvailabilityLoading = false,
                            availability = result.data
                        )
                    }
                }

                is WorkerResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isAvailabilityLoading = false,
                            availability = emptyList(),
                            availabilityErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun Factory(workerId: Int): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(WorkerDetailViewModel::class.java)) {
                        return WorkerDetailViewModel(workerId = workerId) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
