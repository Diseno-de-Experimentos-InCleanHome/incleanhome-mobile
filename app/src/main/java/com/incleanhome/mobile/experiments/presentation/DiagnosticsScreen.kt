package com.incleanhome.mobile.experiments.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.BuildConfig
import com.incleanhome.mobile.R
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentSetting
import com.incleanhome.mobile.experiments.Experimentation
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens
import com.incleanhome.mobile.ui.theme.Navy
import java.io.File
import java.time.Instant
import kotlinx.coroutines.launch

/**
 * Testing-environment screen (staging flavor only): shows the environment, the experiment
 * assignment of the signed-in user and the recorded events, and exports them for the evidence.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val experiments = Experimentation.experimentsOrNull()
    val tracker = Experimentation.trackerOrNull()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var sentCount by remember { mutableStateOf<Int?>(null) }

    ScreenBackground(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(InCleanHomeDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(stringResource(R.string.diagnostics_title), onBack)
            InCleanHomeCard {
                Text(stringResource(R.string.diagnostics_environment, BuildConfig.ENVIRONMENT), color = Navy)
                Text(stringResource(R.string.diagnostics_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                Text(stringResource(R.string.diagnostics_api, BuildConfig.BASE_URL), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(
                        R.string.diagnostics_endpoints,
                        BuildConfig.REMOTE_EXPERIMENTS_ENABLED.toString(),
                        BuildConfig.TRACKING_UPLOAD_ENABLED.toString()
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (experiments == null || tracker == null) return@Column

            key(refresh) {
                Experiment.entries.forEach { experiment ->
                    val assignment = experiments.assignment(experiment)
                    val setting = experiments.config().settingFor(experiment)
                    InCleanHomeCard {
                        Text("${experiment.id} · ${experiment.userStory}", style = MaterialTheme.typography.titleMedium, color = Navy)
                        Text(
                            stringResource(
                                R.string.diagnostics_assignment,
                                if (setting.enabled) "on" else "off",
                                setting.treatmentPercent,
                                if (assignment.inExperiment) assignment.variant.wire else "-",
                                assignment.featureActive.toString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                R.string.diagnostics_override_base to null,
                                R.string.diagnostics_override_off to ExperimentSetting.OFF,
                                R.string.diagnostics_override_treatment to ExperimentSetting(true, 100),
                                R.string.diagnostics_override_control to ExperimentSetting(true, 0),
                                R.string.diagnostics_override_half to ExperimentSetting(true, 50)
                            ).forEach { (label, setting) ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        experiments.setLocalOverride(experiment, setting)
                                        experiments.resetAssignment(experiment)
                                        refresh++
                                    },
                                    label = { Text(stringResource(label)) }
                                )
                            }
                        }
                    }
                }

                val events = tracker.pendingEvents()
                InCleanHomeCard {
                    Text(stringResource(R.string.diagnostics_events, events.size), style = MaterialTheme.typography.titleMedium, color = Navy)
                    Spacer(Modifier.height(6.dp))
                    events.takeLast(25).reversed().forEach { event ->
                        Text(
                            "${event.occurredAt} ${event.eventName} ${event.experimentId ?: ""} ${event.variant ?: ""} ${event.properties}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            PrimaryButton(stringResource(R.string.diagnostics_export), {
                val directory = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(directory, "tracking-export-${Instant.now().toEpochMilli()}.json")
                file.writeText(tracker.exportJson())
                exportMessage = file.absolutePath
            })
            SecondaryButton(stringResource(R.string.diagnostics_send), {
                scope.launch {
                    sentCount = tracker.flush()
                    refresh++
                }
            }, Modifier.fillMaxWidth())
            exportMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            sentCount?.let { Text(stringResource(R.string.diagnostics_sent, it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
