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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingStatus

@Composable
fun MyBookingsScreen(
    viewModel: BookingListViewModel,
    onBack: () -> Unit,
    onReviewClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) = BookingListScreen(
    title = "Mis reservas",
    workerView = false,
    viewModel = viewModel,
    onBack = onBack,
    onBookingClick = {},
    onReviewClick = onReviewClick,
    modifier = modifier
)

@Composable
fun WorkerRequestsScreen(
    viewModel: BookingListViewModel,
    onBack: () -> Unit,
    onBookingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) = BookingListScreen(
    title = "Solicitudes",
    workerView = true,
    viewModel = viewModel,
    onBack = onBack,
    onBookingClick = onBookingClick,
    onReviewClick = {},
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
    modifier: Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Header(title, onBack)
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar")
        }
        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::refresh) { Text("Reintentar") }
            }
            state.bookings.isEmpty() -> Text(
                if (workerView) "No tienes solicitudes recibidas." else "Aún no tienes reservas."
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
                                Text("Servicio calificado", color = MaterialTheme.colorScheme.primary)
                            } else {
                                Button(
                                    onClick = { onReviewClick(booking.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Calificar servicio")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
        Header("Detalle de solicitud", onBack)
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.booking == null -> {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::refresh) { Text("Reintentar") }
            }
            else -> state.booking?.let { booking ->
                BookingCard(booking, booking.clientName, null)
                Text("Dirección: ${booking.address}")
                if (booking.notes.isNotBlank()) Text("Notas: ${booking.notes}")
                Text("Duración: ${booking.hours.toPlainString()} horas")
                Text("Monto referencial: ${booking.totalAmount.toPlainString()}")

                if (booking.status == BookingStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.updateStatus(BookingStatus.ACCEPTED) },
                            enabled = !state.isUpdating,
                            modifier = Modifier.weight(1f)
                        ) { Text("Aceptar") }
                        Button(
                            onClick = { viewModel.updateStatus(BookingStatus.REJECTED) },
                            enabled = !state.isUpdating,
                            modifier = Modifier.weight(1f)
                        ) { Text("Rechazar") }
                    }
                }
                if (booking.status == BookingStatus.ACCEPTED) {
                    Button(
                        onClick = { viewModel.updateStatus(BookingStatus.COMPLETED) },
                        enabled = !state.isUpdating,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Marcar como completado") }
                }
                if (state.isUpdating) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onBack) { Text("Volver") }
        Text(
            title,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    counterpart: String,
    onClick: (() -> Unit)?
) {
    val cardModifier = Modifier.fillMaxWidth().let {
        if (onClick == null) it else it.clickable(onClick = onClick)
    }
    Card(modifier = cardModifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(counterpart, style = MaterialTheme.typography.titleMedium)
            Text("Servicio: ${booking.serviceType}")
            Text("${booking.date}, ${booking.startTime} - ${booking.endTime}")
            Text("Estado: ${booking.status}")
        }
    }
}
