package com.incleanhome.mobile.worker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.worker.data.AvailabilitySlotInput
import com.incleanhome.mobile.worker.data.WorkerAvailabilitySlot
import com.incleanhome.mobile.worker.data.WorkerSelfRepository
import com.incleanhome.mobile.worker.data.WorkerSelfResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditableAvailabilitySlot(
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean
)

data class WorkerAvailabilityUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val slots: List<EditableAvailabilitySlot> = emptyList()
)

class WorkerAvailabilityViewModel(
    private val workerId: Int,
    private val repository: WorkerSelfRepository = WorkerSelfRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkerAvailabilityUiState())
    val uiState: StateFlow<WorkerAvailabilityUiState> = _uiState.asStateFlow()

    init {
        loadAvailability()
    }

    fun loadAvailability() {
        _uiState.update {
            it.copy(isLoading = true, errorMessage = null, successMessage = null)
        }
        viewModelScope.launch {
            applyLoadedAvailability(repository.getAvailability(workerId))
        }
    }

    fun addSlot() {
        _uiState.update {
            it.copy(
                slots = it.slots + EditableAvailabilitySlot(
                    dayOfWeek = "0",
                    startTime = "08:00",
                    endTime = "18:00",
                    isAvailable = true
                ),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun removeSlot(index: Int) {
        _uiState.update {
            it.copy(
                slots = it.slots.filterIndexed { slotIndex, _ -> slotIndex != index },
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun updateDay(index: Int, value: String) {
        updateSlot(index) {
            it.copy(dayOfWeek = value.filter(Char::isDigit).take(1))
        }
    }

    fun updateStartTime(index: Int, value: String) {
        updateSlot(index) { it.copy(startTime = value.take(5)) }
    }

    fun updateEndTime(index: Int, value: String) {
        updateSlot(index) { it.copy(endTime = value.take(5)) }
    }

    fun updateAvailable(index: Int, isAvailable: Boolean) {
        updateSlot(index) { it.copy(isAvailable = isAvailable) }
    }

    fun saveAvailability() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return

        val slots = state.slots.mapNotNull(::toRequestSlot)
        if (slots.size != state.slots.size) {
            _uiState.update {
                it.copy(errorMessage = "Usa días del 0 al 6 y horarios en formato HH:mm.")
            }
            return
        }

        _uiState.update {
            it.copy(isSaving = true, errorMessage = null, successMessage = null)
        }
        viewModelScope.launch {
            when (val saveResult = repository.replaceAvailability(workerId, slots)) {
                is WorkerSelfResult.Success -> {
                    when (val refreshed = repository.getAvailability(workerId)) {
                        is WorkerSelfResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    slots = refreshed.data.map(::toEditableSlot),
                                    successMessage = "Disponibilidad guardada."
                                )
                            }
                        }

                        is WorkerSelfResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = refreshed.message
                                )
                            }
                        }
                    }
                }

                is WorkerSelfResult.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = saveResult.message)
                    }
                }
            }
        }
    }

    private fun applyLoadedAvailability(
        result: WorkerSelfResult<List<WorkerAvailabilitySlot>>
    ) {
        when (result) {
            is WorkerSelfResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        slots = result.data.map(::toEditableSlot)
                    )
                }
            }

            is WorkerSelfResult.Error -> {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun updateSlot(
        index: Int,
        transform: (EditableAvailabilitySlot) -> EditableAvailabilitySlot
    ) {
        _uiState.update { state ->
            state.copy(
                slots = state.slots.mapIndexed { slotIndex, slot ->
                    if (slotIndex == index) transform(slot) else slot
                },
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun toRequestSlot(slot: EditableAvailabilitySlot): AvailabilitySlotInput? {
        val day = slot.dayOfWeek.toIntOrNull()?.takeIf { it in 0..6 } ?: return null
        if (!TIME_PATTERN.matches(slot.startTime) || !TIME_PATTERN.matches(slot.endTime)) return null
        return AvailabilitySlotInput(
            dayOfWeek = day,
            startTime = slot.startTime,
            endTime = slot.endTime,
            isAvailable = slot.isAvailable
        )
    }

    private fun toEditableSlot(slot: WorkerAvailabilitySlot): EditableAvailabilitySlot {
        return EditableAvailabilitySlot(
            dayOfWeek = slot.dayOfWeek.toString(),
            startTime = slot.startTime,
            endTime = slot.endTime,
            isAvailable = slot.isAvailable
        )
    }

    companion object {
        private val TIME_PATTERN = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")

        fun Factory(workerId: Int): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(WorkerAvailabilityViewModel::class.java)) {
                        return WorkerAvailabilityViewModel(workerId = workerId) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
