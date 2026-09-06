package com.incleanhome.mobile.iam.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.PrimaryButton

@Composable
fun TwoFactorVerifyScreen(
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by loginViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    AuthScreen(modifier = modifier, verticalArrangement = Arrangement.Center) {
        AuthBrand()
        Spacer(Modifier.height(40.dp))
        AuthTitle(
            title = stringResource(R.string.auth_2fa_verify_title),
            subtitle = stringResource(R.string.auth_2fa_verify_subtitle)
        )
        Spacer(Modifier.height(28.dp))
        InCleanHomeTextField(
            value = uiState.totpCode,
            onValueChange = loginViewModel::onTotpCodeChange,
            label = stringResource(R.string.auth_totp_code),
            supportingText = { Text(stringResource(R.string.auth_six_digits)) },
            enabled = !uiState.isTwoFactorVerifyLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                loginViewModel.verifyTwoFactor()
            })
        )
        uiState.twoFactorErrorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            AuthInlineError(message)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.auth_verify),
            onClick = {
                focusManager.clearFocus()
                loginViewModel.verifyTwoFactor()
            },
            enabled = uiState.totpCode.length == 6 && !uiState.isTwoFactorVerifyLoading,
            loading = uiState.isTwoFactorVerifyLoading
        )
    }
}
