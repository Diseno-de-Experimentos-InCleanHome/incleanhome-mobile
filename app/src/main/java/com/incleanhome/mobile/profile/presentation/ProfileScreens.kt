package com.incleanhome.mobile.profile.presentation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.components.ServiceTypeSelector
import com.incleanhome.mobile.ui.format.formatMonth
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.worker.data.WorkerProfile

@Composable private fun Head(title:String,back:()->Unit){ScreenHeader(title,back)}
@Composable
fun ClientProfileScreen(vm: ClientProfileViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    ScreenBackground {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Head("Mi perfil", onBack)
            when {
                s.loading -> LoadingState()
                s.error != null -> ErrorRetryState(s.error!!, vm::load)
                s.profile != null -> {
                    val p = s.profile!!
                    InCleanHomeCard {
                        Text("Cuenta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(12.dp))
                        if (!editing) {
                            Text(stringResource(R.string.profile_name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(p.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.profile_phone), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(p.phone ?: "-", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton("Editar perfil", { name = p.name; phone = p.phone.orEmpty(); editing = true })
                        } else {
                            InCleanHomeTextField(name, { name = it }, stringResource(R.string.profile_name), enabled = !s.saving)
                            InCleanHomeTextField(phone, { phone = it }, stringResource(R.string.profile_phone), enabled = !s.saving)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrimaryButton("Guardar", { vm.save(name, phone); editing = false }, Modifier.weight(1f), enabled = !s.saving, loading = s.saving)
                                SecondaryButton("Cancelar", { editing = false }, Modifier.weight(1f), enabled = !s.saving)
                            }
                        }
                    }
                    s.success?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
@Composable
fun WorkerProfileEditScreen(vm: WorkerProfileEditViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    val p = s.profile
    var name by remember(p) { mutableStateOf(p?.name.orEmpty()) }
    var phone by remember(p) { mutableStateOf(p?.phone.orEmpty()) }
    var age by remember(p) { mutableStateOf(p?.age?.toString().orEmpty()) }
    var exp by remember(p) { mutableStateOf(p?.experienceYears?.toString().orEmpty()) }
    var rate by remember(p) { mutableStateOf(p?.hourlyRate?.toPlainString().orEmpty()) }
    var services by remember(p) { mutableStateOf(p?.serviceTypes?.joinToString(",").orEmpty()) }
    var zones by remember(p) { mutableStateOf(p?.zones?.joinToString(", ").orEmpty()) }
    var bio by remember(p) { mutableStateOf(p?.bio.orEmpty()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Head(stringResource(R.string.title_edit_profile), onBack)
        when {
            s.loading -> LoadingState()
            s.error != null && p == null -> ErrorRetryState(s.error!!, vm::load)
            p != null -> {
                val nameLabel = stringResource(R.string.profile_name)
                val phoneLabel = stringResource(R.string.profile_phone)
                val ageLabel = stringResource(R.string.label_age).substringBefore(":")
                val experienceLabel = stringResource(R.string.profile_experience_years)
                listOf(
                    nameLabel to name,
                    phoneLabel to phone,
                    ageLabel to age,
                    experienceLabel to exp,
                    stringResource(R.string.profile_hourly_rate) to rate
                ).forEach { (label, value) ->
                    InCleanHomeTextField(
                        value = value,
                        onValueChange = { newValue ->
                            when (label) {
                                nameLabel -> name = newValue
                                phoneLabel -> phone = newValue
                                ageLabel -> age = newValue
                                experienceLabel -> exp = newValue
                                else -> rate = newValue
                            }
                        },
                        label = label,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !s.saving
                    )
                }
                Text(stringResource(R.string.auth_services), style = MaterialTheme.typography.labelLarge)
                ServiceTypeSelector(
                    selectedValues = parseServiceTypes(services),
                    onSelectionChange = { services = it.joinToString(",") },
                    enabled = !s.saving
                )
                InCleanHomeTextField(
                    value = zones,
                    onValueChange = { zones = it },
                    label = stringResource(R.string.profile_zones_csv),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !s.saving
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(stringResource(R.string.profile_bio)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !s.saving
                )
                Text(stringResource(R.string.label_gender, presentationValue(p.gender)))
                PrimaryButton(stringResource(R.string.action_save), { vm.save(name, phone, age, exp, rate, services, zones, bio) }, enabled = !s.saving, loading = s.saving)
                if (s.saving) LoadingState()
                s.success?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun parseServiceTypes(value: String): List<String> =
    value.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
@Composable fun WorkerStatsScreen(vm:WorkerStatsViewModel,onBack:()->Unit){val s by vm.state.collectAsState();ScreenBackground{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Head(stringResource(R.string.title_statistics),onBack);when{s.loading->LoadingState();s.error!=null->ErrorRetryState(s.error!!,vm::load);s.stats!=null->{val x=s.stats!!;InCleanHomeCard{Text(stringResource(R.string.stats_completed_services_title));Text(x.completedServices.toString(),style=MaterialTheme.typography.headlineMedium)};InCleanHomeCard{Text(stringResource(R.string.stats_average_rating_title));Text("★ ${x.averageRating.toPlainString()}",style=MaterialTheme.typography.headlineSmall)};Text(stringResource(R.string.stats_services_by_month),style=MaterialTheme.typography.titleMedium);if(x.monthlyServiceCounts.isEmpty())EmptyState(stringResource(R.string.empty_monthly_stats)) else x.monthlyServiceCounts.forEach{InCleanHomeCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(formatMonth(it.month));Text(pluralStringResource(R.plurals.services_count,it.count,it.count))}}}}}}}}
