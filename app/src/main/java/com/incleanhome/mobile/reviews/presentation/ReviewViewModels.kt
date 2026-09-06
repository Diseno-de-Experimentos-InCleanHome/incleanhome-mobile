package com.incleanhome.mobile.reviews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingRepository
import com.incleanhome.mobile.booking.data.BookingResult
import com.incleanhome.mobile.booking.data.BookingStatus
import com.incleanhome.mobile.reviews.data.CreateReviewRequest
import com.incleanhome.mobile.reviews.data.Review
import com.incleanhome.mobile.reviews.data.ReviewResult
import com.incleanhome.mobile.reviews.data.ReviewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateReviewUiState(
    val isLoadingBooking: Boolean = true,
    val isSubmitting: Boolean = false,
    val booking: Booking? = null,
    val rating: Int = 0,
    val comment: String = "",
    val createdReview: Review? = null,
    val errorMessage: String? = null
)

class CreateReviewViewModel(
    private val bookingId: Int,
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val reviewsRepository: ReviewsRepository = ReviewsRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateReviewUiState())
    val uiState: StateFlow<CreateReviewUiState> = _uiState.asStateFlow()

    init { loadBooking() }

    fun loadBooking() {
        _uiState.update { it.copy(isLoadingBooking = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = bookingRepository.getMine()) {
                is BookingResult.Success -> {
                    val booking = result.data.firstOrNull { it.id == bookingId }
                    val validationError = when {
                        booking == null -> "No se encontró la reserva."
                        booking.status != BookingStatus.COMPLETED -> "Solo se pueden calificar reservas completadas."
                        booking.hasReview -> "Esta reserva ya tiene una reseña."
                        else -> null
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingBooking = false,
                            booking = booking?.takeIf { validationError == null },
                            errorMessage = validationError
                        )
                    }
                }
                is BookingResult.Error -> _uiState.update {
                    it.copy(isLoadingBooking = false, errorMessage = result.message)
                }
            }
        }
    }

    fun selectRating(rating: Int) {
        if (rating in MIN_RATING..MAX_RATING) {
            _uiState.update { it.copy(rating = rating, errorMessage = null) }
        }
    }

    fun updateComment(value: String) {
        _uiState.update { it.copy(comment = value.take(MAX_COMMENT_LENGTH), errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val booking = state.booking ?: return
        if (state.isSubmitting || state.createdReview != null) return
        if (state.rating !in MIN_RATING..MAX_RATING) {
            _uiState.update { it.copy(errorMessage = "Selecciona una calificación entre 1 y 5.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val request = CreateReviewRequest(
                bookingId = booking.id,
                workerId = booking.workerId,
                rating = state.rating,
                comment = state.comment.trim().takeIf(String::isNotEmpty)
            )
            when (val result = reviewsRepository.createReview(request)) {
                is ReviewResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, createdReview = result.data)
                }
                is ReviewResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        const val MIN_RATING = 1
        const val MAX_RATING = 5
        const val MAX_COMMENT_LENGTH = 1000

        fun Factory(bookingId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CreateReviewViewModel::class.java)) {
                    return CreateReviewViewModel(bookingId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

data class WorkerReviewsUiState(
    val isLoading: Boolean = true,
    val reviews: List<Review> = emptyList(),
    val errorMessage: String? = null
)

class WorkerReviewsViewModel(
    private val workerId: Int,
    private val repository: ReviewsRepository = ReviewsRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkerReviewsUiState())
    val uiState: StateFlow<WorkerReviewsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getWorkerReviews(workerId)) {
                is ReviewResult.Success -> _uiState.update {
                    it.copy(isLoading = false, reviews = result.data)
                }
                is ReviewResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun Factory(workerId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(WorkerReviewsViewModel::class.java)) {
                    return WorkerReviewsViewModel(workerId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
