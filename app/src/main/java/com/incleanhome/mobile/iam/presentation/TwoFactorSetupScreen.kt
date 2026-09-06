package com.incleanhome.mobile.iam.presentation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen

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

    AuthScreen(modifier = modifier) {
        AuthBrand()
        Spacer(Modifier.height(32.dp))
        AuthTitle(
            title = stringResource(R.string.auth_protect_account),
            subtitle = stringResource(R.string.auth_2fa_setup_subtitle)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.auth_2fa_step_scan),
            style = MaterialTheme.typography.bodyLarge,
            color = Navy
        )
        Spacer(Modifier.height(16.dp))

        if (uiState.isTwoFactorSetupLoading) {
            LoadingState(Modifier.padding(vertical = 40.dp))
        } else {
            uiState.twoFactorQrCodeDataUrl?.let { dataUrl ->
                val qrBitmap = remember(dataUrl) { decodeQrDataUrl(dataUrl) }
                qrBitmap?.let { bitmap ->
                    InCleanHomeCard {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.auth_2fa_qr_description),
                                modifier = Modifier.size(220.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            uiState.twoFactorSecret?.let { secret ->
                Text(
                    text = stringResource(R.string.auth_manual_secret),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = secret,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = PrimaryGreen
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.auth_2fa_step_code),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Navy
                )
                Spacer(Modifier.height(12.dp))
                InCleanHomeTextField(
                    value = uiState.totpCode,
                    onValueChange = loginViewModel::onTotpCodeChange,
                    label = stringResource(R.string.auth_totp_code),
                    supportingText = { Text(stringResource(R.string.auth_six_digits)) },
                    enabled = !uiState.isTwoFactorEnableLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        loginViewModel.enableTwoFactor()
                    })
                )
                uiState.twoFactorErrorMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    AuthInlineError(message)
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = stringResource(R.string.auth_enable_2fa),
                    onClick = {
                        focusManager.clearFocus()
                        loginViewModel.enableTwoFactor()
                    },
                    enabled = uiState.totpCode.length == 6 && !uiState.isTwoFactorEnableLoading,
                    loading = uiState.isTwoFactorEnableLoading
                )
            }
        }

        if (uiState.twoFactorSecret == null) {
            uiState.twoFactorErrorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                AuthInlineError(message)
                if (!uiState.isTwoFactorSetupLoading) {
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(
                        text = stringResource(R.string.action_retry),
                        onClick = loginViewModel::loadTwoFactorSetup,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun decodeQrDataUrl(dataUrl: String) = runCatching {
    val encodedImage = dataUrl.substringAfter("base64,", missingDelimiterValue = "")
    require(encodedImage.isNotEmpty())
    val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}.getOrNull()
