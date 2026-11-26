package com.incleanhome.mobile.reviews.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.reviews.data.Review
import com.incleanhome.mobile.reviews.domain.ReviewHistory
import com.incleanhome.mobile.reviews.domain.ReviewSort
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.format.formatDate
import com.incleanhome.mobile.ui.format.formatDateTime
import com.incleanhome.mobile.ui.format.formatRating
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen

/** Verified reviews and worker history (US34, treatment of EC-BLQ02). */
@Composable
fun VerifiedReviewsSection(
    history: ReviewHistory,
    arrangedReviews: List<Review>,
    sort: ReviewSort,
    serviceFilter: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onSortChange: (ReviewSort) -> Unit,
    onServiceFilterChange: (String?) -> Unit
) {
    when {
        isLoading -> LoadingState()
        errorMessage != null -> ErrorRetryState(errorMessage, onRetry)
        history.verifiedCount == 0 -> EmptyState(stringResource(R.string.verified_reviews_empty))
        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReviewSummaryCard(history)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = sort == ReviewSort.NEWEST,
                    onClick = { onSortChange(ReviewSort.NEWEST) },
                    label = { Text(stringResource(R.string.reviews_sort_newest)) }
                )
                FilterChip(
                    selected = sort == ReviewSort.OLDEST,
                    onClick = { onSortChange(ReviewSort.OLDEST) },
                    label = { Text(stringResource(R.string.reviews_sort_oldest)) }
                )
            }
            if (history.serviceTypes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = serviceFilter == null,
                        onClick = { onServiceFilterChange(null) },
                        label = { Text(stringResource(R.string.all_services)) }
                    )
                    history.serviceTypes.forEach { service ->
                        FilterChip(
                            selected = serviceFilter == service,
                            onClick = { onServiceFilterChange(service) },
                            label = { Text(presentationValue(service)) }
                        )
                    }
                }
            }
            arrangedReviews.forEach { review -> VerifiedReviewCard(review) }
        }
    }
}

@Composable
private fun ReviewSummaryCard(history: ReviewHistory) {
    InCleanHomeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = history.averageRating?.let(::formatRating).orEmpty(),
                style = MaterialTheme.typography.displaySmall,
                color = Navy
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.verified_reviews_average), style = MaterialTheme.typography.titleSmall, color = Navy)
                Text(
                    pluralStringResource(R.plurals.verified_reviews_count, history.verifiedCount, history.verifiedCount),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    pluralStringResource(R.plurals.services_completed, history.completedServices, history.completedServices),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        history.distribution.forEach { (stars, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.reviews_stars_label, stars), modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { if (history.verifiedCount == 0) 0f else count.toFloat() / history.verifiedCount },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = PrimaryGreen
                )
                Text(count.toString(), modifier = Modifier.width(32.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun VerifiedReviewCard(review: Review) {
    InCleanHomeCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Verified, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.verified_review_badge), style = MaterialTheme.typography.labelLarge, color = PrimaryGreen)
            }
            Text("${review.rating}/5", style = MaterialTheme.typography.titleLarge)
            Text(review.clientName, style = MaterialTheme.typography.titleMedium)
            val service = review.serviceType?.takeIf(String::isNotBlank)
            val serviceDate = review.serviceDate?.takeIf(String::isNotBlank)
            if (service != null || serviceDate != null) {
                Text(
                    stringResource(
                        R.string.verified_review_service,
                        service?.let { presentationValue(it) } ?: stringResource(R.string.unknown_value),
                        serviceDate?.let(::formatDate) ?: stringResource(R.string.unknown_value)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (review.comment.isNotBlank()) Text(review.comment)
            review.createdAt?.let {
                Text(stringResource(R.string.verified_review_published, formatDateTime(it)), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
