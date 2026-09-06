package com.incleanhome.mobile.worker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.ScreenHeader

@Composable
fun WorkerAvailabilityScreen(
    viewModel: WorkerAvailabilityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(stringResource(R.string.title_my_availability), onBack)
        Spacer(modifier = Modifier.height(20.dp))

        when {
            uiState.isLoading -> LoadingState()

            uiState.errorMessage != null && uiState.slots.isEmpty() ->
                ErrorRetryState(uiState.errorMessage.orEmpty(), viewModel::loadAvailability)

            else -> {
                Text(
                    text = stringResource(R.string.availability_help),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.slots.isEmpty()) {
                    EmptyState(stringResource(R.string.empty_my_availability))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.slots.forEachIndexed { index, slot ->
                        AvailabilityEditor(
                            slot = slot,
                            onDayChange = { viewModel.updateDay(index, it) },
                            onStartTimeChange = { viewModel.updateStartTime(index, it) },
                            onEndTimeChange = { viewModel.updateEndTime(index, it) },
                            onAvailableChange = { viewModel.updateAvailable(index, it) },
                            onRemove = { viewModel.removeSlot(index) },
                            enabled = !uiState.isSaving
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = viewModel::addSlot,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    Text(stringResource(R.string.action_add_schedule))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::saveAvailability,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_save_availability))
                    }
                }

                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                uiState.successMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AvailabilityEditor(
    slot: EditableAvailabilitySlot,
    onDayChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onAvailableChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = slot.dayOfWeek,
                onValueChange = onDayChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_day_of_week)) },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = slot.startTime,
                onValueChange = onStartTimeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_start_time)) },
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                value = slot.endTime,
                onValueChange = onEndTimeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_end_time)) },
                singleLine = true,
                enabled = enabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(if (slot.isAvailable) R.string.availability_available else R.string.availability_unavailable))
                Switch(
                    checked = slot.isAvailable,
                    onCheckedChange = onAvailableChange,
                    enabled = enabled
                )
            }
            Button(onClick = onRemove, enabled = enabled) {
                Text(stringResource(R.string.action_remove_schedule))
            }
        }
    }
}
