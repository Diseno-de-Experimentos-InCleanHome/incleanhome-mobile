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
import com.incleanhome.mobile.reviews.data.WorkerReviewSummary
import com.incleanhome.mobile.experiments.Assignment
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.Experimentation
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.reviews.domain.ReviewHistory
import com.incleanhome.mobile.reviews.domain.ReviewSort
import java.util.UUID
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
    val reviews: List<Review> = emptyList(),
    /** US34 presentation (treatment of EC-BLQ02); the control keeps the current review list. */
    val verifiedReviewsEnabled: Boolean = false,
    /** GET reviews/worker/{id}/summary, loaded only for the treatment; null if it could not be read. */
    val reviewSummary: WorkerReviewSummary? = null,
    val reviewSort: ReviewSort = ReviewSort.NEWEST,
    val reviewServiceFilter: String? = null
) {
    val reviewHistory: ReviewHistory get() = ReviewHistory.from(reviews, reviewSummary)
    val arrangedReviews: List<Review>
        get() = ReviewHistory.arrange(reviewHistory.verifiedReviews, reviewSort, reviewServiceFilter)
}

class WorkerDetailViewModel(
    private val workerId: Int,
    private val source: String = "direct",
    private val searchId: String? = null,
    /** District of the service when the client came from a suggestion: adds the approximate distance. */
    private val clientDistrict: String? = null,
    private val repository: WorkerRepository = WorkerRepository(),
    private val reviewsRepository: ReviewsRepository = ReviewsRepository(),
    private val experiments: ExperimentService? = Experimentation.experimentsOrNull(),
    private val tracker: EventTracker? = Experimentation.trackerOrNull()
) : ViewModel() {
    private val viewId = UUID.randomUUID().toString()
    private val reviewsAssignment: Assignment =
        experiments?.recordExposure(Experiment.VERIFIED_REVIEWS, "worker_profile")
            ?: Assignment.inactive(Experiment.VERIFIED_REVIEWS)

    private val _uiState = MutableStateFlow(WorkerDetailUiState(verifiedReviewsEnabled = reviewsAssignment.featureActive))
    val uiState: StateFlow<WorkerDetailUiState> = _uiState.asStateFlow()

    init {
        loadWorker()
    }

    fun sortReviews(sort: ReviewSort) = _uiState.update { it.copy(reviewSort = sort) }

    fun filterReviews(serviceType: String?) = _uiState.update { it.copy(reviewServiceFilter = serviceType) }

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
            val workerRequest = async { repository.getWorker(workerId, clientDistrict) }
            val availabilityRequest = async { repository.getAvailability(workerId) }
            val reviewsRequest = async { reviewsRepository.getWorkerReviews(workerId) }
            val summaryRequest = if (reviewsAssignment.featureActive) {
                async { reviewsRepository.getWorkerReviewSummary(workerId) }
            } else {
                null
            }

            when (val result = workerRequest.await()) {
                is WorkerResult.Success -> {
                    _uiState.update {
                        it.copy(isWorkerLoading = false, worker = result.data)
                    }
                    tracker?.track(
                        TrackingPlan.WORKER_PROFILE_VIEWED,
                        mapOf("workerId" to workerId, "source" to source, "searchId" to searchId),
                        operationKey = viewId,
                        assignment = reviewsAssignment
                    )
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

            // Without the summary the section keeps the local figures and hides completed services.
            val summary = (summaryRequest?.await() as? ReviewResult.Success)?.data

            when (val result = reviewsRequest.await()) {
                is ReviewResult.Success -> {
                    _uiState.update { it.copy(isReviewsLoading = false, reviews = result.data, reviewSummary = summary) }
                    tracker?.track(
                        TrackingPlan.WORKER_REVIEWS_VIEWED,
                        mapOf(
                            "workerId" to workerId,
                            "reviewCount" to result.data.size,
                            "verifiedCount" to result.data.count(ReviewHistory::isVerified)
                        ),
                        operationKey = viewId,
                        assignment = reviewsAssignment
                    )
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
        fun Factory(
            workerId: Int,
            source: String = "direct",
            searchId: String? = null,
            clientDistrict: String? = null
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(WorkerDetailViewModel::class.java)) {
                        return WorkerDetailViewModel(
                            workerId = workerId,
                            source = source,
                            searchId = searchId,
                            clientDistrict = clientDistrict
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
