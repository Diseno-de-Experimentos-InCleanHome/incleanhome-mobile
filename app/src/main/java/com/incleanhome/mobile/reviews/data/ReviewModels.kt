package com.incleanhome.mobile.reviews.data

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
    val comment: String,
    val createdAt: String?
)
