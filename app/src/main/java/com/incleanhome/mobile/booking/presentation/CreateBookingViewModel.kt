package com.incleanhome.mobile.booking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingEstimateRequest
import com.incleanhome.mobile.booking.data.BookingEstimateResponse
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val date: String = BookingEstimator.todayInPeru().plusDays(1).toString(),
    val startTime: String = "09:00",
    val endTime: String = "11:00",
    val address: String = "",
    val notes: String = "",
    val errorMessage: String? = null,
    val createdBooking: Booking? = null,
    /** US41: true when the estimated amount is shown before confirming (treatment of EC-BLQ06). */
    val showEstimate: Boolean = false,
    val estimate: BookingEstimate? = null,
    /** POST bookings/estimate in flight for the current schedule. */
    val isEstimating: Boolean = false,
    /** Reason given by the API when it cannot estimate the current data (for example a service not offered). */
    val estimateErrorMessage: String? = null,
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
                    requestApiEstimate()
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
        if (state.isSubmitting || state.isEstimating || state.createdBooking != null) return
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
        return state.copy(estimate = estimate, estimateErrorMessage = null)
    }

    private var estimateJob: Job? = null
    private var estimateGeneration = 0

    /**
     * US41 treatment: replaces the local value with POST bookings/estimate, which uses the worker's
     * current rate and the same validations as the booking. A response that arrives after the client
     * changed the schedule is discarded; without connection the local value (same rule) stays.
     */
    private fun requestApiEstimate() {
        estimateJob?.cancel()
        val generation = ++estimateGeneration
        val state = _uiState.value
        val request = estimateRequestFor(workerId, state)
        if (!state.showEstimate || request == null) {
            _uiState.update { it.copy(isEstimating = false) }
            return
        }
        _uiState.update { it.copy(isEstimating = true) }
        estimateJob = viewModelScope.launch {
            delay(ESTIMATE_DEBOUNCE_MILLIS)
            val result = bookingRepository.estimate(request)
            _uiState.update { current ->
                applyEstimateResult(current, workerId, request, generation == estimateGeneration, result)
            }
        }
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
        if (date == null || date.isBefore(BookingEstimator.todayInPeru())) {
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
        if (start != null && end != null && !BookingEstimator.meetsMinimumDuration(start, end)) {
            showValidation(MIN_DURATION_MESSAGE)
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
        val scheduleChanged = scheduleOf(_uiState.value) != scheduleOf(transform(_uiState.value))
        _uiState.update {
            val next = transform(it).copy(errorMessage = null)
            if (scheduleChanged) recalculate(next) else next
        }
        trackEstimateViewed(_uiState.value)
        // Address and notes do not change the amount: only the service and schedule are estimated again.
        if (scheduleChanged) requestApiEstimate()
    }

    private fun scheduleOf(state: CreateBookingUiState) =
        listOf(state.selectedService, state.date, state.startTime, state.endTime)

    companion object {
        const val MIN_DURATION_MESSAGE = "La duración mínima de una reserva es de 30 minutos."
        private const val ESTIMATE_DEBOUNCE_MILLIS = 350L
        // STRICT rejects dates such as 2026-02-30, which SMART would silently move to 2026-02-28.
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)

        /** Schedule sent to POST bookings/estimate, or null while the form cannot be estimated. */
        internal fun estimateRequestFor(
            workerId: Int,
            state: CreateBookingUiState,
            today: LocalDate = BookingEstimator.todayInPeru()
        ): BookingEstimateRequest? {
            if (state.estimate == null || state.selectedService.isBlank()) return null
            val date = try { LocalDate.parse(state.date, DATE_FORMAT) } catch (_: DateTimeParseException) { null }
            if (date == null || date.isBefore(today)) return null
            return BookingEstimateRequest(workerId, state.selectedService, state.date, state.startTime, state.endTime)
        }

        /**
         * Applies the API estimate to the current form. A response from a superseded request
         * ([isLatest] false) changes nothing; one for a schedule that no longer matches only ends
         * the loading state. A 400 shows the API's reason; other failures keep the local value.
         */
        internal fun applyEstimateResult(
            current: CreateBookingUiState,
            workerId: Int,
            sent: BookingEstimateRequest,
            isLatest: Boolean,
            result: BookingResult<BookingEstimateResponse>,
            today: LocalDate = BookingEstimator.todayInPeru()
        ): CreateBookingUiState {
            if (!isLatest) return current
            if (estimateRequestFor(workerId, current, today) != sent) return current.copy(isEstimating = false)
            return when (result) {
                is BookingResult.Success -> current.copy(
                    isEstimating = false,
                    estimate = BookingEstimator.fromApi(result.data),
                    estimateErrorMessage = null
                )
                is BookingResult.Error -> if (result.errorType == "http_400") {
                    current.copy(isEstimating = false, estimate = null, estimateErrorMessage = result.message)
                } else {
                    current.copy(isEstimating = false)
                }
            }
        }
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
