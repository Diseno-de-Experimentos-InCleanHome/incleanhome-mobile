package com.incleanhome.mobile.worker.data

import java.math.BigDecimal

data class WorkerProfile(
    val id: Int,
    val name: String,
    val phone: String?,
    val age: Int,
    val gender: String,
    val serviceTypes: List<String>,
    val zones: List<String>,
    val hourlyRate: BigDecimal,
    val experienceYears: Int,
    val bio: String,
    val averageRating: BigDecimal,
    val totalServices: Int
)

data class WorkerAvailabilitySlot(
    val id: Int,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean
)

data class AvailabilitySlotInput(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean
)

data class ReplaceAvailabilityRequest(
    val slots: List<AvailabilitySlotInput>
)
