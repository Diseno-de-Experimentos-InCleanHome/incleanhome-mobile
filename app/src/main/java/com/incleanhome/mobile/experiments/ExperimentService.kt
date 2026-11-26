package com.incleanhome.mobile.experiments

import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.experiments.tracking.TrackingUser

/**
 * Evaluates experimental features for the signed-in user (US43).
 *
 * - The assignment unit of the A/B tests is the authenticated client (8.2.4); workers and
 *   administrators keep the current behaviour. Non A/B features apply to their target role.
 * - The first variant computed for a user is stored, so later changes to the treatment share do
 *   not move users between groups while the experiment runs.
 * - Disabling an experiment restores the current behaviour without deleting stored data.
 */
class ExperimentService(
    private val store: KeyValueStore,
    private val userProvider: () -> TrackingUser?,
    private val baseConfig: () -> ExperimentConfig,
    private val tracker: EventTracker,
    private val allowLocalOverrides: Boolean = false
) {

    fun config(): ExperimentConfig {
        var config = cachedRemoteConfig() ?: baseConfig()
        if (allowLocalOverrides) {
            Experiment.entries.forEach { experiment ->
                localOverride(experiment)?.let { config = config.withOverride(experiment, it) }
            }
        }
        return config
    }

    fun assignment(experiment: Experiment): Assignment {
        if (experiment == Experiment.SUPPORT_FAQ) {
            // The complaints book is public (also before signing in), so US38 is activated by period.
            return if (config().settingFor(experiment).enabled) {
                Assignment(experiment, Variant.TREATMENT, inExperiment = true)
            } else {
                Assignment.inactive(experiment)
            }
        }
        val user = userProvider() ?: return Assignment.inactive(experiment)
        if (!targetsRole(experiment, user.role)) return Assignment.inactive(experiment)
        val setting = config().settingFor(experiment)
        if (!setting.enabled) return Assignment.inactive(experiment)
        if (!experiment.abTest) {
            return Assignment(experiment, Variant.TREATMENT, inExperiment = true)
        }
        val key = "assignment.${experiment.id}.${user.userId}"
        val stored = store.getString(key)?.let { wire -> Variant.entries.firstOrNull { it.wire == wire } }
        val variant = stored ?: ExperimentAssigner.variantFor(experiment.id, user.userId, setting.treatmentPercent)
            .also { store.putString(key, it.wire) }
        return Assignment(experiment, variant, inExperiment = true)
    }

    /**
     * Records experiment_exposure the first time an assigned user reaches the point where the
     * feature would be shown (intention-to-treat analysis, 8.2.4).
     */
    fun recordExposure(experiment: Experiment, location: String): Assignment {
        val assignment = assignment(experiment)
        val user = userProvider()
        if (assignment.inExperiment && experiment.abTest && user != null) {
            // Own non-expiring flag: the tracker's operation-key window is bounded.
            val key = "exposure.${experiment.id}.${user.userId}"
            if (store.getString(key) == null) {
                tracker.track(
                    TrackingPlan.EXPERIMENT_EXPOSURE,
                    mapOf("location" to location),
                    operationKey = "exposure:${experiment.id}",
                    assignment = assignment
                )
                store.putString(key, location)
            }
        }
        return assignment
    }

    fun cacheRemoteConfig(config: ExperimentConfig) = store.putString(REMOTE_CONFIG_KEY, config.encode())

    fun setLocalOverride(experiment: Experiment, setting: ExperimentSetting?) {
        check(allowLocalOverrides) { "Local overrides are only available in the testing environment" }
        val key = "override.${experiment.id}"
        if (setting == null) store.remove(key) else {
            store.putString(key, ExperimentConfig(mapOf(experiment to setting)).encode())
        }
    }

    /** Testing environment only: forgets the stored variant so a new treatment share applies. */
    fun resetAssignment(experiment: Experiment) {
        check(allowLocalOverrides) { "Assignments can only be reset in the testing environment" }
        val user = userProvider() ?: return
        store.remove("assignment.${experiment.id}.${user.userId}")
    }

    private fun localOverride(experiment: Experiment): ExperimentSetting? =
        store.getString("override.${experiment.id}")
            ?.let(ExperimentConfig::parse)
            ?.settings
            ?.get(experiment)

    private fun cachedRemoteConfig(): ExperimentConfig? =
        store.getString(REMOTE_CONFIG_KEY)?.let(ExperimentConfig::parse)

    private fun targetsRole(experiment: Experiment, role: String): Boolean = when (experiment) {
        Experiment.SERVICE_AREA -> role.equals(WORKER, ignoreCase = true)
        else -> role.equals(CLIENT, ignoreCase = true)
    }

    companion object {
        private const val CLIENT = "client"
        private const val WORKER = "worker"
        private const val REMOTE_CONFIG_KEY = "experiments.remoteConfig"
    }
}
