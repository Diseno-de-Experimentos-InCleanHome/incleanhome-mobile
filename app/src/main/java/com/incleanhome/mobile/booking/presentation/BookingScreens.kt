package com.incleanhome.mobile.booking.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingStatus
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatDateRange
import com.incleanhome.mobile.ui.format.presentationValue

@Composable
fun MyBookingsScreen(
    viewModel: BookingListViewModel,
    onBack: () -> Unit,
    onReviewClick: (Int) -> Unit,
    onCancelClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) = BookingListScreen(
    title = stringResource(R.string.title_my_bookings),
    workerView = false,
    viewModel = viewModel,
    onBack = onBack,
    onBookingClick = {},
    onReviewClick = onReviewClick,
    onCancelClick = onCancelClick,
    modifier = modifier
)

@Composable
fun WorkerRequestsScreen(
    viewModel: BookingListViewModel,
    onBack: () -> Unit,
    onBookingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) = BookingListScreen(
    title = stringResource(R.string.title_booking_requests),
    workerView = true,
    viewModel = viewModel,
    onBack = onBack,
    onBookingClick = onBookingClick,
    onReviewClick = {},
    onCancelClick = {},
    modifier = modifier
)

@Composable
private fun BookingListScreen(
    title: String,
    workerView: Boolean,
    viewModel: BookingListViewModel,
    onBack: () -> Unit,
    onBookingClick: (Int) -> Unit,
    onReviewClick: (Int) -> Unit,
    onCancelClick: (Int) -> Unit,
    modifier: Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var cancellationTarget by remember { mutableStateOf<Booking?>(null) }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(title, onBack)
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_refresh))
        }
        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorRetryState(state.errorMessage.orEmpty(), viewModel::refresh)
            state.bookings.isEmpty() -> EmptyState(
                stringResource(if (workerView) R.string.empty_booking_requests else R.string.empty_bookings)
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.bookings, key = { it.id }) { booking ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BookingCard(
                            booking = booking,
                            counterpart = if (workerView) booking.clientName else booking.workerName,
                            onClick = if (workerView) ({ onBookingClick(booking.id) }) else null
                        )
                        if (!workerView && booking.status == BookingStatus.COMPLETED) {
                            if (booking.hasReview) {
                                Text(stringResource(R.string.booking_reviewed), color = MaterialTheme.colorScheme.primary)
                            } else {
                                Button(
                                    onClick = { onReviewClick(booking.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.booking_rate_service))
                                }
                            }
                        }
                        if (!workerView && (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.ACCEPTED)) {
                            Button(onClick = { cancellationTarget = booking }, enabled = state.updatingBookingId == null, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.booking_cancel)) }
                        }
                    }
                }
            }
        }
        state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
    cancellationTarget?.let { booking -> AlertDialog(onDismissRequest={cancellationTarget=null}, title={Text(stringResource(R.string.booking_cancel))}, text={Text(stringResource(R.string.booking_cancel_question))}, confirmButton={TextButton({onCancelClick(booking.id);cancellationTarget=null}){Text(stringResource(R.string.action_confirm))}}, dismissButton={TextButton({cancellationTarget=null}){Text(stringResource(R.string.action_back))}}) }
}

@Composable
fun WorkerBookingDetailScreen(
    viewModel: BookingDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(stringResource(R.string.title_booking_detail), onBack)
        when {
            state.isLoading -> LoadingState()
            state.booking == null -> {
                ErrorRetryState(state.errorMessage.orEmpty(), viewModel::refresh)
            }
            else -> state.booking?.let { booking ->
                BookingCard(booking, booking.clientName, null)
                Text(stringResource(R.string.label_address, booking.address))
                if (booking.notes.isNotBlank()) Text(stringResource(R.string.label_notes, booking.notes))
                Text(stringResource(R.string.label_duration_hours, booking.hours.toPlainString()))
                Text(stringResource(R.string.label_reference_amount, formatCurrency(booking.totalAmount)))

                if (booking.status == BookingStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.updateStatus(BookingStatus.ACCEPTED) },
                            enabled = !state.isUpdating,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.action_accept)) }
                        Button(
                            onClick = { viewModel.updateStatus(BookingStatus.REJECTED) },
                            enabled = !state.isUpdating,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.action_reject)) }
                    }
                }
                if (booking.status == BookingStatus.ACCEPTED) {
                    Button(
                        onClick = { viewModel.updateStatus(BookingStatus.COMPLETED) },
                        enabled = !state.isUpdating,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.booking_mark_completed)) }
                }
                if (state.isUpdating) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    counterpart: String,
    onClick: (() -> Unit)?
) {
    val cardModifier = Modifier.fillMaxWidth().let {
        if (onClick == null) it else it.semantics { role = Role.Button }.clickable(onClick = onClick)
    }
    Card(modifier = cardModifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(counterpart, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.label_service, presentationValue(booking.serviceType)))
            Text(formatDateRange(booking.date, booking.startTime, booking.endTime))
            Text(stringResource(R.string.label_status, presentationValue(booking.status)))
        }
    }
}
