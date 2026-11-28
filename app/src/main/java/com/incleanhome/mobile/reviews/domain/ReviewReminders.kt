package com.incleanhome.mobile.reviews.domain

import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingStatus
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.KeyValueStore
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Origin of a review for review_submitted (US35, Escenario 4). */
object ReviewSource {
    const val DIRECT = "direct"
    const val REMINDER = "reminder"
}

/**
 * Rules of US35 for a booking: completed, without review and with the configured delay elapsed
 * since the end of the service. The end of the booking is the reference because the API does not
 * expose the completion time.
 */
object ReviewReminderPolicy {
    fun serviceEnd(booking: Booking): LocalDateTime? = runCatching {
        LocalDateTime.of(LocalDate.parse(booking.date), LocalTime.parse(booking.endTime.take(5)))
    }.getOrNull()

    fun isDue(booking: Booking, now: LocalDateTime, delayHours: Long): Boolean {
        if (booking.status != BookingStatus.COMPLETED || booking.hasReview) return false
        val end = serviceEnd(booking) ?: return false
        return !end.plusHours(delayHours).isAfter(now)
    }
}

/**
 * In-app delivery of the reminder: at most one reminder per booking (Escenario 3), only for the
 * treatment group (Escenario 5) and recorded as review_reminder_sent with channel in_app.
 * The e-mail reminder is sent by the API job of incleanhome-backend (channel email); a review opened
 * from either one is sent with source=reminder so the server event keeps the origin.
 */
class ReviewReminderService(
    private val store: KeyValueStore,
    private val experiments: ExperimentService,
    private val tracker: EventTracker,
    private val userId: () -> Int?,
    private val delayHours: Long,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    /** Returns the bookings whose reminder is delivered now and marks them as sent. */
    fun deliverDue(bookings: List<Booking>): List<Booking> {
        val user = userId() ?: return emptyList()
        val now = LocalDateTime.now(clock.withZone(zone))
        val due = bookings.filter { ReviewReminderPolicy.isDue(it, now, delayHours) }
        if (due.isEmpty()) return emptyList()
        val assignment = experiments.recordExposure(Experiment.REVIEW_REMINDER, "client_bookings")
        if (!assignment.featureActive) return emptyList()

        val key = "reminders.sent.$user"
        val sent = store.getString(key).orEmpty().split(',').mapNotNull(String::toIntOrNull).toMutableSet()
        val delivered = due.filter { it.id !in sent }
        delivered.forEach { booking ->
            sent += booking.id
            tracker.track(
                TrackingPlan.REVIEW_REMINDER_SENT,
                mapOf("bookingId" to booking.id, "channel" to "in_app"),
                operationKey = "reminder:${booking.id}",
                assignment = assignment
            )
        }
        if (delivered.isNotEmpty()) store.putString(key, sent.joinToString(","))
        return delivered
    }
}
