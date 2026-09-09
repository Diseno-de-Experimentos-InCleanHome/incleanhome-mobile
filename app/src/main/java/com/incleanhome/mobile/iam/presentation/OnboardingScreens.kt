package com.incleanhome.mobile.iam.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.iam.data.AuthGender
import com.incleanhome.mobile.legal.presentation.LegalDocumentLinks
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ServiceTypeSelector
import com.incleanhome.mobile.ui.format.presentationValue
import com.incleanhome.mobile.ui.theme.GreenLight
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen

@Composable
fun AccountTypeScreen(
    onClient: () -> Unit,
    onWorker: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreen(modifier = modifier, onBack = onBack, verticalArrangement = Arrangement.Center) {
        AuthTitle(
            title = stringResource(R.string.auth_create_account),
            subtitle = stringResource(R.string.auth_account_type_subtitle)
        )
        Spacer(Modifier.height(28.dp))
        AccountTypeCard(
            icon = Icons.Rounded.Home,
            title = stringResource(R.string.auth_client_type),
            description = stringResource(R.string.auth_client_type_description),
            onClick = onClient
        )
        Spacer(Modifier.height(16.dp))
        AccountTypeCard(
            icon = Icons.Rounded.Person,
            title = stringResource(R.string.auth_worker_type),
            description = stringResource(R.string.auth_worker_type_description),
            onClick = onWorker
        )
    }
}

