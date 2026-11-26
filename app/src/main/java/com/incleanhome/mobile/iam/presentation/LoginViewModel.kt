package com.incleanhome.mobile.iam.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.core.session.SessionManager
import com.incleanhome.mobile.core.session.UserSession
import com.incleanhome.mobile.iam.data.AuthRepository
import com.incleanhome.mobile.iam.data.RegisterClientRequest
import com.incleanhome.mobile.iam.data.RegisterWorkerRequest
import com.incleanhome.mobile.iam.data.LoginNextStep
import com.incleanhome.mobile.iam.data.LoginResult
import com.incleanhome.mobile.iam.data.TwoFactorSetupResult
import kotlinx.coroutines.CancellationException
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
    val authenticatedRole: String? = null,
    val membershipStatus: String? = null,
    val membershipMessage: String? = null,
    val membershipWhatsappLink: String? = null
)

class LoginViewModel(
    private val sessionManager: SessionManager,
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var challengeToken: String? = null

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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            nextStep = result.nextStep,
                            totpCode = "",
                            twoFactorQrCodeDataUrl = null,
                            twoFactorSecret = null,
                            twoFactorErrorMessage = null,
                            authenticatedRole = null
                        )
                    }
                }

                is LoginResult.Authenticated -> {
                    val persistenceError = persistAuthenticatedSession(result)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = persistenceError,
                            authenticatedMessage = if (persistenceError == null) {
                                "Sesión iniciada para ${result.user.name}."
                            } else {
                                null
                            }
                        )
                    }
                }

                is LoginResult.MembershipBlocked -> recordMembershipBlock(result) {
                    it.copy(isLoading = false)
                }

                is LoginResult.Error -> {
                    challengeToken = null
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun registerClient(
        name: String, email: String, password: String, phone: String?, termsVersion: String
    ) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, nextStep = null) }
        viewModelScope.launch {
            when (val result = repository.registerClient(
                RegisterClientRequest(name, email, password, phone, termsVersion)
            )) {
                is LoginResult.Challenge -> {
                    challengeToken = result.challengeToken
                    _uiState.update { it.copy(isLoading = false, nextStep = result.nextStep) }
                }
                is LoginResult.Authenticated -> {
                    val error = persistAuthenticatedSession(result)
                    _uiState.update { it.copy(isLoading = false, errorMessage = error) }
                }
                is LoginResult.MembershipBlocked -> recordMembershipBlock(result) {
                    it.copy(isLoading = false)
                }
                is LoginResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun registerWorker(
        name: String, email: String, password: String, phone: String?, age: Int,
        gender: String, serviceTypes: List<String>, zones: List<String>,
        hourlyRate: java.math.BigDecimal, experienceYears: Int, bio: String?, termsVersion: String
    ) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, nextStep = null) }
        viewModelScope.launch {
            when (val result = repository.registerWorker(
                RegisterWorkerRequest(
                    name, email, password, phone, age, gender, serviceTypes, zones,
                    hourlyRate, experienceYears, bio, termsVersion
                )
            )) {
                is LoginResult.Challenge -> {
                    challengeToken = result.challengeToken
                    _uiState.update { it.copy(isLoading = false, nextStep = result.nextStep) }
                }
                is LoginResult.Authenticated -> {
                    val error = persistAuthenticatedSession(result)
                    _uiState.update { it.copy(isLoading = false, errorMessage = error) }
                }
                is LoginResult.MembershipBlocked -> recordMembershipBlock(result) {
                    it.copy(isLoading = false)
                }
                is LoginResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun acceptTerms(version: String) {
        val token = challengeToken
        if (_uiState.value.isLoading || token.isNullOrBlank()) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, nextStep = null) }
        viewModelScope.launch {
            when (val result = repository.acceptTerms(token, version)) {
                is LoginResult.Challenge -> {
                    challengeToken = result.challengeToken
                    _uiState.update { it.copy(isLoading = false, nextStep = result.nextStep) }
                }
                is LoginResult.Authenticated -> {
                    val error = persistAuthenticatedSession(result)
                    _uiState.update { it.copy(isLoading = false, errorMessage = error) }
                }
                is LoginResult.MembershipBlocked -> recordMembershipBlock(result) {
                    it.copy(isLoading = false)
                }
                is LoginResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
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

                is TwoFactorSetupResult.MembershipBlocked -> {
                    challengeToken = null
                    _uiState.update {
                        it.copy(
                            isTwoFactorSetupLoading = false,
                            nextStep = null,
                            membershipStatus = result.status,
                            membershipMessage = result.message,
                            membershipWhatsappLink = result.whatsappLink
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
                    val persistenceError = persistAuthenticatedSession(result)
                    _uiState.update {
                        it.copy(
                            isTwoFactorEnableLoading = false,
                            twoFactorErrorMessage = persistenceError,
                            authenticatedRole = if (persistenceError == null) {
                                result.user.role.lowercase()
                            } else {
                                null
                            }
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

                is LoginResult.MembershipBlocked -> recordMembershipBlock(result) {
                    it.copy(isTwoFactorEnableLoading = false)
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
                    val persistenceError = persistAuthenticatedSession(result)
                    _uiState.update {
                        it.copy(
                            isTwoFactorVerifyLoading = false,
                            twoFactorErrorMessage = persistenceError,
                            authenticatedRole = if (persistenceError == null) {
                                result.user.role.lowercase()
                            } else {
                                null
                            }
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

                is LoginResult.MembershipBlocked -> recordMembershipBlock(result) {
                    it.copy(isTwoFactorVerifyLoading = false)
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

    fun clearAuthenticationState() {
        challengeToken = null
        _uiState.value = LoginUiState()
    }

    private fun recordMembershipBlock(
        result: LoginResult.MembershipBlocked,
        clearLoading: (LoginUiState) -> LoginUiState
    ) {
        challengeToken = null
        _uiState.update {
            clearLoading(it).copy(
                nextStep = null,
                authenticatedRole = null,
                authenticatedMessage = null,
                membershipStatus = result.status,
                membershipMessage = result.message,
                membershipWhatsappLink = result.whatsappLink
            )
        }
    }

    private suspend fun persistAuthenticatedSession(result: LoginResult.Authenticated): String? {
        val normalizedRole = result.user.role.lowercase()
        if (
            normalizedRole != SessionManager.CLIENT_ROLE &&
            normalizedRole != SessionManager.WORKER_ROLE &&
            normalizedRole != SessionManager.ADMIN_ROLE
        ) {
            return "El servidor devolvió un rol de usuario desconocido."
        }

        return try {
            sessionManager.saveSession(
                UserSession(
                    userId = result.user.id,
                    role = normalizedRole,
                    name = result.user.name,
                    email = result.user.email,
                    token = result.token
                )
            )
            challengeToken = null
            null
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            "No se pudo guardar la sesión de forma segura."
        }
    }

    companion object {
        fun Factory(sessionManager: SessionManager): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                        return LoginViewModel(sessionManager = sessionManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
