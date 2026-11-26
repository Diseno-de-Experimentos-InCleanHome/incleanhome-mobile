package com.incleanhome.mobile.booking.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalTime

data class BookingEstimate(
    val hourlyRate: BigDecimal,
    val hours: BigDecimal,
    val totalAmount: BigDecimal
)

/**
 * Estimated amount of US41. It reproduces the rule of the API so the amount shown matches the
 * amount registered (M28): hours are sent with two decimals (HALF_UP, as the request) and the API
 * stores Math.Round(hourlyRate * hours, 2), whose default .NET mode is banker's rounding.
 */
object BookingEstimator {

    fun hoursBetween(start: LocalTime, end: LocalTime): BigDecimal? {
        if (!end.isAfter(start)) return null
        val minutes = Duration.between(start, end).toMinutes()
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
    }

    fun estimate(hourlyRate: BigDecimal, start: LocalTime, end: LocalTime): BookingEstimate? {
        if (hourlyRate.signum() <= 0) return null
        val hours = hoursBetween(start, end) ?: return null
        return BookingEstimate(hourlyRate, hours, total(hourlyRate, hours))
    }

    fun total(hourlyRate: BigDecimal, hours: BigDecimal): BigDecimal =
        hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_EVEN)

    fun matches(estimated: BigDecimal, registered: BigDecimal): Boolean = estimated.compareTo(registered) == 0
}
