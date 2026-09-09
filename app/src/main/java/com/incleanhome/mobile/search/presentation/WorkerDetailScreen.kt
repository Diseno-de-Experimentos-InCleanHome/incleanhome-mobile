package com.incleanhome.mobile.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.localizedDayOfWeek
import com.incleanhome.mobile.ui.format.formatRating
import com.incleanhome.mobile.ui.format.formatTime
import com.incleanhome.mobile.ui.format.humanizeIdentifier
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.theme.GreenLight
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen

@Composable
fun WorkerDetailScreen(
    viewModel: WorkerDetailViewModel,
    onBack: () -> Unit,
    onBook: (Int) -> Unit,
    onContact: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ScreenBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ScreenHeader(stringResource(R.string.worker_profile_title), onBack)
            Spacer(Modifier.height(20.dp))

            when {
                uiState.isWorkerLoading -> CenteredLoading()
                uiState.workerErrorMessage != null -> ErrorState(
                    message = uiState.workerErrorMessage.orEmpty(),
                    onRetry = viewModel::loadWorker
                )
                uiState.worker != null -> uiState.worker?.let { worker ->
                    WorkerProfile(worker)
                    SectionDivider()
                    SectionTitle(stringResource(R.string.worker_availability))
                    Spacer(Modifier.height(12.dp))
                    when {
                        uiState.isAvailabilityLoading -> CenteredLoading()
                        uiState.availabilityErrorMessage != null -> ErrorState(
                            message = uiState.availabilityErrorMessage.orEmpty(),
                            onRetry = viewModel::loadWorker
                        )
                        uiState.availability.isEmpty() -> EmptyState(
                            stringResource(R.string.empty_worker_availability)
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.availability.forEach { slot -> AvailabilityCard(slot) }
                        }
                    }

                    SectionDivider()
                    SectionTitle(stringResource(R.string.worker_reviews))
                    Spacer(Modifier.height(12.dp))
                    ReviewsColumn(
                        reviews = uiState.reviews,
                        isLoading = uiState.isReviewsLoading,
                        errorMessage = uiState.reviewsErrorMessage,
                        onRetry = viewModel::loadWorker
                    )
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton(
                        text = stringResource(R.string.worker_book_service),
                        onClick = { onBook(worker.id) },
                        enabled = worker.serviceTypes.isNotEmpty()
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(
                        text = stringResource(R.string.action_contact),
                        onClick = { onContact(worker.id, worker.name) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WorkerProfile(worker: Worker) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(worker.name, style = MaterialTheme.typography.headlineMedium, color = Navy)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = PrimaryGreen)
            Text(
                text = "${formatRating(worker.averageRating)} · " +
                    androidx.compose.ui.res.pluralStringResource(
                        R.plurals.services_completed,
                        worker.totalServices,
                        worker.totalServices
                    ),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            worker.serviceTypes.forEach { service -> ServiceTag(presentationValue(service)) }
        }
        InCleanHomeCard {
            Text(
                text = stringResource(R.string.worker_rate_value, formatCurrency(worker.hourlyRate)),
                style = MaterialTheme.typography.titleLarge,
                color = Navy
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.worker_experience_value, worker.experienceYears))
            Text(
                text = "${stringResource(R.string.worker_age_value, worker.age)} · ${presentationValue(worker.gender)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (worker.bio.isNotBlank()) {
            SectionTitle(stringResource(R.string.worker_about))
            Text(worker.bio, style = MaterialTheme.typography.bodyLarge)
        }
        SectionTitle(stringResource(R.string.worker_work_zones))
        if (worker.zones.isEmpty()) {
            EmptyState(stringResource(R.string.unknown_value))
        } else {
            Text(
                text = worker.zones.joinToString(" · ") { humanizeIdentifier(it) },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AvailabilityCard(slot: AvailabilitySlot) {
    InCleanHomeCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(localizedDayOfWeek(slot.dayOfWeek), style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(
                    stringResource(
                        R.string.availability_time_range,
                        formatTime(slot.startTime),
                        formatTime(slot.endTime)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                if (slot.isAvailable) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                contentDescription = null,
                tint = if (slot.isAvailable) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(if (slot.isAvailable) R.string.availability_available else R.string.availability_unavailable),
                style = MaterialTheme.typography.labelMedium,
                color = if (slot.isAvailable) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ServiceTag(label: String) {
    Surface(color = GreenLight, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Navy
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Navy)
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(20.dp))
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
