package com.incleanhome.mobile.claims.presentation

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.claims.data.ClaimFailure
import com.incleanhome.mobile.claims.data.ClaimTrackResponse
import com.incleanhome.mobile.claims.data.ClaimType
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.format.formatDateTime
import com.incleanhome.mobile.ui.format.claimStatusValue
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens
import com.incleanhome.mobile.ui.theme.Navy

@Composable
fun ClaimsBookScreen(
    onCreate: () -> Unit,
    onTrack: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(InCleanHomeDimens.ScreenPadding)
        ) {
            ScreenHeader(stringResource(R.string.claims_book_title), onBack)
            Spacer(Modifier.height(20.dp))
            InCleanHomeCard {
                Text(
                    stringResource(R.string.claims_book_intro),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton(stringResource(R.string.claims_register_action), onCreate)
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    stringResource(R.string.claims_track_action),
                    onTrack,
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CreateClaimScreen(
    viewModel: CreateClaimViewModel,
    onTrackCode: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    ScreenBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(InCleanHomeDimens.ScreenPadding)
        ) {
            ScreenHeader(stringResource(R.string.claims_book_title), onBack)
            Spacer(Modifier.height(20.dp))
            if (state.createdCode != null) {
                ClaimCreatedContent(state.createdCode.orEmpty(), onTrackCode, onBack)
            } else {
                ClaimForm(state, viewModel)
            }
        }
    }
}

@Composable
private fun ClaimForm(state: ClaimFormState, viewModel: CreateClaimViewModel) {
    InCleanHomeCard {
        Text(stringResource(R.string.claims_form_title), style = MaterialTheme.typography.titleLarge, color = Navy)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.claims_type), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.type == ClaimType.CLAIM) {
                PrimaryButton(stringResource(R.string.claim_type_claim), {}, Modifier.weight(1f))
            } else {
                SecondaryButton(stringResource(R.string.claim_type_claim), { viewModel.onTypeChange(ClaimType.CLAIM) }, Modifier.weight(1f))
            }
            if (state.type == ClaimType.COMPLAINT) {
                PrimaryButton(stringResource(R.string.claim_type_complaint), {}, Modifier.weight(1f))
            } else {
                SecondaryButton(stringResource(R.string.claim_type_complaint), { viewModel.onTypeChange(ClaimType.COMPLAINT) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        ClaimField(
            value = state.consumerName,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.claims_consumer_name),
            invalid = ClaimFormField.NAME in state.invalidFields,
            requiredMessage = stringResource(R.string.claims_name_required)
        )
        ClaimSpacer()
        InCleanHomeTextField(
            state.consumerDocument,
            viewModel::onDocumentChange,
            stringResource(R.string.claims_consumer_document)
        )
        ClaimSpacer()
        ClaimField(
            value = state.consumerEmail,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.claims_consumer_email),
            invalid = ClaimFormField.EMAIL in state.invalidFields,
            requiredMessage = stringResource(R.string.claims_email_invalid),
            keyboardType = KeyboardType.Email
        )
        ClaimSpacer()
        InCleanHomeTextField(
            state.consumerPhone,
            viewModel::onPhoneChange,
            stringResource(R.string.claims_consumer_phone),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        ClaimSpacer()
        InCleanHomeTextField(
            state.relatedService,
            viewModel::onRelatedServiceChange,
            stringResource(R.string.claims_related_service)
        )
        ClaimSpacer()
        ClaimField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = stringResource(R.string.claims_description),
            invalid = ClaimFormField.DESCRIPTION in state.invalidFields,
            requiredMessage = stringResource(R.string.claims_description_required),
            singleLine = false,
            minLines = 4
        )
        ClaimSpacer()
        InCleanHomeTextField(
            state.consumerRequest,
            viewModel::onConsumerRequestChange,
            stringResource(R.string.claims_consumer_request),
            singleLine = false,
            minLines = 3
        )
        state.failure?.let {
            Spacer(Modifier.height(12.dp))
            ClaimError(it)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.claims_submit),
            onClick = viewModel::submit,
            enabled = !state.isSubmitting,
            loading = state.isSubmitting
        )
    }
}

@Composable
private fun ClaimCreatedContent(code: String, onTrackCode: (String) -> Unit, onBack: () -> Unit) {
    InCleanHomeCard {
        Text(stringResource(R.string.claims_created_title), style = MaterialTheme.typography.titleLarge, color = Navy)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.claims_created_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.claims_tracking_code_label), style = MaterialTheme.typography.labelLarge)
        Text(code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.claims_save_code), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.claims_track_action), { onTrackCode(code) })
        Spacer(Modifier.height(12.dp))
        SecondaryButton(stringResource(R.string.action_back), onBack, Modifier.fillMaxWidth())
    }
}

@Composable
fun ClaimTrackScreen(
    viewModel: ClaimTrackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    ScreenBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(InCleanHomeDimens.ScreenPadding)
        ) {
            ScreenHeader(stringResource(R.string.claims_track_title), onBack)
            Spacer(Modifier.height(20.dp))
            InCleanHomeCard {
                Text(stringResource(R.string.claims_track_help), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                InCleanHomeTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    label = stringResource(R.string.claims_tracking_code_label),
                    isError = state.codeRequired,
                    supportingText = if (state.codeRequired) {
                        { Text(stringResource(R.string.claims_code_required)) }
                    } else null
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    stringResource(R.string.claims_track_submit),
                    viewModel::track,
                    enabled = !state.isLoading,
                    loading = state.isLoading
                )
                state.failure?.let {
                    Spacer(Modifier.height(12.dp))
                    ClaimError(it)
                }
            }
            if (state.isLoading) {
                Spacer(Modifier.height(20.dp))
                LoadingState()
            }
            state.claim?.let {
                Spacer(Modifier.height(20.dp))
                ClaimTrackingResult(it)
            }
        }
    }
}

@Composable
private fun ClaimTrackingResult(claim: ClaimTrackResponse) {
    InCleanHomeCard {
        ClaimResultRow(stringResource(R.string.claims_tracking_code_label), claim.code)
        ClaimResultRow(stringResource(R.string.claims_type), presentationValue(claim.type))
        ClaimResultRow(stringResource(R.string.claims_status), claimStatusValue(claim.status))
        claim.createdAt?.let {
            ClaimResultRow(stringResource(R.string.claims_created_at), formatDateTime(it))
        }
        claim.resolvedAt?.let {
            ClaimResultRow(stringResource(R.string.claims_resolved_at), formatDateTime(it))
        }
        claim.adminNote?.takeIf(String::isNotBlank)?.let {
            ClaimResultRow(stringResource(R.string.claims_admin_response), it)
        }
    }
}

@Composable
private fun ClaimResultRow(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = Navy)
    Text(value, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ClaimField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    invalid: Boolean,
    requiredMessage: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    InCleanHomeTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = invalid,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = if (invalid) ({ Text(requiredMessage) }) else null
    )
}

@Composable
private fun ClaimError(failure: ClaimFailure) {
    Text(
        text = stringResource(
            when (failure) {
                ClaimFailure.INVALID_DATA -> R.string.claims_error_invalid_data
                ClaimFailure.NOT_FOUND -> R.string.claims_error_not_found
                ClaimFailure.NETWORK -> R.string.claims_error_network
                ClaimFailure.SERVER -> R.string.claims_error_server
                ClaimFailure.INVALID_RESPONSE -> R.string.claims_error_invalid_response
                ClaimFailure.UNKNOWN -> R.string.claims_error_unknown
            }
        ),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ClaimSpacer() = Spacer(Modifier.height(12.dp))
