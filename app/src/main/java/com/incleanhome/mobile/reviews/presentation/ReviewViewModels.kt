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
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.Experimentation
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.reviews.domain.ReviewHistory
import com.incleanhome.mobile.reviews.domain.ReviewSource
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
    private val source: String = ReviewSource.DIRECT,
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val reviewsRepository: ReviewsRepository = ReviewsRepository(),
    private val experiments: ExperimentService? = Experimentation.experimentsOrNull(),
    private val tracker: EventTracker? = Experimentation.trackerOrNull()
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
        // The API requires the comment since US34 (400 "El comentario es obligatorio").
        if (state.comment.isBlank()) {
            _uiState.update { it.copy(errorMessage = COMMENT_REQUIRED_MESSAGE) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val request = CreateReviewRequest(
                bookingId = booking.id,
                workerId = booking.workerId,
                rating = state.rating,
                comment = state.comment.trim(),
                source = source
            )
            when (val result = reviewsRepository.createReview(request)) {
                is ReviewResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, createdReview = result.data) }
                    // Measurement of US35/EC-BLQ03: the origin tells direct ratings from reminders.
                    tracker?.track(
                        TrackingPlan.REVIEW_SUBMITTED,
                        mapOf(
                            "bookingId" to booking.id,
                            "rating" to result.data.rating,
                            "source" to source,
                            "isVerified" to ReviewHistory.isVerified(result.data),
                            "capturedBy" to "client"
                        ),
                        operationKey = "review:${booking.id}",
                        assignment = experiments?.assignment(Experiment.REVIEW_REMINDER)
                    )
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
        const val COMMENT_REQUIRED_MESSAGE = "El comentario es obligatorio."

        fun Factory(bookingId: Int, source: String = ReviewSource.DIRECT): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CreateReviewViewModel::class.java)) {
                    return CreateReviewViewModel(bookingId, source) as T
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
