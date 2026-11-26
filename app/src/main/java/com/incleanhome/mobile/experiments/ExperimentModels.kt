package com.incleanhome.mobile.experiments

/**
 * Experimental features of the To-Be platform (US43). Identifiers match the Experiment Cards
 * of the report so mobile events can be joined with web and API events.
 */
enum class Experiment(val id: String, val userStory: String, val abTest: Boolean) {
    /** EC-BLQ01: suggestion of nearby and available workers (US33). */
    WORKER_SUGGESTIONS("EC-BLQ01", "US33", abTest = true),

    /** EC-BLQ02: verified reviews and worker history (US34). */
    VERIFIED_REVIEWS("EC-BLQ02", "US34", abTest = true),

    /** EC-BLQ03: reminder to rate a completed service (US35). */
    REVIEW_REMINDER("EC-BLQ03", "US35", abTest = true),

    /** EC-BLQ06: estimated amount before confirming a booking (US41). */
    BOOKING_ESTIMATE("EC-BLQ06", "US41", abTest = true),

    /** EC-BLQ04: frequent-problem answers before a claim (US38). Activated by period, not A/B. */
    SUPPORT_FAQ("EC-BLQ04", "US38", abTest = false),

    /** EC-BLQ01 (prestador): service area registration (US32). Activated for every worker. */
    SERVICE_AREA("EC-BLQ01-SA", "US32", abTest = false);

    companion object {
        fun fromId(id: String): Experiment? = entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
    }
}

enum class Variant(val wire: String) {
    CONTROL("control"),
    TREATMENT("treatment")
}

/** Configuration of one experiment: whether it runs and the share of users in treatment. */
data class ExperimentSetting(
    val enabled: Boolean,
    val treatmentPercent: Int
) {
    init {
        require(treatmentPercent in 0..100) { "treatmentPercent must be between 0 and 100" }
    }

    companion object {
        val OFF = ExperimentSetting(enabled = false, treatmentPercent = 0)
    }
}

/**
 * Result of evaluating an experiment for the current user.
 * [inExperiment] is false when the experiment is off or the user is outside the assignment unit,
 * in which case the current (As-Is) behaviour applies.
 */
data class Assignment(
    val experiment: Experiment,
    val variant: Variant,
    val inExperiment: Boolean
) {
    val featureActive: Boolean get() = inExperiment && variant == Variant.TREATMENT

    companion object {
        fun inactive(experiment: Experiment) = Assignment(experiment, Variant.CONTROL, inExperiment = false)
    }
}
