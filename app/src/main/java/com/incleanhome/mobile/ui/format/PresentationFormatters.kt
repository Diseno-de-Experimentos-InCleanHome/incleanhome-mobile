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

private val backendUiMessageResources = mapOf(
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

private val emailAlreadyTakenPattern = Regex("^Email (.+) is already taken$")

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

@StringRes
internal fun fixedUiMessageResource(message: String): Int? = backendUiMessageResources[message] ?: when (message) {
    "Email o contraseña incorrectos." -> R.string.ui_error_invalid_credentials
    "Los datos no son válidos o el desafío expiró." -> R.string.ui_error_auth_challenge
    "El servidor devolvió una respuesta de autenticación incompleta." -> R.string.ui_error_auth_incomplete
    "El desafío de autenticación expiró. Inicia sesión nuevamente." -> R.string.ui_error_auth_expired
    "No se pudo configurar la autenticación en dos pasos." -> R.string.ui_error_auth_setup
    "El código de verificación no es válido." -> R.string.ui_error_auth_code
    "No se pudo habilitar la autenticación en dos pasos." -> R.string.ui_error_auth_enable
    "No se pudo verificar la autenticación en dos pasos." -> R.string.ui_error_auth_verify
    "El servidor devolvió una respuesta de autenticación desconocida.",
    "El servidor devolvió una respuesta inesperada." -> R.string.ui_error_auth_unknown
    "Ingresa tu email y contraseña." -> R.string.ui_error_login_fields
    "Ingresa un código TOTP de 6 dígitos." -> R.string.ui_error_totp_required
    "El servidor devolvió un rol de usuario desconocido." -> R.string.ui_error_unknown_role
    "No se pudo guardar la sesión de forma segura." -> R.string.ui_error_session_save
    "Reserva cancelada." -> R.string.ui_booking_cancelled
    "Estado actualizado correctamente." -> R.string.ui_booking_status_updated
    "Selecciona uno de los servicios ofrecidos por el trabajador." -> R.string.ui_booking_service_required
    "Ingresa una fecha válida en formato yyyy-MM-dd que no esté en el pasado." -> R.string.ui_booking_date_invalid
    "Usa horas HH:mm y una hora de fin posterior a la de inicio.",
    "Usa horas HH:mm y una hora final posterior." -> R.string.ui_time_range_invalid
    "La dirección es obligatoria." -> R.string.ui_address_required
    "El título es obligatorio." -> R.string.ui_event_title_required
    "Ingresa al menos un tipo de servicio." -> R.string.ui_event_service_required
    "La zona es obligatoria." -> R.string.ui_zone_required
    "Usa una fecha válida que no esté en el pasado." -> R.string.ui_event_date_invalid
    "Se necesita al menos 1 worker." -> R.string.ui_event_workers_required
    "Ingresa una tarifa por hora válida." -> R.string.ui_event_rate_invalid
    "Usa un deadline ISO-8601 UTC, por ejemplo 2026-09-10T18:00:00Z." -> R.string.ui_event_deadline_invalid
    "El deadline debe ser anterior al inicio del evento." -> R.string.ui_event_deadline_order
    "Postulación enviada." -> R.string.ui_application_submitted
    "Evento cancelado." -> R.string.ui_event_cancelled
    "Evento completado." -> R.string.ui_event_completed
    "Perfil actualizado." -> R.string.profile_updated
    "Completa correctamente los campos numéricos y el nombre." -> R.string.ui_profile_fields_invalid
    "Solo se pueden calificar reservas completadas." -> R.string.ui_review_completed_only
    "Esta reserva ya tiene una reseña." -> R.string.ui_review_already_exists
    "Selecciona una calificación entre 1 y 5." -> R.string.ui_review_rating_invalid
    "El mensaje no puede estar vacío." -> R.string.ui_message_empty
    "Usa días del 0 al 6 y horarios en formato HH:mm." -> R.string.ui_availability_invalid
    "Disponibilidad guardada." -> R.string.ui_availability_saved
    "Los datos enviados no son válidos." -> R.string.ui_error_invalid_data
    "La operación entra en conflicto con el estado actual." -> R.string.ui_error_conflict
    "El servidor no está disponible. Inténtalo nuevamente." -> R.string.ui_error_server
    "No se pudo conectar con el servidor." -> R.string.ui_error_network
    "Ocurrió un error inesperado." -> R.string.ui_error_unexpected
    else -> when {
        message.startsWith("La sesión no es válida") -> R.string.ui_error_invalid_session
        message.startsWith("No tienes permiso") -> R.string.ui_error_forbidden
        message.startsWith("No se encontró") -> R.string.ui_error_not_found
        message.startsWith("No se pudo completar") || message.startsWith("No se pudo obtener") ||
            message.startsWith("No se pudo iniciar sesión") -> R.string.ui_error_operation
        else -> null
    }
}

internal fun emailAlreadyTakenAddress(message: String): String? =
    emailAlreadyTakenPattern.matchEntire(message)?.groupValues?.get(1)

@Composable
fun localizedUiMessage(message: String): String {
    emailAlreadyTakenAddress(message)?.let { email ->
        return stringResource(R.string.backend_error_email_taken, email)
    }
    return fixedUiMessageResource(message)?.let { stringResource(it) } ?: message
}

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

fun formatDayOfWeek(dayOfWeek: Int): String = if (dayOfWeek in 0..6) {
    LocalDate.of(2023, 1, 1).plusDays(dayOfWeek.toLong())
        .format(DateTimeFormatter.ofPattern("EEEE", locale()))
        .replaceFirstChar { it.titlecase(locale()) }
} else ""

@Composable
fun localizedDayOfWeek(dayOfWeek: Int): String =
    formatDayOfWeek(dayOfWeek).ifBlank { stringResource(R.string.unavailable_day) }

private inline fun parseOrOriginal(value: String, formatter: (String) -> String): String = try {
    formatter(value)
} catch (_: DateTimeParseException) {
    value
}

