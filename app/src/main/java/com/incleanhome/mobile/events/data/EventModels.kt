package com.incleanhome.mobile.events.data

import java.math.BigDecimal

data class CreateEventRequest(
    val title: String, val description: String?, val serviceTypes: List<String>,
    val zone: String, val address: String, val date: String, val startTime: String,
    val endTime: String, val hours: BigDecimal, val workersNeeded: Int,
    val hourlyRateOffered: BigDecimal, val applicationDeadline: String
)

data class ApplyToEventRequest(val message: String?)

data class Event(
    val id: Int, val clientId: Int, val clientName: String, val title: String,
    val description: String, val serviceTypes: List<String>, val zone: String,
    val address: String, val date: String, val startTime: String, val endTime: String,
    val hours: BigDecimal, val workersNeeded: Int, val acceptedCount: Int,
    val hourlyRateOffered: BigDecimal, val applicationDeadline: String,
    val status: String, val myApplicationStatus: String?, val createdAt: String?
)

data class EventApplication(
    val id: Int, val eventId: Int, val eventTitle: String, val eventDate: String,
    val eventZone: String, val eventStatus: String, val workerId: Int,
    val workerName: String, val message: String?, val status: String,
    val createdAt: String?
)

object EventStatus {
    const val OPEN = "open"
    const val STAFFED = "staffed"
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}

object ApplicationStatus {
    const val PENDING = "pending"
    const val ACCEPTED = "accepted"
    const val REJECTED = "rejected"
    const val WITHDRAWN = "withdrawn"
}
