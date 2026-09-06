package com.incleanhome.mobile.worker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.worker.data.WorkerProfile
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.format.presentationValues

@Composable
fun WorkerProfileScreen(
    viewModel: WorkerProfileViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(stringResource(R.string.title_my_profile), onBack)
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> LoadingState()

            uiState.errorMessage != null -> ErrorRetryState(uiState.errorMessage.orEmpty(), viewModel::loadProfile)

            uiState.profile != null -> uiState.profile?.let { profile ->
                WorkerProfileContent(profile)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onEdit) { Text(stringResource(R.string.action_edit_profile)) }
            }
        }
    }
}

@Composable
private fun WorkerProfileContent(profile: WorkerProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(profile.name, style = MaterialTheme.typography.headlineMedium)
        profile.phone?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.label_phone, it)) }
        Text(stringResource(R.string.label_age, profile.age))
        Text(stringResource(R.string.label_gender, presentationValue(profile.gender)))
        Text(stringResource(R.string.label_services, presentationValues(profile.serviceTypes)))
        Text(stringResource(R.string.label_zones, profile.zones.joinToString()))
        Text(stringResource(R.string.label_hourly_rate_named, formatCurrency(profile.hourlyRate)))
        Text(stringResource(R.string.label_experience, profile.experienceYears))
        Text(stringResource(R.string.label_bio, profile.bio))
        Text(stringResource(R.string.label_rating, profile.averageRating.toPlainString()))
        Text(stringResource(R.string.label_services_completed, profile.totalServices))
    }
}