@Composable
fun ClientRegistrationScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val state by viewModel.uiState.collectAsState()
    val commonValidation = commonValidationMessages()

    AuthScreen(modifier = modifier, onBack = onBack) {
        AuthTitle(
            title = stringResource(R.string.auth_client_registration),
            subtitle = stringResource(R.string.auth_client_registration_subtitle)
        )
        Spacer(Modifier.height(24.dp))
        InCleanHomeCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InCleanHomeTextField(name, { name = it }, stringResource(R.string.auth_name), enabled = !state.isLoading)
                InCleanHomeTextField(
                    email,
                    { email = it },
                    stringResource(R.string.auth_email),
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                InCleanHomeTextField(
                    password,
                    { password = it },
                    stringResource(R.string.auth_password),
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                InCleanHomeTextField(
                    phone,
                    { phone = it },
                    stringResource(R.string.auth_phone_optional),
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        TermsCheck(
            checked = accepted,
            onChecked = { accepted = it },
            onTerms = onTerms,
            onPrivacy = onPrivacy,
            enabled = !state.isLoading
        )
        (validationError ?: state.errorMessage)?.let {
            Spacer(Modifier.height(8.dp))
            AuthInlineError(it)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.auth_create_account_action),
            loading = state.isLoading,
            enabled = !state.isLoading,
            onClick = {
                validationError = validateCommon(name, email, password, accepted, commonValidation)
                if (validationError == null) {
                    viewModel.registerClient(
                        name.trim(), email.trim(), password,
                        phone.trim().takeIf(String::isNotEmpty), TERMS_VERSION
                    )
                }
            }
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun WorkerRegistrationScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var services by rememberSaveable { mutableStateOf("") }
    var zones by rememberSaveable { mutableStateOf("") }
    var hourlyRate by rememberSaveable { mutableStateOf("") }
    var experience by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val state by viewModel.uiState.collectAsState()
    val commonValidation = commonValidationMessages()
    val invalidGender = stringResource(R.string.validation_gender_invalid)
    val missingService = stringResource(R.string.validation_service_required)
    val invalidAge = stringResource(R.string.validation_age_invalid)
    val invalidRate = stringResource(R.string.validation_rate_invalid)
    val invalidExperience = stringResource(R.string.validation_experience_invalid)

    AuthScreen(modifier = modifier, onBack = onBack) {
        AuthTitle(
            title = stringResource(R.string.auth_worker_registration),
            subtitle = stringResource(R.string.auth_worker_registration_subtitle)
        )
        Spacer(Modifier.height(20.dp))

        FormSection(stringResource(R.string.auth_personal_information)) {
            InCleanHomeTextField(name, { name = it }, stringResource(R.string.auth_name), enabled = !state.isLoading)
            InCleanHomeTextField(
                email, { email = it }, stringResource(R.string.auth_email), enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            InCleanHomeTextField(
                password, { password = it }, stringResource(R.string.auth_password), enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()
            )
            InCleanHomeTextField(
                phone, { phone = it }, stringResource(R.string.auth_phone_optional), enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            InCleanHomeTextField(
                age, { age = it.filter(Char::isDigit) }, stringResource(R.string.auth_age), enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(stringResource(R.string.field_gender), style = MaterialTheme.typography.labelLarge, color = Navy)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthGender.VALUES.forEach { value ->
                    FilterChip(
                        selected = gender == value,
                        onClick = { gender = value },
                        label = { Text(presentationValue(value)) },
                        enabled = !state.isLoading
                    )
                }
            }
        }

        FormSection(stringResource(R.string.auth_professional_information)) {
            InCleanHomeTextField(
                hourlyRate, { hourlyRate = it }, stringResource(R.string.auth_hourly_rate), enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            InCleanHomeTextField(
                experience, { experience = it.filter(Char::isDigit) },
                stringResource(R.string.auth_experience_years), enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        FormSection(stringResource(R.string.auth_services)) {
            val serviceList = splitValues(services)
            Text(
                text = stringResource(R.string.auth_choose_services),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ServiceTypeSelector(
                selectedValues = serviceList,
                onSelectionChange = { services = it.joinToString(",") },
                enabled = !state.isLoading
            )
            if (serviceList.isEmpty()) {
                Text(
                    text = stringResource(R.string.auth_services_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FormSection(stringResource(R.string.auth_work_zones)) {
            InCleanHomeTextField(
                zones, { zones = it }, stringResource(R.string.auth_zones_csv), enabled = !state.isLoading,
                supportingText = { Text(stringResource(R.string.auth_zones_help)) }
            )
        }

        FormSection(stringResource(R.string.auth_about_me)) {
            InCleanHomeTextField(
                bio, { bio = it }, stringResource(R.string.auth_bio_optional), enabled = !state.isLoading,
                singleLine = false, minLines = 3, maxLines = 5
            )
        }

        Spacer(Modifier.height(4.dp))
        TermsCheck(
            checked = accepted,
            onChecked = { accepted = it },
            onTerms = onTerms,
            onPrivacy = onPrivacy,
            enabled = !state.isLoading
        )
        (validationError ?: state.errorMessage)?.let {
            Spacer(Modifier.height(8.dp))
            AuthInlineError(it)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.auth_create_account_action),
            loading = state.isLoading,
            enabled = !state.isLoading,
            onClick = {
                val serviceList = splitValues(services)
                val zoneList = splitValues(zones)
                validationError = when {
                    validateCommon(name, email, password, accepted, commonValidation) != null ->
                        validateCommon(name, email, password, accepted, commonValidation)
                    gender !in AuthGender.VALUES -> invalidGender
                    serviceList.isEmpty() -> missingService
                    age.toIntOrNull() == null -> invalidAge
                    hourlyRate.toBigDecimalOrNull() == null -> invalidRate
                    experience.toIntOrNull() == null -> invalidExperience
                    else -> null
                }
                if (validationError == null) {
                    viewModel.registerWorker(
                        name.trim(), email.trim(), password,
                        phone.trim().takeIf(String::isNotEmpty), age.toInt(), gender,
                        serviceList, zoneList, hourlyRate.toBigDecimal(), experience.toInt(),
                        bio.trim().takeIf(String::isNotEmpty), TERMS_VERSION
                    )
                }
            }
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun TermsAcceptanceScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    AuthScreen(modifier = modifier, onBack = onBack, verticalArrangement = Arrangement.Center) {
        AuthTitle(
            title = stringResource(R.string.auth_terms_title),
            subtitle = stringResource(R.string.auth_terms_subtitle)
        )
        Spacer(Modifier.height(24.dp))
        InCleanHomeCard {
            Text(
                text = stringResource(R.string.auth_terms_summary),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.auth_terms_current_version, TERMS_VERSION),
                style = MaterialTheme.typography.labelLarge,
                color = Navy
            )
        }
        Spacer(Modifier.height(16.dp))
        LegalDocumentLinks(
            onTerms = onTerms,
            onPrivacy = onPrivacy,
            enabled = !state.isLoading
        )
        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            AuthInlineError(it)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.auth_accept_continue),
            onClick = { viewModel.acceptTerms(TERMS_VERSION) },
            enabled = !state.isLoading,
            loading = state.isLoading
        )
    }
}

@Composable
private fun AccountTypeCard(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    InCleanHomeCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(GreenLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    AuthSectionTitle(title)
    Spacer(Modifier.height(8.dp))
    InCleanHomeCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TermsCheck(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    enabled: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onChecked(!checked) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onChecked, enabled = enabled)
            Text(
                text = stringResource(R.string.auth_accept_terms_version, TERMS_VERSION),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Navy
            )
        }
        Spacer(Modifier.height(8.dp))
        LegalDocumentLinks(
            onTerms = onTerms,
            onPrivacy = onPrivacy,
            enabled = enabled
        )
    }
}

private data class CommonValidationMessages(
    val nameRequired: String,
    val emailInvalid: String,
    val passwordRequired: String,
    val termsRequired: String
)

@Composable
private fun commonValidationMessages() = CommonValidationMessages(
    nameRequired = stringResource(R.string.validation_name_required),
    emailInvalid = stringResource(R.string.validation_email_invalid),
    passwordRequired = stringResource(R.string.validation_password_required),
    termsRequired = stringResource(R.string.validation_terms_required)
)

private fun validateCommon(
    name: String,
    email: String,
    password: String,
    accepted: Boolean,
    messages: CommonValidationMessages
): String? = when {
    name.isBlank() -> messages.nameRequired
    !email.contains("@") -> messages.emailInvalid
    password.isBlank() -> messages.passwordRequired
    !accepted -> messages.termsRequired
    else -> null
}

private fun splitValues(value: String): List<String> =
    value.split(',').map(String::trim).filter(String::isNotEmpty).distinct()

internal const val TERMS_VERSION = "v3"
