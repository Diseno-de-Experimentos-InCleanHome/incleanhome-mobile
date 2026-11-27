package com.incleanhome.mobile.reviews.data

import java.math.BigDecimal

data class CreateReviewRequest(
    val bookingId: Int,
    val workerId: Int,
    val rating: Int,
    val comment: String?
)

data class Review(
    val id: Int,
    val bookingId: Int,
    val clientId: Int,
    val workerId: Int,
    val clientName: String,
    val rating: Int,
    /** Reviews published before US34 may come without text; Gson can leave it null at runtime. */
    val comment: String?,
    val createdAt: String?,
    /** Service type and date of the reviewed booking (US34, GET reviews/worker/{id}). */
    val serviceType: String? = null,
    val serviceDate: String? = null,
    /** Set by the API: the review belongs to a completed booking of the same client and worker. */
    val isVerified: Boolean? = null
)

/** Response of GET reviews/worker/{id}/summary (US34). [averageRating] is null without verified reviews. */
data class WorkerReviewSummary(
    val workerId: Int,
    val verifiedReviewCount: Int,
    val averageRating: BigDecimal?,
    val distribution: Map<String, Int>?,
    /** Completed bookings, reviewed or not (the profile's totalServices counts reviews). */
    val completedServices: Int,
    val message: String?
)
