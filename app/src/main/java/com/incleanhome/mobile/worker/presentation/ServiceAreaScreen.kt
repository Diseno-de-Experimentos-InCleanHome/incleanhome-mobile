package com.incleanhome.mobile.worker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.incleanhome.mobile.core.location.District
import com.incleanhome.mobile.core.location.Districts
import com.incleanhome.mobile.profile.data.ProfileRepository
import com.incleanhome.mobile.profile.data.ProfileResult
import com.incleanhome.mobile.ui.components.DistrictSelector
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
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
    val districts: List<District> = Districts.ALL,
    val district: String = "",
    val radius: String = "",
    /** Area stored in the API, the only one shown as registered. */
    val savedArea: ServiceArea? = null,
    /** 403 of the API: EC-BLQ01-SA is not active for this worker. */
    val notActive: Boolean = false,
    val saved: Boolean = false,
    val loadErrorMessage: String? = null,
    val errorMessage: String? = null
)

class ServiceAreaViewModel(
    private val repository: ServiceAreaRepository = ServiceAreaRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    val minKm: Int = BuildConfig.SERVICE_RADIUS_MIN_KM,
    val maxKm: Int = BuildConfig.SERVICE_RADIUS_MAX_KM
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServiceAreaUiState())
    val uiState: StateFlow<ServiceAreaUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadErrorMessage = null, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.load()) {
                is ServiceAreaResult.Success -> {
                    val area = result.area
                    // Without a registered area, the first profile zone that the API accepts is proposed.
                    val proposed = area?.district ?: suggestedDistrict()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            savedArea = area,
                            district = Districts.find(proposed)?.key ?: proposed.orEmpty(),
                            radius = area?.radiusKm?.toString().orEmpty()
                        )
                    }
                }
                ServiceAreaResult.NotActive -> _uiState.update { it.copy(isLoading = false, notActive = true) }
                is ServiceAreaResult.Rejected -> _uiState.update { it.copy(isLoading = false, loadErrorMessage = result.message) }
                is ServiceAreaResult.Error -> _uiState.update { it.copy(isLoading = false, loadErrorMessage = result.message) }
            }
        }
    }

    private suspend fun suggestedDistrict(): String? = when (val profile = profileRepository.worker()) {
        is ProfileResult.Success -> profile.data.zones.firstNotNullOfOrNull { Districts.find(it)?.key }
        is ProfileResult.Error -> null
    }

    fun selectDistrict(value: String) = _uiState.update { it.copy(district = value, saved = false, errorMessage = null) }
    fun updateRadius(value: String) = _uiState.update { it.copy(radius = value.filter(Char::isDigit).take(3), saved = false, errorMessage = null) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.notActive) return
        val radius = state.radius.toIntOrNull()
        val error = when {
            state.district.isBlank() -> "Selecciona el distrito de tu ubicación base."
            ServiceAreaRules.radiusError(radius, minKm, maxKm) -> RADIUS_MESSAGE
            else -> null
        }
        // Escenario 2: out-of-range values are not sent and the allowed range is shown.
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error, saved = false) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saved = false) }
        viewModelScope.launch {
            when (val result = repository.save(ServiceArea(state.district, radius!!))) {
                is ServiceAreaResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        saved = result.area != null,
                        savedArea = result.area ?: it.savedArea,
                        district = result.area?.district ?: it.district,
                        radius = result.area?.radiusKm?.toString() ?: it.radius
                    )
                }
                ServiceAreaResult.NotActive -> _uiState.update { it.copy(isSaving = false, notActive = true) }
                is ServiceAreaResult.Rejected -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.message,
                        districts = if (result.validDistricts.isEmpty()) it.districts else Districts.fromApi(result.validDistricts)
                    )
                }
                is ServiceAreaResult.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
            }
        }
    }

    companion object {
        const val RADIUS_MESSAGE = "El radio de atención debe estar dentro del rango permitido."

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
            when {
                state.isLoading -> {
                    LoadingState()
                    return@Column
                }
                state.notActive -> {
                    Text(stringResource(R.string.service_area_not_active), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    return@Column
                }
                state.loadErrorMessage != null -> {
                    ErrorRetryState(state.loadErrorMessage.orEmpty(), viewModel::load)
                    return@Column
                }
            }
            InCleanHomeCard {
                val registered = state.savedArea
                Text(
                    text = if (registered == null) {
                        stringResource(R.string.service_area_none)
                    } else {
                        stringResource(R.string.service_area_current, Districts.label(registered.district).orEmpty(), registered.radiusKm)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.service_area_district), style = MaterialTheme.typography.titleMedium, color = Navy)
                Spacer(Modifier.height(8.dp))
                DistrictSelector(
                    districts = state.districts,
                    selectedKey = state.district,
                    placeholder = stringResource(R.string.service_area_select_district),
                    onSelect = viewModel::selectDistrict,
                    enabled = !state.isSaving
                )
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
                if (it == ServiceAreaViewModel.RADIUS_MESSAGE) {
                    Text(stringResource(R.string.service_area_range, viewModel.minKm, viewModel.maxKm), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.saved) {
                Text(stringResource(R.string.service_area_saved), color = PrimaryGreen)
            }
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = !state.isSaving,
                loading = state.isSaving
            )
        }
    }
}
