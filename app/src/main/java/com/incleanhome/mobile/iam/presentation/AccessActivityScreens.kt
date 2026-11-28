package com.incleanhome.mobile.iam.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.R
import com.incleanhome.mobile.iam.data.AccessActivity
import com.incleanhome.mobile.iam.data.AccessActivityRepository
import com.incleanhome.mobile.iam.data.AccessActivityResult
import com.incleanhome.mobile.iam.data.AccessEntry
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.format.localizedUiMessage
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Presentation rules of US40 kept apart from Compose so they can be unit tested. */
object AccessActivityFormat {
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /** The API sends UTC; the list shows the time of the device. Unreadable values are shown as received. */
    fun localTime(occurredAt: String, zone: ZoneId = ZoneId.systemDefault()): String = runCatching {
        OffsetDateTime.parse(occurredAt).atZoneSameInstant(zone).format(FORMAT)
    }.getOrDefault(occurredAt)

    @StringRes
    fun platformLabel(platform: String): Int = when (platform.trim().lowercase()) {
        "mobile" -> R.string.access_platform_mobile
        "web" -> R.string.access_platform_web
        "api" -> R.string.access_platform_api
        else -> R.string.access_platform_unknown
    }
}

data class AccessActivityUiState(
    val isLoading: Boolean = true,
    val activity: AccessActivity? = null,
    val loadErrorMessage: String? = null,
    /** Access waiting for the confirmation of the report dialog. */
    val confirmingReport: AccessEntry? = null,
    val reportingId: Int? = null,
    val reportedIds: Set<Int> = emptySet(),
    val reportMessage: String? = null,
    val errorMessage: String? = null
)

class AccessActivityViewModel(
    private val repository: AccessActivityRepository = AccessActivityRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccessActivityUiState())
    val uiState: StateFlow<AccessActivityUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadErrorMessage = null, reportMessage = null, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.load()) {
                is AccessActivityResult.Success -> _uiState.update { it.copy(isLoading = false, activity = result.data) }
                is AccessActivityResult.Error -> _uiState.update { it.copy(isLoading = false, loadErrorMessage = result.message) }
            }
        }
    }

    fun askReport(entry: AccessEntry) = _uiState.update { it.copy(confirmingReport = entry, errorMessage = null) }
    fun cancelReport() = _uiState.update { it.copy(confirmingReport = null) }

    /** Escenario 2: the API closes the other sessions and asks for a new password. */
    fun confirmReport() {
        val entry = _uiState.value.confirmingReport ?: return
        if (_uiState.value.reportingId != null) return
        _uiState.update { it.copy(confirmingReport = null, reportingId = entry.id, errorMessage = null, reportMessage = null) }
        viewModelScope.launch {
            when (val result = repository.report(entry.id)) {
                is AccessActivityResult.Success -> {
                    _uiState.update { it.copy(reportingId = null, reportedIds = it.reportedIds + entry.id, reportMessage = REPORTED_MESSAGE) }
                    refreshAfterReport()
                }
                is AccessActivityResult.Error -> _uiState.update { it.copy(reportingId = null, errorMessage = result.message) }
            }
        }
    }

    // Reads passwordChangeRequired again from the API instead of assuming it.
    private suspend fun refreshAfterReport() {
        val result = repository.load()
        if (result is AccessActivityResult.Success) _uiState.update { it.copy(activity = result.data) }
    }

    companion object {
        const val REPORTED_MESSAGE = "Reportamos el acceso. Cerramos tus otras sesiones; cambia tu contraseña."

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AccessActivityViewModel() as T
        }
    }
}

data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val confirmation: String = "",
    val isSaving: Boolean = false,
    val changed: Boolean = false,
    val errorMessage: String? = null
)

class ChangePasswordViewModel(
    private val repository: AccessActivityRepository = AccessActivityRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun updateCurrent(value: String) = _uiState.update { it.copy(current = value, errorMessage = null) }
    fun updateNew(value: String) = _uiState.update { it.copy(new = value, errorMessage = null) }
    fun updateConfirmation(value: String) = _uiState.update { it.copy(confirmation = value, errorMessage = null) }

    /** The registration policy is checked by the API; its message is shown as received. */
    fun submit() {
        val state = _uiState.value
        if (state.isSaving || state.changed) return
        val error = when {
            state.current.isEmpty() || state.new.isEmpty() -> "Ingresa tu contraseña actual y la nueva contraseña."
            state.new != state.confirmation -> "La confirmación no coincide con la nueva contraseña."
            state.new == state.current -> "La nueva contraseña debe ser distinta de la actual."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.changePassword(state.current, state.new)) {
                // The fields are cleared: passwords never stay in memory longer than needed.
                is AccessActivityResult.Success -> _uiState.value = ChangePasswordUiState(changed = true)
                is AccessActivityResult.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ChangePasswordViewModel() as T
        }
    }
}

