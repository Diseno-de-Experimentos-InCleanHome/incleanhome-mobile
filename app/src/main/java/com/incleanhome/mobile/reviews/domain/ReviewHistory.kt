package com.incleanhome.mobile.reviews.domain

import com.incleanhome.mobile.reviews.data.Review
import java.math.BigDecimal
import java.math.RoundingMode

enum class ReviewSort { NEWEST, OLDEST }

/**
 * Worker review history of US34. A review is verified when it is linked to a booking: the API only
 * accepts a review for a booking of the client in status completed (ReviewCommandService), so every
 * review it returns with a bookingId comes from a completed service on the platform.
 */
data class ReviewHistory(
    val verifiedReviews: List<Review>,
    val averageRating: BigDecimal?,
    val distribution: Map<Int, Int>,
    val completedServices: Int,
    val serviceTypes: List<String>
) {
    val verifiedCount: Int get() = verifiedReviews.size

    companion object {
        fun isVerified(review: Review): Boolean = review.bookingId > 0 && review.rating in 1..5

        fun from(reviews: List<Review>, completedServices: Int): ReviewHistory {
            val verified = reviews.filter(::isVerified)
            val average = verified.takeIf { it.isNotEmpty() }
                ?.let { list ->
                    BigDecimal(list.sumOf { it.rating }).divide(BigDecimal(list.size), 1, RoundingMode.HALF_UP)
                }
            return ReviewHistory(
                verifiedReviews = verified,
                averageRating = average,
                distribution = (5 downTo 1).associateWith { stars -> verified.count { it.rating == stars } },
                completedServices = completedServices,
                serviceTypes = verified.mapNotNull { it.serviceType?.takeIf(String::isNotBlank) }.distinct().sorted()
            )
        }

        /** Sort by publication date and optionally keep one service type (dates are ISO-8601). */
        fun arrange(reviews: List<Review>, sort: ReviewSort, serviceType: String?): List<Review> {
            val filtered = if (serviceType.isNullOrBlank()) reviews else reviews.filter { it.serviceType == serviceType }
            val byDate = filtered.sortedBy { it.createdAt.orEmpty() }
            return if (sort == ReviewSort.NEWEST) byDate.reversed() else byDate
        }
    }
}
