package com.incleanhome.mobile.iam.presentation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TwoFactorSetupScreen(
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by loginViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(loginViewModel) {
        loginViewModel.loadTwoFactorSetup()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val authenticatedRole = uiState.authenticatedRole
        if (authenticatedRole != null) {
            Text(
                text = "Autenticación completada",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Rol: $authenticatedRole",
                style = MaterialTheme.typography.titleMedium
            )
            return@Column
        }

        Text(
            text = "Configura la autenticación en dos pasos",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Escanea el código QR con tu aplicación de autenticación y confirma el código generado."
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isTwoFactorSetupLoading) {
            CircularProgressIndicator()
        } else {
            uiState.twoFactorQrCodeDataUrl?.let { dataUrl ->
                val qrBitmap = remember(dataUrl) { decodeQrDataUrl(dataUrl) }
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Código QR para configurar 2FA",
                        modifier = Modifier.size(220.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            uiState.twoFactorSecret?.let { secret ->
                Text(
                    text = "Clave para ingreso manual",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = secret,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.totpCode,
                    onValueChange = loginViewModel::onTotpCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código TOTP") },
                    supportingText = { Text("6 dígitos") },
                    singleLine = true,
                    enabled = !uiState.isTwoFactorEnableLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            loginViewModel.enableTwoFactor()
                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        loginViewModel.enableTwoFactor()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.totpCode.length == 6 && !uiState.isTwoFactorEnableLoading
                ) {
                    if (uiState.isTwoFactorEnableLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Habilitar 2FA")
                    }
                }
            }
        }

        uiState.twoFactorErrorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
            if (
                uiState.twoFactorSecret == null &&
                !uiState.isTwoFactorSetupLoading
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = loginViewModel::loadTwoFactorSetup) {
                    Text("Reintentar")
                }
            }
        }
    }
}

private fun decodeQrDataUrl(dataUrl: String) = runCatching {
    val encodedImage = dataUrl.substringAfter("base64,", missingDelimiterValue = "")
    require(encodedImage.isNotEmpty())
    val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}.getOrNull()
