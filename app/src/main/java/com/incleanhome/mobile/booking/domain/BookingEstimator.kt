package com.incleanhome.mobile.booking.domain

import com.incleanhome.mobile.booking.data.BookingEstimateResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class BookingEstimate(
    val hourlyRate: BigDecimal,
    val hours: BigDecimal,
    val totalAmount: BigDecimal,
    /** True when the amount comes from POST bookings/estimate (current rate of the worker). */
    val fromApi: Boolean = false
)

/**
 * Estimated amount of US41. The API is the authority (POST bookings/estimate, BookingPricing); this
 * local copy of its rule gives an immediate value while the request is in flight or when the API
 * cannot be reached: hours with two decimals (HALF_UP) and Math.Round(hourlyRate * hours, 2), whose
 * default .NET mode is banker's rounding.
 */
object BookingEstimator {
    /** Minimum duration the API accepts for a booking and an estimate. */
    const val MIN_DURATION_MINUTES = 30L

    /** The API validates past dates with the date in Peru, not UTC. */
    val PERU_ZONE: ZoneId = ZoneId.of("America/Lima")

    fun todayInPeru(clock: Clock = Clock.systemUTC()): LocalDate = LocalDate.now(clock.withZone(PERU_ZONE))

    fun hoursBetween(start: LocalTime, end: LocalTime): BigDecimal? {
        if (!end.isAfter(start)) return null
        val minutes = Duration.between(start, end).toMinutes()
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
    }

    fun meetsMinimumDuration(start: LocalTime, end: LocalTime): Boolean =
        end.isAfter(start) && Duration.between(start, end).toMinutes() >= MIN_DURATION_MINUTES

    fun estimate(hourlyRate: BigDecimal, start: LocalTime, end: LocalTime): BookingEstimate? {
        if (hourlyRate.signum() <= 0 || !meetsMinimumDuration(start, end)) return null
        val hours = hoursBetween(start, end) ?: return null
        return BookingEstimate(hourlyRate, hours, total(hourlyRate, hours))
    }

    fun fromApi(response: BookingEstimateResponse): BookingEstimate =
        BookingEstimate(response.hourlyRate, response.hours, response.totalAmount, fromApi = true)

    fun total(hourlyRate: BigDecimal, hours: BigDecimal): BigDecimal =
        hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_EVEN)

    fun matches(estimated: BigDecimal, registered: BigDecimal): Boolean = estimated.compareTo(registered) == 0
}
