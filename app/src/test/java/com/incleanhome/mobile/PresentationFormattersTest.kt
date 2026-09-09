package com.incleanhome.mobile

import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatDate
import com.incleanhome.mobile.ui.format.formatMonth
import com.incleanhome.mobile.ui.format.formatTime
import com.incleanhome.mobile.ui.format.formatDayOfWeek
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
