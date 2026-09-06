package com.incleanhome.mobile.booking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingRepository
import com.incleanhome.mobile.booking.data.BookingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingListUiState(
    val isLoading: Boolean = true,
    val bookings: List<Booking> = emptyList(),
    val errorMessage: String? = null
)

class BookingListViewModel(
    private val repository: BookingRepository = BookingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingListUiState())
    val uiState: StateFlow<BookingListUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getMine()) {
                is BookingResult.Success -> _uiState.update {
                    it.copy(isLoading = false, bookings = result.data)
                }
                is BookingResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BookingListViewModel::class.java)) {
                    return BookingListViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

data class BookingDetailUiState(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val booking: Booking? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BookingDetailViewModel(
    private val bookingId: Int,
    private val repository: BookingRepository = BookingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getMine()) {
                is BookingResult.Success -> {
                    val booking = result.data.firstOrNull { it.id == bookingId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            booking = booking,
                            errorMessage = if (booking == null) "No se encontró la reserva." else null
                        )
                    }
                }
                is BookingResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun updateStatus(status: String) {
        if (_uiState.value.isUpdating) return
        _uiState.update { it.copy(isUpdating = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = repository.updateStatus(bookingId, status)) {
                is BookingResult.Success -> _uiState.update {
                    it.copy(
                        isUpdating = false,
                        booking = result.data,
                        successMessage = "Estado actualizado correctamente."
                    )
                }
                is BookingResult.Error -> _uiState.update {
                    it.copy(isUpdating = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun Factory(bookingId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BookingDetailViewModel::class.java)) {
                    return BookingDetailViewModel(bookingId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
