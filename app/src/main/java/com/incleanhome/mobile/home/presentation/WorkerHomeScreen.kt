package com.incleanhome.mobile.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WorkerHomeScreen(
    onProfile: () -> Unit,
    onAvailability: () -> Unit,
    onRequests: () -> Unit,
    onMessages: () -> Unit,
    onReviews: () -> Unit,
    onEvents: () -> Unit,
    onEventApplications: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Rol: worker", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onProfile) {
            Text("Mi perfil")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onAvailability) {
            Text("Mi disponibilidad")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRequests) {
            Text("Solicitudes")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onMessages) {
            Text("Mensajes")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onReviews) {
            Text("Mis reseñas")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onEvents) {
            Text("Eventos disponibles")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onEventApplications) {
            Text("Mis postulaciones")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onLogout) {
            Text("Cerrar sesión")
        }
    }
}
