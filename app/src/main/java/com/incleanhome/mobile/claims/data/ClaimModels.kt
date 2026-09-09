package com.incleanhome.mobile.claims.data

data class CreateClaimRequest(
    val type: String,
    val consumerName: String,
    val consumerDocument: String,
    val consumerEmail: String,
    val consumerPhone: String,
    val relatedService: String?,
    val description: String,
    val consumerRequest: String?
)

data class ClaimCreatedResponse(
    val code: String,
    val status: String
)

data class ClaimTrackResponse(
    val code: String,
    val type: String,
    val status: String,
    val createdAt: String?,
    val resolvedAt: String?,
    val adminNote: String?
)

object ClaimType {
    const val CLAIM = "reclamo"
    const val COMPLAINT = "queja"
    val VALUES = setOf(CLAIM, COMPLAINT)
}

object ClaimStatus {
    const val REGISTERED = "registered"
    const val IN_REVIEW = "in_review"
    const val RESOLVED = "resolved"
    const val REJECTED = "rejected"
    val VALUES = setOf(REGISTERED, IN_REVIEW, RESOLVED, REJECTED)
}
