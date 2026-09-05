package com.incleanhome.mobile.iam.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.iam.data.AuthRepository
import com.incleanhome.mobile.iam.data.LoginNextStep
import com.incleanhome.mobile.iam.data.LoginResult
import com.incleanhome.mobile.iam.data.TwoFactorSetupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val nextStep: LoginNextStep? = null,
    val authenticatedMessage: String? = null,
    val twoFactorQrCodeDataUrl: String? = null,
    val twoFactorSecret: String? = null,
    val totpCode: String = "",
    val isTwoFactorSetupLoading: Boolean = false,
    val isTwoFactorEnableLoading: Boolean = false,
    val isTwoFactorVerifyLoading: Boolean = false,
    val twoFactorErrorMessage: String? = null,
    val authenticatedRole: String? = null
)

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var challengeToken: String? = null
    private var accessToken: String? = null

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, errorMessage = null, nextStep = null, authenticatedMessage = null)
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, errorMessage = null, nextStep = null, authenticatedMessage = null)
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        val email = state.email.trim()
        if (email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa tu email y contraseña.") }
            return
        }

        _uiState.update {
            it.copy(
                email = email,
                isLoading = true,
                errorMessage = null,
                nextStep = null,
                authenticatedMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = repository.login(email, state.password)) {
                is LoginResult.Challenge -> {
                    challengeToken = result.challengeToken
                    accessToken = null
                    _uiState.update {
                        it.copy(isLoading = false, nextStep = result.nextStep)
                    }
                }

                is LoginResult.Authenticated -> {
                    challengeToken = null
                    accessToken = result.token
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authenticatedMessage = "Sesión iniciada para ${result.user.name}."
                        )
                    }
                }

                is LoginResult.Error -> {
                    challengeToken = null
                    accessToken = null
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun loadTwoFactorSetup() {
        val state = _uiState.value
        val token = challengeToken
        if (
            state.nextStep != LoginNextStep.TWO_FA_SETUP ||
            token == null ||
            state.isTwoFactorSetupLoading ||
            state.twoFactorQrCodeDataUrl != null ||
            state.twoFactorSecret != null ||
            state.authenticatedRole != null
        ) {
            return
        }

        _uiState.update {
            it.copy(isTwoFactorSetupLoading = true, twoFactorErrorMessage = null)
        }

        viewModelScope.launch {
            when (val result = repository.setupTwoFactor(token)) {
                is TwoFactorSetupResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isTwoFactorSetupLoading = false,
                            twoFactorQrCodeDataUrl = result.setup.qrCodeDataUrl,
                            twoFactorSecret = result.setup.secret
                        )
                    }
                }

                is TwoFactorSetupResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isTwoFactorSetupLoading = false,
                            twoFactorErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun onTotpCodeChange(code: String) {
        val normalizedCode = code.filter(Char::isDigit).take(6)
        _uiState.update {
            it.copy(totpCode = normalizedCode, twoFactorErrorMessage = null)
        }
    }

    fun enableTwoFactor() {
        val state = _uiState.value
        val token = challengeToken
        if (state.isTwoFactorEnableLoading || token == null) return

        if (state.totpCode.length != 6) {
            _uiState.update {
                it.copy(twoFactorErrorMessage = "Ingresa un código TOTP de 6 dígitos.")
            }
            return
        }

        _uiState.update {
            it.copy(isTwoFactorEnableLoading = true, twoFactorErrorMessage = null)
        }

        viewModelScope.launch {
            when (val result = repository.enableTwoFactor(token, state.totpCode)) {
                is LoginResult.Authenticated -> {
                    accessToken = result.token
                    challengeToken = null
                    _uiState.update {
                        it.copy(
                            isTwoFactorEnableLoading = false,
                            authenticatedRole = result.user.role
                        )
                    }
                }

                is LoginResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isTwoFactorEnableLoading = false,
                            twoFactorErrorMessage = result.message
                        )
                    }
                }

                is LoginResult.Challenge -> {
                    _uiState.update {
                        it.copy(
                            isTwoFactorEnableLoading = false,
                            twoFactorErrorMessage = "El servidor devolvió una respuesta inesperada."
                        )
                    }
                }
            }
        }
    }

    fun verifyTwoFactor() {
        val state = _uiState.value
        val token = challengeToken
        if (state.isTwoFactorVerifyLoading || token == null) return

        if (state.totpCode.length != 6) {
            _uiState.update {
                it.copy(twoFactorErrorMessage = "Ingresa un código TOTP de 6 dígitos.")
            }
            return
        }

        _uiState.update {
            it.copy(isTwoFactorVerifyLoading = true, twoFactorErrorMessage = null)
        }

        viewModelScope.launch {
            when (val result = repository.verifyTwoFactor(token, state.totpCode)) {
                is LoginResult.Authenticated -> {
                    accessToken = result.token
                    challengeToken = null
                    _uiState.update {
                        it.copy(
                            isTwoFactorVerifyLoading = false,
                            authenticatedRole = result.user.role
                        )
                    }
                }

                is LoginResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isTwoFactorVerifyLoading = false,
                            twoFactorErrorMessage = result.message
                        )
                    }
                }

                is LoginResult.Challenge -> {
                    _uiState.update {
                        it.copy(
                            isTwoFactorVerifyLoading = false,
                            twoFactorErrorMessage = "El servidor devolvió una respuesta inesperada."
                        )
                    }
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    return LoginViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
