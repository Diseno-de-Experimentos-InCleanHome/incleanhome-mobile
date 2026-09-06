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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.search.data.AvailabilitySlot
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.reviews.presentation.ReviewsColumn
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatTime
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.format.presentationValues

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
        ScreenHeader(stringResource(R.string.title_worker_detail), onBack)
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
                    Text(stringResource(R.string.action_contact))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onBook(worker.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = worker.serviceTypes.isNotEmpty()
                ) {
                    Text(stringResource(R.string.action_book))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))
        Text(stringResource(R.string.title_availability), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isAvailabilityLoading -> CenteredLoading()
            uiState.availabilityErrorMessage != null -> ErrorState(
                message = uiState.availabilityErrorMessage.orEmpty(),
                onRetry = viewModel::loadWorker
            )
            uiState.availability.isEmpty() -> EmptyState(stringResource(R.string.empty_worker_availability))
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.availability.forEach { slot ->
                    AvailabilityCard(slot)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))
        Text(stringResource(R.string.title_my_reviews), style = MaterialTheme.typography.headlineSmall)
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
        worker.phone?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.label_phone, it)) }
        Text(stringResource(R.string.label_age, worker.age))
        Text(stringResource(R.string.label_gender, presentationValue(worker.gender)))
        Text(stringResource(R.string.label_services, presentationValues(worker.serviceTypes)))
        Text(stringResource(R.string.label_zones, worker.zones.joinToString()))
        Text(stringResource(R.string.label_hourly_rate_named, formatCurrency(worker.hourlyRate)))
        Text(stringResource(R.string.label_experience, worker.experienceYears))
        Text(stringResource(R.string.label_bio, worker.bio))
        Text(stringResource(R.string.label_rating, worker.averageRating.toPlainString()))
        Text(stringResource(R.string.label_services_completed, worker.totalServices))
    }
}

@Composable
private fun AvailabilityCard(slot: AvailabilitySlot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(dayName(slot.dayOfWeek), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.label_schedule, "${formatTime(slot.startTime)} – ${formatTime(slot.endTime)}"))
            Text(stringResource(if (slot.isAvailable) R.string.availability_available else R.string.availability_unavailable))
        }
    }
}

@Composable
private fun CenteredLoading() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        LoadingState()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ErrorRetryState(message, onRetry)
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