@Composable
fun AccessActivityScreen(
    viewModel: AccessActivityViewModel,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    ScreenBackground(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(InCleanHomeDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(stringResource(R.string.access_activity_title), onBack)
            Text(stringResource(R.string.access_activity_intro), style = MaterialTheme.typography.bodyLarge)
            when {
                state.isLoading -> LoadingState()
                state.loadErrorMessage != null -> ErrorRetryState(state.loadErrorMessage.orEmpty(), viewModel::load)
                else -> state.activity?.let { activity ->
                    if (activity.passwordChangeRequired) {
                        InCleanHomeCard {
                            Text(stringResource(R.string.access_activity_password_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            PrimaryButton(stringResource(R.string.change_password_title), onChangePassword)
                        }
                    }
                    Text(
                        stringResource(if (activity.twoFactorEnabled) R.string.access_activity_2fa_on else R.string.access_activity_2fa_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (activity.twoFactorEnabled) PrimaryGreen else MaterialTheme.colorScheme.error
                    )
                    state.reportMessage?.let { Text(localizedUiMessage(it), color = PrimaryGreen) }
                    state.errorMessage?.let { Text(localizedUiMessage(it), color = MaterialTheme.colorScheme.error) }
                    if (activity.accesses.isEmpty()) {
                        EmptyState(stringResource(R.string.access_activity_empty))
                    } else {
                        activity.accesses.forEach { entry ->
                            AccessCard(
                                entry = entry,
                                reported = entry.id in state.reportedIds,
                                reporting = state.reportingId == entry.id,
                                onReport = { viewModel.askReport(entry) }
                            )
                        }
                    }
                    if (!activity.passwordChangeRequired) {
                        SecondaryButton(stringResource(R.string.change_password_title), onChangePassword, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    state.confirmingReport?.let { entry ->
        AlertDialog(
            onDismissRequest = viewModel::cancelReport,
            title = { Text(stringResource(R.string.access_report_confirm_title)) },
            text = { Text(stringResource(R.string.access_report_confirm_body, AccessActivityFormat.localTime(entry.occurredAt))) },
            confirmButton = { TextButton(onClick = viewModel::confirmReport) { Text(stringResource(R.string.access_report_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelReport) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun AccessCard(entry: AccessEntry, reported: Boolean, reporting: Boolean, onReport: () -> Unit) {
    InCleanHomeCard {
        Text(AccessActivityFormat.localTime(entry.occurredAt), style = MaterialTheme.typography.titleMedium, color = Navy)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.access_entry_platform, stringResource(AccessActivityFormat.platformLabel(entry.platform))),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(if (entry.succeeded) R.string.access_entry_success else R.string.access_entry_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.succeeded) PrimaryGreen else MaterialTheme.colorScheme.error
        )
        Text(
            stringResource(if (entry.twoFactorVerified) R.string.access_entry_2fa_verified else R.string.access_entry_2fa_not_verified),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (reported) {
            Text(stringResource(R.string.access_entry_reported), color = PrimaryGreen, style = MaterialTheme.typography.labelLarge)
        } else {
            SecondaryButton(
                text = stringResource(if (reporting) R.string.access_reporting else R.string.access_report_action),
                onClick = onReport,
                modifier = Modifier.fillMaxWidth(),
                enabled = !reporting
            )
        }
    }
}

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onChanged: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.changed) { if (state.changed) onChanged() }
    ScreenBackground(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(InCleanHomeDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(stringResource(R.string.change_password_title), onBack)
            Text(stringResource(R.string.change_password_intro), style = MaterialTheme.typography.bodyLarge)
            if (state.changed) {
                Text(stringResource(R.string.change_password_done), color = PrimaryGreen)
                PrimaryButton(stringResource(R.string.action_back), onBack)
                return@Column
            }
            InCleanHomeCard {
                PasswordField(state.current, viewModel::updateCurrent, stringResource(R.string.change_password_current), !state.isSaving)
                Spacer(Modifier.height(8.dp))
                PasswordField(state.new, viewModel::updateNew, stringResource(R.string.change_password_new), !state.isSaving)
                Spacer(Modifier.height(8.dp))
                PasswordField(state.confirmation, viewModel::updateConfirmation, stringResource(R.string.change_password_confirm), !state.isSaving)
            }
            state.errorMessage?.let { Text(localizedUiMessage(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryButton(stringResource(R.string.action_save), viewModel::submit, enabled = !state.isSaving, loading = state.isSaving)
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean) {
    InCleanHomeTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation()
    )
}
