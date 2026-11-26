package com.incleanhome.mobile.worker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.BuildConfig
import com.incleanhome.mobile.R
import com.incleanhome.mobile.profile.data.ProfileRepository
import com.incleanhome.mobile.profile.data.ProfileResult
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.humanizeIdentifier
import com.incleanhome.mobile.ui.format.localizedUiMessage
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen
import com.incleanhome.mobile.worker.data.ServiceArea
import com.incleanhome.mobile.worker.data.ServiceAreaRepository
import com.incleanhome.mobile.worker.data.ServiceAreaResult
import com.incleanhome.mobile.worker.data.ServiceAreaRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServiceAreaUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val zones: List<String> = emptyList(),
    val district: String = "",
    val radius: String = "",
    val apiAvailable: Boolean = true,
    val saved: Boolean = false,
    val errorMessage: String? = null
)

class ServiceAreaViewModel(
    private val repository: ServiceAreaRepository = ServiceAreaRepository(BuildConfig.SERVICE_AREA_API_ENABLED),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    val minKm: Int = BuildConfig.SERVICE_RADIUS_MIN_KM,
    val maxKm: Int = BuildConfig.SERVICE_RADIUS_MAX_KM
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServiceAreaUiState())
    val uiState: StateFlow<ServiceAreaUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val zones = when (val profile = profileRepository.worker()) {
                is ProfileResult.Success -> profile.data.zones
                is ProfileResult.Error -> emptyList()
            }
            when (val result = repository.load()) {
                is ServiceAreaResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        zones = zones,
                        district = result.area?.district ?: zones.firstOrNull().orEmpty(),
                        radius = result.area?.radiusKm?.toString().orEmpty()
                    )
                }
                ServiceAreaResult.Unavailable -> _uiState.update {
                    it.copy(isLoading = false, zones = zones, district = zones.firstOrNull().orEmpty(), apiAvailable = false)
                }
                is ServiceAreaResult.Error -> _uiState.update {
                    it.copy(isLoading = false, zones = zones, errorMessage = result.message)
                }
            }
        }
    }

    fun selectDistrict(value: String) = _uiState.update { it.copy(district = value, saved = false, errorMessage = null) }
    fun updateRadius(value: String) = _uiState.update { it.copy(radius = value.filter(Char::isDigit).take(3), saved = false, errorMessage = null) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val radius = state.radius.toIntOrNull()
        val error = when {
            state.district.isBlank() -> "Selecciona el distrito de tu ubicación base."
            ServiceAreaRules.radiusError(radius, minKm, maxKm) -> "El radio de atención debe estar dentro del rango permitido."
            else -> null
        }
        // Escenario 2: out-of-range values are not sent and the allowed range is shown.
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.save(ServiceArea(state.district, radius!!))) {
                is ServiceAreaResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                ServiceAreaResult.Unavailable -> _uiState.update { it.copy(isSaving = false, apiAvailable = false) }
                is ServiceAreaResult.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ServiceAreaViewModel() as T
        }
    }
}

@Composable
fun ServiceAreaScreen(viewModel: ServiceAreaViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ScreenBackground(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(InCleanHomeDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(stringResource(R.string.service_area_title), onBack)
            Text(stringResource(R.string.service_area_intro), style = MaterialTheme.typography.bodyLarge)
            if (state.isLoading) {
                LoadingState()
                return@Column
            }
            InCleanHomeCard {
                Text(stringResource(R.string.service_area_district), style = MaterialTheme.typography.titleMedium, color = Navy)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.zones.forEach { zone ->
                        FilterChip(
                            selected = state.district == zone,
                            onClick = { viewModel.selectDistrict(zone) },
                            label = { Text(humanizeIdentifier(zone)) }
                        )
                    }
                }
                if (state.zones.isEmpty()) {
                    Text(stringResource(R.string.service_area_no_zones), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                InCleanHomeTextField(
                    value = state.radius,
                    onValueChange = viewModel::updateRadius,
                    label = stringResource(R.string.service_area_radius, viewModel.minKm, viewModel.maxKm),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !state.isSaving
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.service_area_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.errorMessage?.let {
                Text(localizedUiMessage(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (it.startsWith("El radio")) {
                    Text(stringResource(R.string.service_area_range, viewModel.minKm, viewModel.maxKm), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!state.apiAvailable) {
                Text(stringResource(R.string.service_area_unavailable), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.saved) {
                Text(stringResource(R.string.service_area_saved), color = PrimaryGreen)
            }
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = state.apiAvailable && !state.isSaving,
                loading = state.isSaving
            )
        }
    }
}
