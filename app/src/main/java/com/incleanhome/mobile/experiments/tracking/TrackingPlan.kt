package com.incleanhome.mobile.experiments.tracking

/**
 * Mobile subset of the tracking plan (8.2.8, Tabla 8.46). Only the listed properties are kept;
 * anything else is discarded before the event is stored (US42, Escenario 3).
 *
 * Operation events that the plan assigns to the API (booking_request_created, review_submitted)
 * are also captured after the API confirms the operation and flagged with capturedBy=client,
 * so they can be reconciled with the server records when those exist.
 */
object TrackingPlan {
    const val WORKER_SEARCH_PERFORMED = "worker_search_performed"
    const val WORKER_SUGGESTIONS_SHOWN = "worker_suggestions_shown"
    const val WORKER_PROFILE_VIEWED = "worker_profile_viewed"
    const val WORKER_REVIEWS_VIEWED = "worker_reviews_viewed"
    const val BOOKING_FORM_OPENED = "booking_form_opened"
    const val BOOKING_ESTIMATE_VIEWED = "booking_estimate_viewed"
    const val BOOKING_REQUEST_CREATED = "booking_request_created"
    const val BOOKING_REQUEST_FAILED = "booking_request_failed"
    const val REVIEW_REMINDER_SENT = "review_reminder_sent"
    const val REVIEW_SUBMITTED = "review_submitted"
    const val FAQ_VIEWED = "faq_viewed"
    const val FAQ_RESOLVED = "faq_resolved"
    const val EXPERIMENT_EXPOSURE = "experiment_exposure"

    val allowedProperties: Map<String, Set<String>> = mapOf(
        WORKER_SEARCH_PERFORMED to setOf(
            "searchId", "searchMode", "serviceCategory", "district", "filtersApplied", "resultsCount"
        ),
        WORKER_SUGGESTIONS_SHOWN to setOf("searchId", "suggestionCount"),
        WORKER_PROFILE_VIEWED to setOf("workerId", "source", "searchId"),
        WORKER_REVIEWS_VIEWED to setOf("workerId", "reviewCount", "verifiedCount"),
        BOOKING_FORM_OPENED to setOf("formId", "searchId", "workerId", "origin"),
        BOOKING_ESTIMATE_VIEWED to setOf("formId", "estimatedAmount"),
        BOOKING_REQUEST_CREATED to setOf(
            "bookingId", "formId", "searchId", "origin", "estimatedAmount", "registeredAmount",
            "amountMatches", "capturedBy"
        ),
        BOOKING_REQUEST_FAILED to setOf("formId", "errorType"),
        REVIEW_REMINDER_SENT to setOf("bookingId", "channel"),
        REVIEW_SUBMITTED to setOf("bookingId", "rating", "source", "isVerified", "capturedBy"),
        FAQ_VIEWED to setOf("category", "faqId"),
        FAQ_RESOLVED to setOf("category", "faqId"),
        EXPERIMENT_EXPOSURE to setOf("location")
    )

    fun isKnown(eventName: String): Boolean = eventName in allowedProperties
}

/**
 * Privacy rules of 8.2.8: no passwords, TOTP codes, tokens, personal addresses, coordinates,
 * message or claim content. A property survives only if it is in the plan, its key is not
 * sensitive and its value does not look like personal data.
 */
object PrivacyGuard {
    private val sensitiveKeyFragments = listOf(
        "password", "pass", "token", "totp", "otp", "secret", "authorization", "address",
        "direccion", "lat", "lng", "lon", "coordinate", "message", "content", "comment",
        "description", "email", "phone", "document", "name"
    )
    private val emailPattern = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
    private val jwtPattern = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")
    private val longDigitsPattern = Regex(".*\\d{7,}.*")
    private const val MAX_STRING_LENGTH = 64

    // The district comes from a free text field: keep it only if it looks like a district name
    // (letters, spaces, apostrophes, hyphens), never a street address with numbers.
    private val districtPattern = Regex("^[\\p{L} '_-]{2,40}$")
    private val addressWords = Regex("(?i)\\b(av|avenida|jr|jir[oó]n|calle|psje|pasaje|mz|lote|dpto|nro)\\b")

    fun isDistrictName(value: Any): Boolean =
        value is String && districtPattern.matches(value.trim()) && !addressWords.containsMatchIn(value)

    fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase()
        return sensitiveKeyFragments.any { normalized.contains(it) }
    }

    /** [isIdentifier] relaxes the digit rule for generated identifiers such as formId (UUID). */
    fun isSafeValue(value: Any?, isIdentifier: Boolean = false): Boolean = when (value) {
        null -> true
        is Boolean, is Int, is Long -> true
        is Double -> !value.isNaN() && !value.isInfinite()
        is Number -> true
        is String -> value.length <= MAX_STRING_LENGTH &&
            !emailPattern.containsMatchIn(value) &&
            !jwtPattern.matches(value) &&
            (isIdentifier || !longDigitsPattern.matches(value))
        else -> false
    }

    fun sanitize(eventName: String, properties: Map<String, Any?>): Map<String, Any?> {
        val allowed = TrackingPlan.allowedProperties[eventName] ?: return emptyMap()
        return properties.filter { (key, value) ->
            key in allowed && !isSensitiveKey(key) && isSafeValue(value, isIdentifier = key.endsWith("Id")) &&
                (key != "district" || value == null || isDistrictName(value))
        }
    }
}
