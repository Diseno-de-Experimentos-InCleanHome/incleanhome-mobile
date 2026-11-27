package com.incleanhome.mobile.reviews.domain

import com.incleanhome.mobile.reviews.data.Review
import com.incleanhome.mobile.reviews.data.WorkerReviewSummary
import java.math.BigDecimal
import java.math.RoundingMode

enum class ReviewSort { NEWEST, OLDEST }

/**
 * Worker review history of US34. The API marks each review with isVerified (completed booking of the
 * same client and worker) and computes the summary; the app only falls back to its own rule and
 * figures when the summary cannot be loaded, and then it does not show completed services.
 */
data class ReviewHistory(
    val verifiedReviews: List<Review>,
    val averageRating: BigDecimal?,
    val distribution: Map<Int, Int>,
    /** Completed bookings from the API summary; null when the summary is not available. */
    val completedServices: Int?,
    val serviceTypes: List<String>
) {
    val verifiedCount: Int get() = verifiedReviews.size

    companion object {
        fun isVerified(review: Review): Boolean =
            review.isVerified ?: (review.bookingId > 0 && review.rating in 1..5)

        fun from(reviews: List<Review>, summary: WorkerReviewSummary?): ReviewHistory {
            val verified = reviews.filter(::isVerified)
            val localAverage = verified.takeIf { it.isNotEmpty() }
                ?.let { list ->
                    BigDecimal(list.sumOf { it.rating }).divide(BigDecimal(list.size), 2, RoundingMode.HALF_EVEN)
                }
            val localDistribution = (5 downTo 1).associateWith { stars -> verified.count { it.rating == stars } }
            return ReviewHistory(
                verifiedReviews = verified,
                averageRating = if (summary != null) summary.averageRating else localAverage,
                distribution = summary?.distribution
                    ?.let { remote -> (5 downTo 1).associateWith { stars -> remote[stars.toString()] ?: 0 } }
                    ?: localDistribution,
                completedServices = summary?.completedServices,
                serviceTypes = verified.mapNotNull { it.serviceType?.takeIf(String::isNotBlank) }
                    .distinctBy(String::lowercase)
                    .sorted()
            )
        }

        /**
         * Sort by publication date and optionally keep one service type, with the same rules as the
         * sort and serviceType parameters of GET reviews/worker/{id} (case-insensitive service).
         */
        fun arrange(reviews: List<Review>, sort: ReviewSort, serviceType: String?): List<Review> {
            val filtered = if (serviceType.isNullOrBlank()) reviews else {
                reviews.filter { it.serviceType.equals(serviceType, ignoreCase = true) }
            }
            val byDate = filtered.sortedBy { it.createdAt.orEmpty() }
            return if (sort == ReviewSort.NEWEST) byDate.reversed() else byDate
        }
    }
}
