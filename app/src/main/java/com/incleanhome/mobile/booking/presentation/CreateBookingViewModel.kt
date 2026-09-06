package com.incleanhome.mobile.booking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingRepository
import com.incleanhome.mobile.booking.data.BookingResult
import com.incleanhome.mobile.booking.data.CreateBookingRequest
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
    val createdBooking: Booking? = null
)

class CreateBookingViewModel(
    private val workerId: Int,
    private val workerRepository: WorkerRepository = WorkerRepository(),
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateBookingUiState())
    val uiState: StateFlow<CreateBookingUiState> = _uiState.asStateFlow()

    init { loadWorker() }

    fun loadWorker() {
        _uiState.update { it.copy(isLoadingWorker = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = workerRepository.getWorker(workerId)) {
                is WorkerResult.Success -> _uiState.update {
                    it.copy(
                        isLoadingWorker = false,
                        worker = result.data,
                        selectedService = it.selectedService.takeIf(result.data.serviceTypes::contains)
                            ?: result.data.serviceTypes.firstOrNull().orEmpty()
                    )
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
        if (state.isSubmitting) return
        val request = buildRequest(state) ?: return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = bookingRepository.create(request)) {
                is BookingResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, createdBooking = result.data)
                }
                is BookingResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
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
        if (start == null || end == null || !end.isAfter(start)) {
            showValidation("Usa horas HH:mm y una hora de fin posterior a la de inicio.")
            return null
        }
        if (state.address.isBlank()) {
            showValidation("La dirección es obligatoria.")
            return null
        }
        val minutes = java.time.Duration.between(start, end).toMinutes()
        val hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
        return CreateBookingRequest(
            workerId = workerId,
            serviceType = state.selectedService,
            date = state.date,
            startTime = state.startTime,
            endTime = state.endTime,
            hours = hours,
            address = state.address.trim(),
            notes = state.notes.trim().takeIf(String::isNotEmpty)
        )
    }

    private fun parseTime(value: String): LocalTime? = try {
        LocalTime.parse(value, TIME_FORMAT)
    } catch (_: DateTimeParseException) { null }

    private fun showValidation(message: String) = _uiState.update { it.copy(errorMessage = message) }
    private fun edit(transform: (CreateBookingUiState) -> CreateBookingUiState) =
        _uiState.update { transform(it).copy(errorMessage = null) }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

        fun Factory(workerId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CreateBookingViewModel::class.java)) {
                    return CreateBookingViewModel(workerId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
