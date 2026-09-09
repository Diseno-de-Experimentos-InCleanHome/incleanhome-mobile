package com.incleanhome.mobile.worker.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.legal.presentation.LegalDocumentLinks
import com.incleanhome.mobile.worker.data.WorkerProfile
import com.incleanhome.mobile.ui.components.*
import com.incleanhome.mobile.ui.format.*
import com.incleanhome.mobile.ui.theme.GreenLight
import com.incleanhome.mobile.ui.theme.Navy

@Composable
fun WorkerProfileScreen(
    viewModel: WorkerProfileViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenBackground(modifier) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            ScreenHeader(stringResource(R.string.title_my_profile), onBack)
            Spacer(Modifier.height(16.dp))
            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorRetryState(uiState.errorMessage.orEmpty(), viewModel::loadProfile)
                uiState.profile != null -> uiState.profile?.let { profile ->
                    InCleanHomeCard {
                        Text(profile.name, color = Navy, style = MaterialTheme.typography.headlineSmall)
                        profile.phone?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.label_phone, it)) }
                        Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.label_gender, presentationValue(profile.gender))); Text(stringResource(R.string.label_age, profile.age))
                    }
                    Spacer(Modifier.height(12.dp)); InCleanHomeCard {
                        Text(stringResource(R.string.label_services_title), color = Navy, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp)); profile.serviceTypes.forEach { value -> Text(presentationValue(value), Modifier.background(GreenLight, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)); Spacer(Modifier.height(4.dp)) }
                        Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.label_zones, presentationValues(profile.zones)))
                    }
                    Spacer(Modifier.height(12.dp)); InCleanHomeCard {
                        Text(stringResource(R.string.label_hourly_rate_named, formatCurrency(profile.hourlyRate)), color = Navy, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.label_experience, profile.experienceYears)); Row { Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.primary); Text(" ${formatRating(profile.averageRating)}") }; Text(stringResource(R.string.label_services_completed, profile.totalServices))
                    }
                    if (profile.bio.isNotBlank()) { Spacer(Modifier.height(12.dp)); InCleanHomeCard { Text(stringResource(R.string.label_bio_title), color = Navy, style = MaterialTheme.typography.titleMedium); Text(profile.bio) } }
                    Spacer(Modifier.height(16.dp)); PrimaryButton(stringResource(R.string.action_edit_profile), onEdit)
                }
            }
            Spacer(Modifier.height(12.dp))
            InCleanHomeCard {
                Text(
                    stringResource(R.string.legal_information),
                    color = Navy,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                LegalDocumentLinks(onTerms = onTerms, onPrivacy = onPrivacy)
            }
        }
    }
}
