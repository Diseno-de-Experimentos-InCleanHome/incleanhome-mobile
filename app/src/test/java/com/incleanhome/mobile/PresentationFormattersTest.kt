package com.incleanhome.mobile

import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatDate
import com.incleanhome.mobile.ui.format.formatMonth
import com.incleanhome.mobile.ui.format.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PresentationFormattersTest {
    @Test
    fun formatsApiDateForSpanishPresentation() {
        assertEquals("6 de septiembre de 2026", formatDate("2026-09-06"))
    }

    @Test
    fun formatsApiMonthForSpanishPresentation() {
        assertEquals("Septiembre 2026", formatMonth("2026-09"))
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
}
