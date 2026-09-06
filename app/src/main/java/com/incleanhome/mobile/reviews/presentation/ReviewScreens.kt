package com.incleanhome.mobile.reviews.presentation

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.reviews.data.Review
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CreateReviewScreen(
    viewModel: CreateReviewViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReviewHeader("Calificar servicio", onBack)

        when {
            state.isLoadingBooking -> CircularProgressIndicator(
                Modifier.align(Alignment.CenterHorizontally)
            )
            state.createdReview != null -> {
                Text("Reseña enviada correctamente", style = MaterialTheme.typography.headlineSmall)
                Text("Calificación: ${state.createdReview?.rating}/5")
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver a Mis reservas")
                }
            }
            state.booking == null -> {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::loadBooking) { Text("Reintentar") }
            }
            else -> state.booking?.let { booking ->
                Text("Trabajador: ${booking.workerName}", style = MaterialTheme.typography.titleLarge)
                Text("Servicio: ${booking.serviceType}")
                Text("Selecciona una calificación de 1 a 5:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (CreateReviewViewModel.MIN_RATING..CreateReviewViewModel.MAX_RATING).forEach { rating ->
                        FilterChip(
                            selected = state.rating == rating,
                            onClick = { viewModel.selectRating(rating) },
                            label = { Text(rating.toString()) },
                            enabled = !state.isSubmitting
                        )
                    }
                }
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = viewModel::updateComment,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Comentario (opcional)") },
                    supportingText = {
                        Text("${state.comment.length}/${CreateReviewViewModel.MAX_COMMENT_LENGTH}")
                    },
                    minLines = 3,
                    maxLines = 6,
                    enabled = !state.isSubmitting
                )
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting && state.rating in 1..5
                ) {
                    if (state.isSubmitting) CircularProgressIndicator(strokeWidth = 2.dp)
                    else Text("Enviar reseña")
                }
            }
        }
    }
}

@Composable
fun WorkerReviewsScreen(
    viewModel: WorkerReviewsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        ReviewHeader("Mis reseñas", onBack)
        Spacer(Modifier.height(12.dp))
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar")
        }
        Spacer(Modifier.height(12.dp))
        ReviewsList(
            reviews = state.reviews,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            onRetry = viewModel::refresh,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ReviewsList(
    reviews: List<Review>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) { CircularProgressIndicator() }
        errorMessage != null -> Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) { Text("Reintentar") }
        }
        reviews.isEmpty() -> Text("Este trabajador aún no tiene reseñas.", modifier = modifier)
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reviews, key = Review::id) { review -> ReviewCard(review) }
        }
    }
}

@Composable
fun ReviewsColumn(
    reviews: List<Review>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) { CircularProgressIndicator() }
        errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) { Text("Reintentar") }
        }
        reviews.isEmpty() -> Text("Este trabajador aún no tiene reseñas.")
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            reviews.forEach { review -> ReviewCard(review) }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${review.rating}/5", style = MaterialTheme.typography.titleLarge)
            Text(review.clientName, style = MaterialTheme.typography.titleMedium)
            if (review.comment.isNotBlank()) Text(review.comment)
            review.createdAt?.let {
                Text(formatReviewDate(it), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReviewHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onBack) { Text("Volver") }
        Text(
            title,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

private fun formatReviewDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(REVIEW_DATE_FORMAT)
}.getOrDefault(value)

private val REVIEW_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
