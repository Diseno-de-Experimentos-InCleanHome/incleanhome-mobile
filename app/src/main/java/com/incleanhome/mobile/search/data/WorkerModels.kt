package com.incleanhome.mobile.search.data

import java.math.BigDecimal

data class Worker(
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

data class AvailabilitySlot(
    val id: Int,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean
)
