package com.incleanhome.mobile.booking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingRepository
import com.incleanhome.mobile.booking.data.BookingResult
import com.incleanhome.mobile.booking.data.CreateBookingRequest
import com.incleanhome.mobile.booking.domain.BookingEstimate
import com.incleanhome.mobile.booking.domain.BookingEstimator
import com.incleanhome.mobile.experiments.Assignment
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.Experimentation
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateBookingUiState(
    val isLoadingWorker: Boolean = true,
    val isSubmitting: Boolean = false,
    val worker: Worker? = null,
    val selectedService: String = "",
    val date: String = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString(),
    val startTime: String = "09:00",
    val endTime: String = "11:00",
    val address: String = "",
    val notes: String = "",
    val errorMessage: String? = null,
    val createdBooking: Booking? = null,
    /** US41: true when the estimated amount is shown before confirming (treatment of EC-BLQ06). */
    val showEstimate: Boolean = false,
    val estimate: BookingEstimate? = null,
    /** Amount shown before confirming, kept to verify it against the registered amount (M28). */
    val confirmedEstimate: BigDecimal? = null
)

/** Where the client came from: the plan distinguishes filter search and suggestions (US33). */
object BookingOrigin {
    const val FILTERS = "filters"
    const val SUGGESTION = "suggestion"
    const val DIRECT = "direct"
}

