package com.incleanhome.mobile

import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatDate
import com.incleanhome.mobile.ui.format.formatMonth
import com.incleanhome.mobile.ui.format.formatTime
import com.incleanhome.mobile.ui.format.formatDayOfWeek
import com.incleanhome.mobile.ui.format.emailAlreadyTakenAddress
import com.incleanhome.mobile.ui.format.fixedUiMessageResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class PresentationFormattersTest {
    @Test
    fun formatsApiDateForSpanishPresentation() {
        withLocale(Locale.forLanguageTag("es-419")) {
            assertEquals("6 de septiembre de 2026", formatDate("2026-09-06"))
        }
    }

    @Test
    fun formatsApiMonthForSpanishPresentation() {
        withLocale(Locale.forLanguageTag("es-419")) {
            assertEquals("Septiembre 2026", formatMonth("2026-09"))
        }
    }

    @Test
    fun formatsApiDateForEnglishPresentation() {
        withLocale(Locale.US) {
            assertEquals("September 6, 2026", formatDate("2026-09-06"))
            assertEquals("September 2026", formatMonth("2026-09"))
            assertEquals("Sunday", formatDayOfWeek(0))
        }
    }

    @Test
    fun formatsLocalizedTimeAndSpanishDay() {
        withLocale(Locale.forLanguageTag("es-419")) {
            assertEquals("Domingo", formatDayOfWeek(0))
            assert(formatTime("09:05").startsWith("9:05"))
        }
    }

    @Test
    fun formatsCurrencyWithoutChangingInputScaleContract() {
        assertEquals("S/ 30.00", formatCurrency(BigDecimal("30")))
    }

    @Test
    fun preservesInvalidApiPresentationValues() {
        assertEquals("sin-fecha", formatDate("sin-fecha"))
        assertEquals("sin-hora", formatTime("sin-hora"))
    }

    @Test
    fun mapsOnlyKnownFixedMessagesToLocalizedResources() {
        assertEquals(R.string.ui_booking_cancelled, fixedUiMessageResource("Reserva cancelada."))
        assertEquals(R.string.ui_error_network, fixedUiMessageResource("No se pudo conectar con el servidor."))
        assertEquals(R.string.ui_error_totp_required, fixedUiMessageResource("Ingresa un código TOTP de 6 dígitos."))
        assertNull(fixedUiMessageResource("Backend-provided message"))
    }

    @Test
    fun mapsKnownBackendErrorsToLocalizedResources() {
        val expectedMappings = mapOf(
            "Invalid email or password" to R.string.backend_error_invalid_email_or_password,
            "Invalid gender" to R.string.backend_error_invalid_gender,
            "Select at least one service type" to R.string.backend_error_select_service_type,
            "Invalid verification code" to R.string.backend_error_invalid_verification_code,
            "Run 2FA setup first" to R.string.backend_error_run_2fa_setup_first,
            "Invalid or expired token" to R.string.backend_error_invalid_or_expired_token,
            "Invalid or expired challenge token" to R.string.backend_error_invalid_or_expired_challenge_token,
            "Worker not found" to R.string.backend_error_worker_not_found,
            "Worker not available at that time slot" to R.string.backend_error_worker_unavailable,
            "Booking date cannot be in the past" to R.string.backend_error_booking_date_in_past,
            "Only clients can create bookings" to R.string.backend_error_only_clients_create_bookings,
            "Invalid date format (expected yyyy-MM-dd)" to R.string.backend_error_invalid_date_format,
            "Invalid status" to R.string.backend_error_invalid_status,
            "Event not found" to R.string.backend_error_event_not_found,
            "Event date cannot be in the past" to R.string.backend_error_event_date_in_past,
            "The application deadline has passed" to R.string.backend_error_application_deadline_passed,
            "You have already applied to this event" to R.string.backend_error_already_applied,
            "Only workers can apply to events" to R.string.backend_error_only_workers_apply,
            "Only clients can publish events" to R.string.backend_error_only_clients_publish_events,
            "Booking not found" to R.string.backend_error_booking_not_found,
            "Only completed bookings can be reviewed" to R.string.backend_error_only_completed_bookings_reviewed,
            "This booking has already been reviewed" to R.string.backend_error_booking_already_reviewed,
            "Rating must be between 1 and 5" to R.string.backend_error_rating_out_of_range,
            "Cannot send a message to yourself." to R.string.backend_error_cannot_message_yourself,
            "Message content cannot be empty." to R.string.backend_error_message_empty,
            "Profile not found" to R.string.backend_error_profile_not_found,
            "Unsupported role" to R.string.backend_error_unsupported_role,
            "Invalid claim type" to R.string.backend_error_invalid_claim_type,
            "Consumer name and email are required" to R.string.backend_error_consumer_identity_required,
            "Description is required" to R.string.backend_error_description_required,
            "Claim not found" to R.string.backend_error_claim_not_found,
            "Only workers have a worker profile" to R.string.backend_error_only_workers_have_profile,
            "Only workers have dashboard stats" to R.string.backend_error_only_workers_have_stats,
            "Debes aceptar los términos y condiciones actuales" to R.string.backend_error_current_terms_required
        )

        expectedMappings.forEach { (message, resource) ->
            assertEquals("Unexpected mapping for: $message", resource, fixedUiMessageResource(message))
        }
    }

    @Test
    fun preservesDynamicEmailAndLeavesUnknownBackendErrorsUnmapped() {
        assertEquals(
            "usuario@gmail.com",
            emailAlreadyTakenAddress("Email usuario@gmail.com is already taken")
        )
        assertNull(emailAlreadyTakenAddress("Email is already taken"))
        assertNull(fixedUiMessageResource("A backend error that Mobile does not know"))
    }

    private fun withLocale(locale: Locale, assertions: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(locale)
            assertions()
        } finally {
            Locale.setDefault(original)
        }
    }
}
