package com.incleanhome.mobile.booking.data

import java.math.BigDecimal

data class CreateBookingRequest(
    val workerId: Int,
    val serviceType: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val hours: BigDecimal,
    val address: String,
    val notes: String?,
    /** suggestion, filters or direct: only measured by the API, never stored (US33, Escenario 4). */
    val origin: String? = null
)

data class UpdateBookingStatusRequest(val status: String)

/** Body of POST bookings/estimate (US41): the same schedule fields as the booking. */
data class BookingEstimateRequest(
    val workerId: Int,
    val serviceType: String,
    val date: String,
    val startTime: String,
    val endTime: String
)

/** Estimate computed by the API with the worker's current rate; nothing is stored. */
data class BookingEstimateResponse(
    val hourlyRate: BigDecimal,
    val hours: BigDecimal,
    val totalAmount: BigDecimal
)

data class Booking(
    val id: Int,
    val clientId: Int,
    val workerId: Int,
    val clientName: String,
    val workerName: String,
    val serviceType: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val hours: BigDecimal,
    val address: String,
    val notes: String,
    val hourlyRate: BigDecimal,
    val totalAmount: BigDecimal,
    val status: String,
    val hasReview: Boolean,
    val createdAt: String?
)

object BookingStatus {
    const val PENDING = "pending"
    const val ACCEPTED = "accepted"
    const val REJECTED = "rejected"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
}
