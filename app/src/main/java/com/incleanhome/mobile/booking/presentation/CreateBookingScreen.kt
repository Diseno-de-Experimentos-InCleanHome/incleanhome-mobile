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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.formatDateRange
import com.incleanhome.mobile.ui.format.presentationValue

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
        ScreenHeader(stringResource(R.string.title_new_booking), onBack)

        if (state.isLoadingWorker) {
            LoadingState()
            return@Column
        }

        state.createdBooking?.let { booking ->
            Text(stringResource(R.string.booking_created), style = MaterialTheme.typography.headlineSmall)
            Text("${booking.workerName} · ${presentationValue(booking.serviceType)}")
            Text(formatDateRange(booking.date, booking.startTime, booking.endTime))
            Text(stringResource(R.string.label_status, presentationValue(booking.status)))
            PrimaryButton(stringResource(R.string.booking_view_mine), onViewBookings)
            return@Column
        }

        val worker = state.worker
        if (worker == null) {
            ErrorRetryState(state.errorMessage.orEmpty(), viewModel::loadWorker)
            return@Column
        }

        Text(stringResource(R.string.booking_worker, worker.name), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.booking_choose_service))
        worker.serviceTypes.forEach { service ->
            FilterChip(
                selected = state.selectedService == service,
                onClick = { viewModel.selectService(service) },
                label = { Text(presentationValue(service)) },
                enabled = !state.isSubmitting
            )
        }
        if (worker.serviceTypes.isEmpty()) {
            Text(stringResource(R.string.booking_no_worker_services))
        }

        OutlinedTextField(
            value = state.date,
            onValueChange = viewModel::updateDate,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_date_iso)) },
            singleLine = true,
            enabled = !state.isSubmitting
        )
        OutlinedTextField(
            value = state.startTime,
            onValueChange = viewModel::updateStartTime,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_start_time)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = state.endTime,
            onValueChange = viewModel::updateEndTime,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_end_time)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = state.address,
            onValueChange = viewModel::updateAddress,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_address)) },
            enabled = !state.isSubmitting
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::updateNotes,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_notes_optional)) },
            enabled = !state.isSubmitting,
            minLines = 2
        )

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        PrimaryButton(
            text = stringResource(R.string.booking_confirm),
            onClick = viewModel::submit,
            enabled = worker.serviceTypes.isNotEmpty(),
            loading = state.isSubmitting
        )
        Spacer(Modifier.height(8.dp))
    }
}
