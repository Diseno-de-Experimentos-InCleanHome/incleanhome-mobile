package com.incleanhome.mobile.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.search.data.AvailabilitySlot
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import com.incleanhome.mobile.reviews.data.Review
import com.incleanhome.mobile.reviews.data.ReviewResult
import com.incleanhome.mobile.reviews.data.ReviewsRepository
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
    val availability: List<AvailabilitySlot> = emptyList(),
    val isReviewsLoading: Boolean = true,
    val reviewsErrorMessage: String? = null,
    val reviews: List<Review> = emptyList()
)

class WorkerDetailViewModel(
    private val workerId: Int,
    private val repository: WorkerRepository = WorkerRepository(),
    private val reviewsRepository: ReviewsRepository = ReviewsRepository()
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
                availabilityErrorMessage = null,
                isReviewsLoading = true,
                reviewsErrorMessage = null
            )
        }

        viewModelScope.launch {
            val workerRequest = async { repository.getWorker(workerId) }
            val availabilityRequest = async { repository.getAvailability(workerId) }
            val reviewsRequest = async { reviewsRepository.getWorkerReviews(workerId) }

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

            when (val result = reviewsRequest.await()) {
                is ReviewResult.Success -> _uiState.update {
                    it.copy(isReviewsLoading = false, reviews = result.data)
                }
                is ReviewResult.Error -> _uiState.update {
                    it.copy(
                        isReviewsLoading = false,
                        reviews = emptyList(),
                        reviewsErrorMessage = result.message
                    )
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
