package com.incleanhome.mobile.booking.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatDate
import com.incleanhome.mobile.ui.format.formatDateRange
import com.incleanhome.mobile.ui.format.formatTime
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.theme.Navy

@Composable
fun CreateBookingScreen(
    viewModel: CreateBookingViewModel,
    onBack: () -> Unit,
    onViewBookings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    ScreenBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(stringResource(R.string.title_new_booking), onBack)

            if (state.isLoadingWorker) {
                LoadingState(Modifier.padding(vertical = 32.dp))
                return@Column
            }

            state.createdBooking?.let { booking ->
                InCleanHomeCard {
                    Text(
                        stringResource(R.string.booking_created),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Navy
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("${booking.workerName} · ${presentationValue(booking.serviceType)}")
                    Text(formatDateRange(booking.date, booking.startTime, booking.endTime))
                    Text(stringResource(R.string.label_status, presentationValue(booking.status)))
                }
                PrimaryButton(stringResource(R.string.booking_view_mine), onViewBookings)
                return@Column
            }

            val worker = state.worker
            if (worker == null) {
                ErrorRetryState(state.errorMessage.orEmpty(), viewModel::loadWorker)
                return@Column
            }

            BookingSection(stringResource(R.string.booking_worker_section)) {
                InCleanHomeCard {
                    Text(worker.name, style = MaterialTheme.typography.titleLarge, color = Navy)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.worker_rate_value, formatCurrency(worker.hourlyRate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BookingSection(stringResource(R.string.booking_service_section)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    worker.serviceTypes.forEach { service ->
                        FilterChip(
                            selected = state.selectedService == service,
                            onClick = { viewModel.selectService(service) },
                            label = { Text(presentationValue(service)) },
                            enabled = !state.isSubmitting
                        )
                    }
                }
                if (worker.serviceTypes.isEmpty()) {
                    Text(stringResource(R.string.booking_no_worker_services))
                }
            }

            BookingSection(stringResource(R.string.booking_schedule_section)) {
                InCleanHomeTextField(
                    value = state.date,
                    onValueChange = viewModel::updateDate,
                    label = stringResource(R.string.field_date_iso),
                    enabled = !state.isSubmitting
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InCleanHomeTextField(
                        value = state.startTime,
                        onValueChange = viewModel::updateStartTime,
                        label = stringResource(R.string.field_start_time),
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    InCleanHomeTextField(
                        value = state.endTime,
                        onValueChange = viewModel::updateEndTime,
                        label = stringResource(R.string.field_end_time),
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            BookingSection(stringResource(R.string.booking_address_section)) {
                InCleanHomeTextField(
                    value = state.address,
                    onValueChange = viewModel::updateAddress,
                    label = stringResource(R.string.field_address),
                    enabled = !state.isSubmitting
                )
                Spacer(Modifier.height(12.dp))
                InCleanHomeTextField(
                    value = state.notes,
                    onValueChange = viewModel::updateNotes,
                    label = stringResource(R.string.field_notes_optional),
                    enabled = !state.isSubmitting,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 5
                )
            }

            Text(stringResource(R.string.booking_summary), style = MaterialTheme.typography.titleMedium, color = Navy)
            InCleanHomeCard {
                BookingSummaryRow(
                    stringResource(R.string.booking_summary_service),
                    presentationValue(state.selectedService)
                )
                BookingSummaryRow(
                    stringResource(R.string.booking_summary_schedule),
                    "${formatDate(state.date)} · ${formatTime(state.startTime)} – ${formatTime(state.endTime)}"
                )
                BookingSummaryRow(
                    stringResource(R.string.booking_summary_rate),
                    stringResource(R.string.worker_rate_value, formatCurrency(worker.hourlyRate))
                )
            }

            state.errorMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            PrimaryButton(
                text = stringResource(R.string.booking_confirm),
                onClick = viewModel::submit,
                enabled = worker.serviceTypes.isNotEmpty() && !state.isSubmitting,
                loading = state.isSubmitting
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BookingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Navy)
        content()
    }
}

@Composable
private fun BookingSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodyMedium,
            color = Navy
        )
    }
    Spacer(Modifier.height(10.dp))
}
