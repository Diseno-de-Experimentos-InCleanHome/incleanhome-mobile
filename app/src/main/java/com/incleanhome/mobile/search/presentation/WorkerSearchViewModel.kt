package com.incleanhome.mobile.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.booking.domain.BookingEstimator
import com.incleanhome.mobile.core.location.District
import com.incleanhome.mobile.core.location.Districts
import com.incleanhome.mobile.experiments.Assignment
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.Experimentation
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import com.incleanhome.mobile.search.domain.SuggestionRequest
import com.incleanhome.mobile.search.domain.SuggestionResult
import com.incleanhome.mobile.search.domain.WorkerSuggestionService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerSearchUiState(
    val serviceType: String = "",
    val zone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val workers: List<Worker> = emptyList(),
    val hasSearched: Boolean = false,
    val searchId: String? = null,
    /** US33 is available only for clients in the treatment group of EC-BLQ01. */
    val suggestionsEnabled: Boolean = false,
    val suggestionDate: String = BookingEstimator.todayInPeru().plusDays(1).toString(),
    /** Canonical key of the district of the service, chosen from the closed list (US32). */
    val suggestionDistrict: String = "",
    val districts: List<District> = Districts.ALL,
    val suggestionStart: String = "09:00",
    val suggestionEnd: String = "11:00",
    val isSuggesting: Boolean = false,
    val suggestionError: String? = null,
    val suggestions: List<Worker>? = null,
    val suggestionSearchId: String? = null
)

class WorkerSearchViewModel(
    private val repository: WorkerRepository = WorkerRepository(),
    private val suggestionService: WorkerSuggestionService = WorkerSuggestionService(),
    private val experiments: ExperimentService? = Experimentation.experimentsOrNull(),
    private val tracker: EventTracker? = Experimentation.trackerOrNull()
) : ViewModel() {
    private val suggestionAssignment: Assignment =
        experiments?.recordExposure(Experiment.WORKER_SUGGESTIONS, "worker_search")
            ?: Assignment.inactive(Experiment.WORKER_SUGGESTIONS)

    private val _uiState = MutableStateFlow(WorkerSearchUiState(suggestionsEnabled = suggestionAssignment.featureActive))
    val uiState: StateFlow<WorkerSearchUiState> = _uiState.asStateFlow()

    init {
        search(userInitiated = false)
    }

    fun onServiceTypeChange(serviceType: String) {
        _uiState.update { it.copy(serviceType = serviceType, errorMessage = null, suggestionError = null) }
    }

    fun onZoneChange(zone: String) {
        _uiState.update { it.copy(zone = zone, errorMessage = null, suggestionError = null) }
    }

    fun onSuggestionDistrictChange(value: String) = _uiState.update { it.copy(suggestionDistrict = value, suggestionError = null) }
    fun onSuggestionDateChange(value: String) = _uiState.update { it.copy(suggestionDate = value.take(10), suggestionError = null) }
    fun onSuggestionStartChange(value: String) = _uiState.update { it.copy(suggestionStart = value.take(5), suggestionError = null) }
    fun onSuggestionEndChange(value: String) = _uiState.update { it.copy(suggestionEnd = value.take(5), suggestionError = null) }

    fun search() = search(userInitiated = true)

    private fun search(userInitiated: Boolean) {
        val state = _uiState.value
        if (state.isLoading) return

        val searchId = UUID.randomUUID().toString()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.searchWorkers(state.serviceType, state.zone)) {
                is WorkerResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            workers = result.data,
                            hasSearched = true,
                            searchId = searchId
                        )
                    }
                    // The automatic first listing is not a search decided by the client (M01).
                    if (userInitiated) {
                        tracker?.track(
                            TrackingPlan.WORKER_SEARCH_PERFORMED,
                            mapOf(
                                "searchId" to searchId,
                                "searchMode" to "filters",
                                "serviceCategory" to state.serviceType.ifBlank { null },
                                "district" to state.zone.trim().ifBlank { null },
                                "filtersApplied" to listOf(state.serviceType, state.zone).count(String::isNotBlank),
                                "resultsCount" to result.data.size
                            ),
                            operationKey = searchId,
                            assignment = suggestionAssignment
                        )
                    }
                }

                is WorkerResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            workers = emptyList(),
                            hasSearched = true
                        )
                    }
                }
            }
        }
    }

    fun requestSuggestions() {
        val state = _uiState.value
        if (!state.suggestionsEnabled || state.isSuggesting) return
        val request = buildSuggestionRequest(state) ?: return
        val searchId = UUID.randomUUID().toString()
        _uiState.update { it.copy(isSuggesting = true, suggestionError = null) }
        viewModelScope.launch {
            when (val result = suggestionService.suggest(request)) {
                is SuggestionResult.Success -> {
                    _uiState.update {
                        it.copy(isSuggesting = false, suggestions = result.workers, suggestionSearchId = searchId)
                    }
                    tracker?.track(
                        TrackingPlan.WORKER_SEARCH_PERFORMED,
                        mapOf(
                            "searchId" to searchId,
                            "searchMode" to "suggestion",
                            "serviceCategory" to request.serviceType,
                            "district" to request.district,
                            "filtersApplied" to 2,
                            "resultsCount" to result.workers.size
                        ),
                        operationKey = searchId,
                        assignment = suggestionAssignment
                    )
                    tracker?.track(
                        TrackingPlan.WORKER_SUGGESTIONS_SHOWN,
                        mapOf("searchId" to searchId, "suggestionCount" to result.workers.size),
                        operationKey = searchId,
                        assignment = suggestionAssignment
                    )
                }
                // Escenario 3: the API decides; without the feature only the filter search remains.
                SuggestionResult.NotActive -> _uiState.update {
                    it.copy(isSuggesting = false, suggestionsEnabled = false, suggestions = null)
                }
                is SuggestionResult.Error -> _uiState.update {
                    it.copy(
                        isSuggesting = false,
                        suggestionError = result.message,
                        suggestions = null,
                        districts = if (result.validDistricts.isEmpty()) it.districts else Districts.fromApi(result.validDistricts)
                    )
                }
            }
        }
    }

    private fun buildSuggestionRequest(state: WorkerSearchUiState): SuggestionRequest? {
        val date = try { LocalDate.parse(state.suggestionDate) } catch (_: DateTimeParseException) { null }
        val start = parseTime(state.suggestionStart)
        val end = parseTime(state.suggestionEnd)
        val message = when {
            state.serviceType.isBlank() -> "Selecciona un tipo de servicio para recibir sugerencias."
            state.suggestionDistrict.isBlank() -> "Ingresa el distrito del servicio para recibir sugerencias."
            // Same rules as the API: today or later in Peru and at least 30 minutes.
            date == null || date.isBefore(BookingEstimator.todayInPeru()) ->
                "Ingresa una fecha válida en formato yyyy-MM-dd que no esté en el pasado."
            start == null || end == null || !end.isAfter(start) ->
                "Usa horas HH:mm y una hora de fin posterior a la de inicio."
            !BookingEstimator.meetsMinimumDuration(start, end) -> "La duración mínima de una reserva es de 30 minutos."
            else -> null
        }
        if (message != null) {
            _uiState.update { it.copy(suggestionError = message) }
            return null
        }
        return SuggestionRequest(state.serviceType, date!!, start!!, end!!, state.suggestionDistrict)
    }

    private fun parseTime(value: String): LocalTime? = try {
        LocalTime.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(WorkerSearchViewModel::class.java)) {
                    return WorkerSearchViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
