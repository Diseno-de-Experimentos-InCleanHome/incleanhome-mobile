package com.incleanhome.mobile.booking.presentation

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CreateBookingScreen(
    viewModel: CreateBookingViewModel,
    onBack: () -> Unit,
    onViewBookings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text("Volver") }
            Text(
                "Nueva reserva",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (state.isLoadingWorker) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        state.createdBooking?.let { booking ->
            Text("Reserva creada correctamente", style = MaterialTheme.typography.headlineSmall)
            Text("${booking.workerName} · ${booking.serviceType}")
            Text("${booking.date}, ${booking.startTime} - ${booking.endTime}")
            Text("Estado: ${booking.status}")
            Button(onClick = onViewBookings, modifier = Modifier.fillMaxWidth()) {
                Text("Ver Mis reservas")
            }
            return@Column
        }

        val worker = state.worker
        if (worker == null) {
            Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            Button(onClick = viewModel::loadWorker) { Text("Reintentar") }
            return@Column
        }

        Text("Trabajador: ${worker.name}", style = MaterialTheme.typography.titleLarge)
        Text("Selecciona un servicio ofrecido por este trabajador:")
        worker.serviceTypes.forEach { service ->
            FilterChip(
                selected = state.selectedService == service,
                onClick = { viewModel.selectService(service) },
                label = { Text(service) },
                enabled = !state.isSubmitting
            )
        }
        if (worker.serviceTypes.isEmpty()) {
            Text("Este trabajador no tiene servicios registrados.")
        }

        OutlinedTextField(
            value = state.date,
            onValueChange = viewModel::updateDate,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Fecha (yyyy-MM-dd)") },
            singleLine = true,
            enabled = !state.isSubmitting
        )
        OutlinedTextField(
            value = state.startTime,
            onValueChange = viewModel::updateStartTime,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Hora de inicio (HH:mm)") },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = state.endTime,
            onValueChange = viewModel::updateEndTime,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Hora de fin (HH:mm)") },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = state.address,
            onValueChange = viewModel::updateAddress,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Dirección") },
            enabled = !state.isSubmitting
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::updateNotes,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notas (opcional)") },
            enabled = !state.isSubmitting,
            minLines = 2
        )

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = viewModel::submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting && worker.serviceTypes.isNotEmpty()
        ) {
            if (state.isSubmitting) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text("Confirmar reserva")
        }
        Spacer(Modifier.height(8.dp))
    }
}
