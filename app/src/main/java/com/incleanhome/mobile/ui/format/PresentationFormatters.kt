package com.incleanhome.mobile.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.incleanhome.mobile.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

private fun locale() = Locale.getDefault()
private fun fullDateFormatter() = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale())
private fun monthFormatter() = DateTimeFormatter.ofPattern("MMMM uuuu", locale())
private fun timeFormatter() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale())
private fun dateTimeFormatter() = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale())

private val presentationValues = mapOf(
    "limpieza_general" to R.string.service_general_cleaning,
    "cocina" to R.string.service_cooking,
    "lavanderia" to R.string.service_laundry,
    "planchado" to R.string.service_ironing,
    "limpieza_profunda" to R.string.service_deep_cleaning,
    "cuidado_ninos" to R.string.service_child_care,
    "cuidado_adultos" to R.string.service_adult_care,
    "jardineria" to R.string.service_gardening,
    "female" to R.string.gender_female,
    "male" to R.string.gender_male,
    "other" to R.string.gender_other,
    "client" to R.string.role_client,
    "worker" to R.string.role_worker,
    "pending" to R.string.status_pending,
    "accepted" to R.string.status_accepted,
    "rejected" to R.string.status_rejected,
    "cancelled" to R.string.status_cancelled,
    "completed" to R.string.status_completed,
    "open" to R.string.status_open,
    "staffed" to R.string.status_staffed,
    "in_progress" to R.string.status_in_progress,
    "withdrawn" to R.string.status_withdrawn,
    "reclamo" to R.string.claim_type_claim,
    "queja" to R.string.claim_type_complaint,
    "registered" to R.string.status_registered,
    "in_review" to R.string.status_in_review,
    "resolved" to R.string.status_resolved
)

private val claimStatusValues = mapOf(
    "registered" to R.string.status_registered,
    "in_review" to R.string.status_in_review,
    "resolved" to R.string.status_resolved,
    "rejected" to R.string.claim_status_rejected
)

val CanonicalServiceTypes = listOf(
    "limpieza_general",
    "cocina",
    "lavanderia",
    "planchado",
    "cuidado_ninos",
    "cuidado_adultos",
    "jardineria",
    "limpieza_profunda"
)

@StringRes
fun presentationValueResource(value: String): Int? = presentationValues[value.trim().lowercase(Locale.ROOT)]

@StringRes
fun claimStatusResource(value: String): Int? = claimStatusValues[value.trim().lowercase(Locale.ROOT)]

@Composable
fun presentationValue(value: String): String {
    val normalized = value.trim()
    val resource = presentationValueResource(normalized)
    return resource?.let { stringResource(it) } ?: normalized
}

@Composable
fun claimStatusValue(value: String): String {
    val normalized = value.trim()
    return claimStatusResource(normalized)?.let { stringResource(it) } ?: normalized
}
@Composable
fun presentationValues(values: Iterable<String>, separator: String = ", "): String {
    val labels = mutableListOf<String>()
    for (value in values) labels += presentationValue(value)
    return labels.joinToString(separator)
}
fun formatDate(value: String): String = parseOrOriginal(value) {
    LocalDate.parse(it).format(fullDateFormatter())
}

fun formatMonth(value: String): String = parseOrOriginal(value) {
    YearMonth.parse(it).format(monthFormatter()).replaceFirstChar { char -> char.titlecase(locale()) }
}

fun formatTime(value: String): String = parseOrOriginal(value) {
    LocalTime.parse(it).format(timeFormatter())
}

fun formatDateRange(date: String, startTime: String, endTime: String): String =
    "${formatDate(date)}, ${formatTime(startTime)} – ${formatTime(endTime)}"

fun formatDateTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(dateTimeFormatter())
}.recoverCatching {
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(dateTimeFormatter())
}.getOrDefault(value)

fun formatCurrency(amount: BigDecimal): String = "S/ ${amount.setScale(2, RoundingMode.HALF_UP).toPlainString()}"

fun formatRating(rating: BigDecimal): String = rating.setScale(1, RoundingMode.HALF_UP).toPlainString()

fun humanizeIdentifier(value: String): String = value
    .trim()
    .replace('_', ' ')
    .replaceFirstChar { it.titlecase(locale()) }

fun formatDayOfWeek(dayOfWeek: Int): String = if (dayOfWeek in 0..6) { LocalDate.of(2023, 1, 1).plusDays(dayOfWeek.toLong()).format(DateTimeFormatter.ofPattern("EEEE", locale())).replaceFirstChar { it.titlecase(locale()) } } else "Unavailable day"

private inline fun parseOrOriginal(value: String, formatter: (String) -> String): String = try {
    formatter(value)
} catch (_: DateTimeParseException) {
    value
}

