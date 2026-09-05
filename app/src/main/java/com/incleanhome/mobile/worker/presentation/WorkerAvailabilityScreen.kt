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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text("Volver")
            }
            Text(
                text = "Mi disponibilidad",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        when {
            uiState.isLoading -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null && uiState.slots.isEmpty() -> Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = viewModel::loadAvailability) {
                    Text("Reintentar")
                }
            }

            else -> {
                Text(
                    text = "Días: 0 domingo, 1 lunes, 2 martes, 3 miércoles, 4 jueves, 5 viernes, 6 sábado. Horarios en formato HH:mm.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.slots.isEmpty()) {
                    Text("No tienes disponibilidad registrada.")
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
                    Text("Agregar horario")
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
                        Text("Guardar disponibilidad")
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
                label = { Text("Día de la semana (0-6)") },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = slot.startTime,
                onValueChange = onStartTimeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hora de inicio (HH:mm)") },
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                value = slot.endTime,
                onValueChange = onEndTimeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hora de fin (HH:mm)") },
                singleLine = true,
                enabled = enabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (slot.isAvailable) "Disponible" else "No disponible")
                Switch(
                    checked = slot.isAvailable,
                    onCheckedChange = onAvailableChange,
                    enabled = enabled
                )
            }
            Button(onClick = onRemove, enabled = enabled) {
                Text("Eliminar horario")
            }
        }
    }
}
