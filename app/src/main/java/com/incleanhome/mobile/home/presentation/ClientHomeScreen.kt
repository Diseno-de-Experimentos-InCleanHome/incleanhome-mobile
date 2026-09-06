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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.format.presentationValue

@Composable
fun ClientHomeScreen(
    onSearchWorkers: () -> Unit,
    onBookings: () -> Unit,
    onMessages: () -> Unit,
    onEvents: () -> Unit,
    onProfile: () -> Unit,
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
        Text(stringResource(R.string.home_welcome), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.home_role, presentationValue("client")), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSearchWorkers) {
            Text("Buscar trabajadores")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onBookings) {
            Text("Mis reservas")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onMessages) {
            Text(stringResource(R.string.home_messages))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onEvents) {
            Text("Mis eventos")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onProfile) { Text("Mi perfil") }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onLogout) {
            Text(stringResource(R.string.home_logout))
        }
    }
}
