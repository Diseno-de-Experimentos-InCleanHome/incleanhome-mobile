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
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.worker.data.WorkerProfile

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text("Volver")
            }
            Text(
                text = "Mi perfil",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = viewModel::loadProfile) {
                    Text("Reintentar")
                }
            }

            uiState.profile != null -> uiState.profile?.let { profile ->
                WorkerProfileContent(profile)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onEdit) { Text("Editar perfil") }
            }
        }
    }
}

@Composable
private fun WorkerProfileContent(profile: WorkerProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(profile.name, style = MaterialTheme.typography.headlineMedium)
        profile.phone?.takeIf(String::isNotBlank)?.let { Text("Teléfono: $it") }
        Text("Edad: ${profile.age}")
        Text("Género: ${profile.gender}")
        Text("Servicios: ${profile.serviceTypes.joinToString()}")
        Text("Zonas: ${profile.zones.joinToString()}")
        Text("Tarifa por hora: ${profile.hourlyRate.toPlainString()}")
        Text("Experiencia: ${profile.experienceYears} años")
        Text("Biografía: ${profile.bio}")
        Text("Calificación: ${profile.averageRating.toPlainString()}")
        Text("Servicios realizados: ${profile.totalServices}")
    }
}
