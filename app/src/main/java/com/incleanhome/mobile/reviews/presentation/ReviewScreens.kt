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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.reviews.data.Review
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.formatDateTime
import com.incleanhome.mobile.ui.format.presentationValue

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
        ScreenHeader(stringResource(R.string.title_rate_service), onBack)

        when {
            state.isLoadingBooking -> LoadingState()
            state.createdReview != null -> {
                Text(stringResource(R.string.review_sent), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.label_rating, "${state.createdReview?.rating}/5"))
                PrimaryButton(stringResource(R.string.booking_view_mine), onDone)
            }
            state.booking == null -> {
                ErrorRetryState(state.errorMessage.orEmpty(), viewModel::loadBooking)
            }
            else -> state.booking?.let { booking ->
                Text(stringResource(R.string.booking_worker, booking.workerName), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.label_service, presentationValue(booking.serviceType)))
                Text(stringResource(R.string.review_choose_rating))
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
                    label = { Text(stringResource(R.string.review_comment_optional)) },
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
                PrimaryButton(
                    text = stringResource(R.string.review_send),
                    onClick = viewModel::submit,
                    enabled = state.rating in 1..5,
                    loading = state.isSubmitting
                )
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
        ScreenHeader(stringResource(R.string.title_my_reviews), onBack)
        Spacer(Modifier.height(12.dp))
        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_refresh))
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
        isLoading -> LoadingState(modifier)
        errorMessage != null -> ErrorRetryState(errorMessage, onRetry, modifier)
        reviews.isEmpty() -> EmptyState(stringResource(R.string.empty_reviews), modifier)
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
        isLoading -> LoadingState()
        errorMessage != null -> ErrorRetryState(errorMessage, onRetry)
        reviews.isEmpty() -> EmptyState(stringResource(R.string.empty_reviews))
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
                Text(formatDateTime(it), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