class CreateBookingViewModel(
    private val workerId: Int,
    private val origin: String = BookingOrigin.DIRECT,
    private val searchId: String? = null,
    private val workerRepository: WorkerRepository = WorkerRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val experiments: ExperimentService? = Experimentation.experimentsOrNull(),
    private val tracker: EventTracker? = Experimentation.trackerOrNull()
) : ViewModel() {
    /** One identifier per opened form: the plan counts forms once per formId. */
    val formId: String = UUID.randomUUID().toString()

    private val estimateAssignment: Assignment =
        experiments?.recordExposure(Experiment.BOOKING_ESTIMATE, "booking_form")
            ?: Assignment.inactive(Experiment.BOOKING_ESTIMATE)

    private val _uiState = MutableStateFlow(CreateBookingUiState(showEstimate = estimateAssignment.featureActive))
    val uiState: StateFlow<CreateBookingUiState> = _uiState.asStateFlow()

    init { loadWorker() }

    fun loadWorker() {
        _uiState.update { it.copy(isLoadingWorker = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = workerRepository.getWorker(workerId)) {
                is WorkerResult.Success -> {
                    _uiState.update {
                        recalculate(
                            it.copy(
                                isLoadingWorker = false,
                                worker = result.data,
                                selectedService = it.selectedService.takeIf(result.data.serviceTypes::contains)
                                    ?: result.data.serviceTypes.firstOrNull().orEmpty()
                            )
                        )
                    }
                    tracker?.track(
                        TrackingPlan.BOOKING_FORM_OPENED,
                        mapOf("formId" to formId, "searchId" to searchId, "workerId" to workerId, "origin" to origin),
                        operationKey = formId,
                        assignment = estimateAssignment
                    )
                    trackEstimateViewed(_uiState.value)
                }
                is WorkerResult.Error -> _uiState.update {
                    it.copy(isLoadingWorker = false, worker = null, errorMessage = result.message)
                }
            }
        }
    }

    fun selectService(value: String) = edit { it.copy(selectedService = value) }
    fun updateDate(value: String) = edit { it.copy(date = value.take(10)) }
    fun updateStartTime(value: String) = edit { it.copy(startTime = value.take(5)) }
    fun updateEndTime(value: String) = edit { it.copy(endTime = value.take(5)) }
    fun updateAddress(value: String) = edit { it.copy(address = value) }
    fun updateNotes(value: String) = edit { it.copy(notes = value) }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting || state.createdBooking != null) return
        val request = buildRequest(state) ?: return
        val shownAmount = state.estimate?.totalAmount?.takeIf { state.showEstimate }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, confirmedEstimate = shownAmount) }
        viewModelScope.launch {
            when (val result = bookingRepository.create(request)) {
                is BookingResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, createdBooking = result.data) }
                    val estimated = state.worker?.let { BookingEstimator.total(it.hourlyRate, request.hours) }
                    tracker?.track(
                        TrackingPlan.BOOKING_REQUEST_CREATED,
                        mapOf(
                            "bookingId" to result.data.id,
                            "formId" to formId,
                            "searchId" to searchId,
                            "origin" to origin,
                            "estimatedAmount" to (shownAmount ?: estimated)?.toDouble(),
                            "registeredAmount" to result.data.totalAmount.toDouble(),
                            "amountMatches" to (shownAmount ?: estimated)?.let {
                                BookingEstimator.matches(it, result.data.totalAmount)
                            },
                            "capturedBy" to "client"
                        ),
                        operationKey = "booking:${result.data.id}",
                        assignment = estimateAssignment
                    )
                }
                is BookingResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                    tracker?.track(
                        TrackingPlan.BOOKING_REQUEST_FAILED,
                        mapOf("formId" to formId, "errorType" to result.errorType),
                        assignment = estimateAssignment
                    )
                }
            }
        }
    }

    private fun recalculate(state: CreateBookingUiState): CreateBookingUiState {
        val worker = state.worker ?: return state.copy(estimate = null)
        val start = parseTime(state.startTime)
        val end = parseTime(state.endTime)
        val estimate = if (start != null && end != null) BookingEstimator.estimate(worker.hourlyRate, start, end) else null
        return state.copy(estimate = estimate)
    }

    private fun trackEstimateViewed(state: CreateBookingUiState) {
        val estimate = state.estimate ?: return
        if (!state.showEstimate) return
        tracker?.track(
            TrackingPlan.BOOKING_ESTIMATE_VIEWED,
            mapOf("formId" to formId, "estimatedAmount" to estimate.totalAmount.toDouble()),
            operationKey = formId,
            assignment = estimateAssignment
        )
    }

    private fun buildRequest(state: CreateBookingUiState): CreateBookingRequest? {
        if (state.selectedService !in (state.worker?.serviceTypes ?: emptyList())) {
            showValidation("Selecciona uno de los servicios ofrecidos por el trabajador.")
            return null
        }
        val date = try { LocalDate.parse(state.date, DATE_FORMAT) } catch (_: DateTimeParseException) { null }
        if (date == null || date.isBefore(LocalDate.now(ZoneOffset.UTC))) {
            showValidation("Ingresa una fecha válida en formato yyyy-MM-dd que no esté en el pasado.")
            return null
        }
        val start = parseTime(state.startTime)
        val end = parseTime(state.endTime)
        val hours = if (start != null && end != null) BookingEstimator.hoursBetween(start, end) else null
        if (hours == null) {
            showValidation("Usa horas HH:mm y una hora de fin posterior a la de inicio.")
            return null
        }
        val address = state.address.trim()
        if (address.length !in 5..250) {
            showValidation("La dirección debe tener entre 5 y 250 caracteres.")
            return null
        }
        if (state.notes.trim().length > 1000) {
            showValidation("Las notas no pueden superar 1000 caracteres.")
            return null
        }
        return CreateBookingRequest(
            workerId = workerId,
            serviceType = state.selectedService,
            date = state.date,
            startTime = state.startTime,
            endTime = state.endTime,
            hours = hours,
            address = address,
            notes = state.notes.trim().takeIf(String::isNotEmpty)
        )
    }

    private fun parseTime(value: String): LocalTime? = try {
        LocalTime.parse(value, TIME_FORMAT)
    } catch (_: DateTimeParseException) { null }

    private fun showValidation(message: String) = _uiState.update { it.copy(errorMessage = message) }
    private fun edit(transform: (CreateBookingUiState) -> CreateBookingUiState) {
        _uiState.update { recalculate(transform(it).copy(errorMessage = null)) }
        trackEstimateViewed(_uiState.value)
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

        fun Factory(
            workerId: Int,
            origin: String = BookingOrigin.DIRECT,
            searchId: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CreateBookingViewModel::class.java)) {
                    return CreateBookingViewModel(workerId, origin, searchId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
