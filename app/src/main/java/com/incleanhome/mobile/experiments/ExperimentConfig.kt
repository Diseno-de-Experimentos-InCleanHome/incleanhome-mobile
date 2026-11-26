package com.incleanhome.mobile.experiments

/**
 * Experiment configuration. Each build variant embeds a default (BuildConfig.EXPERIMENTS) and the
 * API can replace it at runtime through [RemoteExperimentConfig] once the endpoint exists, so an
 * experiment starts or stops without publishing a new version (US43).
 *
 * Text format: "EC-BLQ01=on:50;EC-BLQ02=off:50" (id=state:treatmentPercent).
 */
data class ExperimentConfig(val settings: Map<Experiment, ExperimentSetting>) {

    fun settingFor(experiment: Experiment): ExperimentSetting = settings[experiment] ?: ExperimentSetting.OFF

    fun withOverride(experiment: Experiment, setting: ExperimentSetting): ExperimentConfig =
        copy(settings = settings + (experiment to setting))

    fun encode(): String = Experiment.entries.joinToString(";") { experiment ->
        val setting = settingFor(experiment)
        "${experiment.id}=${if (setting.enabled) "on" else "off"}:${setting.treatmentPercent}"
    }

    companion object {
        val ALL_OFF = ExperimentConfig(emptyMap())

        /** Unknown identifiers and malformed entries are ignored so a bad value never enables a feature. */
        fun parse(text: String?): ExperimentConfig {
            if (text.isNullOrBlank()) return ALL_OFF
            val settings = mutableMapOf<Experiment, ExperimentSetting>()
            text.split(';').forEach { entry ->
                val parts = entry.split('=', limit = 2)
                if (parts.size != 2) return@forEach
                val experiment = Experiment.fromId(parts[0]) ?: return@forEach
                val values = parts[1].trim().split(':')
                val enabled = when (values[0].trim().lowercase()) {
                    "on" -> true
                    "off" -> false
                    else -> return@forEach
                }
                val percent = values.getOrNull(1)?.trim()?.toIntOrNull()?.takeIf { it in 0..100 }
                    ?: if (experiment.abTest) 50 else 100
                settings[experiment] = ExperimentSetting(enabled, percent)
            }
            return ExperimentConfig(settings)
        }
    }
}

/** Response of GET experiments/config (dependency documented for the backend; see docs/backend-endpoints.md). */
data class RemoteExperimentConfig(val experiments: List<RemoteExperimentSetting>?) {
    fun toConfig(): ExperimentConfig {
        val settings = experiments.orEmpty().mapNotNull { remote ->
            val experiment = remote.id?.let(Experiment::fromId) ?: return@mapNotNull null
            val percent = remote.treatmentPercent?.takeIf { it in 0..100 } ?: return@mapNotNull null
            experiment to ExperimentSetting(remote.enabled == true, percent)
        }.toMap()
        return ExperimentConfig(settings)
    }
}

data class RemoteExperimentSetting(
    val id: String?,
    val enabled: Boolean?,
    val treatmentPercent: Int?
)
