package com.incleanhome.mobile.iam.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.iam.data.AuthGender
import java.math.BigDecimal

@Composable
fun AccountTypeScreen(
    onClient: () -> Unit,
    onWorker: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClient, modifier = Modifier.fillMaxWidth()) { Text("Soy cliente") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onWorker, modifier = Modifier.fillMaxWidth()) { Text("Soy trabajador/a") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

@Composable
fun ClientRegistrationScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val state by viewModel.uiState.collectAsState()
    RegistrationColumn("Registro de cliente", onBack, modifier) {
        Field(name, { name = it }, "Nombre", state.isLoading)
        Field(email, { email = it }, "Email", state.isLoading)
        Field(password, { password = it }, "Contraseña", state.isLoading, password = true)
        Field(phone, { phone = it }, "Teléfono (opcional)", state.isLoading)
        TermsCheck(accepted, { accepted = it })
        validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        SubmitButton(state.isLoading) {
            validationError = validateCommon(name, email, password, accepted)
            if (validationError == null) {
                viewModel.registerClient(
                    name.trim(), email.trim(), password,
                    phone.trim().takeIf(String::isNotEmpty), TERMS_VERSION
                )
            }
        }
    }
}

@Composable
fun WorkerRegistrationScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
    RegistrationColumn("Registro de trabajador/a", onBack, modifier) {
        Field(name, { name = it }, "Nombre", state.isLoading)
        Field(email, { email = it }, "Email", state.isLoading)
        Field(password, { password = it }, "Contraseña", state.isLoading, password = true)
        Field(phone, { phone = it }, "Teléfono (opcional)", state.isLoading)
        Field(age, { age = it.filter(Char::isDigit) }, "Edad", state.isLoading)
        Text("Género", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthGender.VALUES.forEach { value ->
                FilterChip(
                    selected = gender == value,
                    onClick = { gender = value },
                    label = { Text(value) },
                    enabled = !state.isLoading
                )
            }
        }
        Field(services, { services = it }, "Servicios separados por coma", state.isLoading)
        Field(zones, { zones = it }, "Zonas separadas por coma (opcional)", state.isLoading)
        Field(hourlyRate, { hourlyRate = it }, "Tarifa por hora", state.isLoading)
        Field(experience, { experience = it.filter(Char::isDigit) }, "Años de experiencia", state.isLoading)
        Field(bio, { bio = it }, "Biografía (opcional)", state.isLoading, minLines = 3)
        TermsCheck(accepted, { accepted = it })
        validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        SubmitButton(state.isLoading) {
            val serviceList = splitValues(services)
            val zoneList = splitValues(zones)
            validationError = when {
                validateCommon(name, email, password, accepted) != null -> validateCommon(name, email, password, accepted)
                gender !in AuthGender.VALUES -> "Selecciona un género válido."
                serviceList.isEmpty() -> "Ingresa al menos un servicio."
                age.toIntOrNull() == null -> "Ingresa una edad válida."
                hourlyRate.toBigDecimalOrNull() == null -> "Ingresa una tarifa válida."
                experience.toIntOrNull() == null -> "Ingresa los años de experiencia."
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
    }
}

@Composable
fun TermsAcceptanceScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Actualización de términos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("Debes aceptar los términos y condiciones vigentes para continuar.")
        Spacer(Modifier.height(12.dp))
        Text("Versión vigente: $TERMS_VERSION")
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.acceptTerms(TERMS_VERSION) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) CircularProgressIndicator(strokeWidth = 2.dp)
            else Text("Aceptar términos")
        }
        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, enabled = !state.isLoading) { Text("Volver") }
    }
}

@Composable
private fun RegistrationColumn(
    title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack) { Text("Volver") }
                Text(title, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.headlineSmall)
            }
            content()
        }
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, disabled: Boolean, minLines: Int = 1, password: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, enabled = !disabled, minLines = minLines,
        maxLines = if (minLines > 1) 5 else 1,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
}

@Composable
private fun TermsCheck(checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text("Acepto los términos y condiciones ($TERMS_VERSION)")
    }
}

@Composable
private fun SubmitButton(loading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
        if (loading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Crear cuenta")
    }
}

private fun validateCommon(name: String, email: String, password: String, accepted: Boolean): String? = when {
    name.isBlank() -> "El nombre es obligatorio."
    !email.contains("@") -> "Ingresa un email válido."
    password.isBlank() -> "La contraseña es obligatoria."
    !accepted -> "Debes aceptar los términos y condiciones."
    else -> null
}

private fun splitValues(value: String): List<String> = value.split(',').map(String::trim).filter(String::isNotEmpty).distinct()

private const val TERMS_VERSION = "v2"
