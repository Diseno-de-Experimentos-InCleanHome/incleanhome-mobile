package com.incleanhome.mobile.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.search.data.AvailabilitySlot
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.reviews.presentation.ReviewsColumn

@Composable
fun WorkerDetailScreen(
    viewModel: WorkerDetailViewModel,
    onBack: () -> Unit,
    onBook: (Int) -> Unit,
    onContact: (Int, String) -> Unit,
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
                text = "Detalle del trabajador",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        when {
            uiState.isWorkerLoading -> CenteredLoading()
            uiState.workerErrorMessage != null -> ErrorState(
                message = uiState.workerErrorMessage.orEmpty(),
                onRetry = viewModel::loadWorker
            )
            uiState.worker != null -> uiState.worker?.let { worker ->
                WorkerProfile(worker = worker)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onContact(worker.id, worker.name) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Contactar")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onBook(worker.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = worker.serviceTypes.isNotEmpty()
                ) {
                    Text("Reservar")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))
        Text("Disponibilidad", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isAvailabilityLoading -> CenteredLoading()
            uiState.availabilityErrorMessage != null -> ErrorState(
                message = uiState.availabilityErrorMessage.orEmpty(),
                onRetry = viewModel::loadWorker
            )
            uiState.availability.isEmpty() -> Text("Este trabajador no tiene disponibilidad registrada.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.availability.forEach { slot ->
                    AvailabilityCard(slot)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))
        Text("Reseñas", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        ReviewsColumn(
            reviews = uiState.reviews,
            isLoading = uiState.isReviewsLoading,
            errorMessage = uiState.reviewsErrorMessage,
            onRetry = viewModel::loadWorker
        )
    }
}

@Composable
private fun WorkerProfile(worker: Worker) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(worker.name, style = MaterialTheme.typography.headlineMedium)
        worker.phone?.takeIf(String::isNotBlank)?.let { Text("Teléfono: $it") }
        Text("Edad: ${worker.age}")
        Text("Género: ${worker.gender}")
        Text("Servicios: ${worker.serviceTypes.joinToString()}")
        Text("Zonas: ${worker.zones.joinToString()}")
        Text("Tarifa por hora: ${worker.hourlyRate.toPlainString()}")
        Text("Experiencia: ${worker.experienceYears} años")
        Text("Biografía: ${worker.bio}")
        Text("Calificación: ${worker.averageRating.toPlainString()}")
        Text("Servicios realizados: ${worker.totalServices}")
    }
}

@Composable
private fun AvailabilityCard(slot: AvailabilitySlot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(dayName(slot.dayOfWeek), style = MaterialTheme.typography.titleMedium)
            Text("Horario: ${slot.startTime} - ${slot.endTime}")
            Text(if (slot.isAvailable) "Disponible" else "No disponible")
        }
    }
}

@Composable
private fun CenteredLoading() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

private fun dayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        0 -> "Domingo"
        1 -> "Lunes"
        2 -> "Martes"
        3 -> "Miércoles"
        4 -> "Jueves"
        5 -> "Viernes"
        6 -> "Sábado"
        else -> dayOfWeek.toString()
    }
}
