package com.incleanhome.mobile.iam.data

import com.incleanhome.mobile.core.network.RetrofitClient
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

enum class LoginNextStep(val label: String) {
    TERMS("TERMS"),
    TWO_FA_SETUP("2FA_SETUP"),
    TWO_FA_VERIFY("2FA_VERIFY")
}

sealed interface LoginResult {
    data class Challenge(
        val nextStep: LoginNextStep,
        val challengeToken: String
    ) : LoginResult

    data class Authenticated(
        val user: AuthUser,
        val token: String
    ) : LoginResult

    data class MembershipBlocked(
        val status: String,
        val message: String?,
        val whatsappLink: String?
    ) : LoginResult

    data class Error(val message: String) : LoginResult
}

sealed interface TwoFactorSetupResult {
    data class Success(val setup: TwoFactorSetupResponse) : TwoFactorSetupResult
    data class MembershipBlocked(
        val status: String,
        val message: String?,
        val whatsappLink: String?
    ) : TwoFactorSetupResult
    data class Error(val message: String) : TwoFactorSetupResult
}

class AuthRepository(
    private val api: AuthApi = RetrofitClient.retrofit.create(AuthApi::class.java)
) {
    suspend fun registerClient(request: RegisterClientRequest): LoginResult =
        authenticateCall { api.registerClient(request) }

    suspend fun registerWorker(request: RegisterWorkerRequest): LoginResult =
        authenticateCall { api.registerWorker(request) }

    suspend fun acceptTerms(challengeToken: String, version: String): LoginResult =
        authenticateCall {
            api.acceptTerms(challengeToken.asBearerToken(), AcceptTermsRequest(version))
        }

    suspend fun login(email: String, password: String): LoginResult {
        return try {
            mapResponse(api.login(LoginRequest(email = email, password = password)))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            exception.membershipResultFromErrorBody() ?: run {
                val message = when (exception.code()) {
                    400, 401 -> "Email o contraseña incorrectos."
                    else -> "No se pudo iniciar sesión. Inténtalo nuevamente."
                }
                LoginResult.Error(message)
            }
        } catch (exception: IOException) {
            LoginResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            LoginResult.Error("Ocurrió un error inesperado.")
        }
    }

    private suspend fun authenticateCall(call: suspend () -> AuthResponse): LoginResult {
        return try {
            mapResponse(call())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            val errorBody = exception.errorBodyText()
            membershipResultFromJson(errorBody) ?: run {
                val bodyMessage = runCatching {
                    com.google.gson.JsonParser.parseString(errorBody)
                        .asJsonObject
                        .get("error")
                        ?.asString
                }.getOrNull()
                LoginResult.Error(bodyMessage ?: when (exception.code()) {
                    400, 401 -> "Los datos no son válidos o el desafío expiró."
                    else -> "No se pudo completar la autenticación."
                })
            }
        } catch (exception: IOException) {
            LoginResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            LoginResult.Error("Ocurrió un error inesperado.")
        }
    }

    suspend fun setupTwoFactor(challengeToken: String): TwoFactorSetupResult {
        return try {
            val response = api.setupTwoFactor(authorization = challengeToken.asBearerToken())
            membershipBlock(
                membershipPending = response.membershipPending,
                membershipStatus = response.membershipStatus,
                message = response.message,
                whatsappLink = response.whatsappLink
            )?.let {
                TwoFactorSetupResult.MembershipBlocked(it.status, it.message, it.whatsappLink)
            } ?: if (!response.qrCodeDataUrl.isNullOrBlank() && !response.secret.isNullOrBlank()) {
                TwoFactorSetupResult.Success(response)
            } else {
                TwoFactorSetupResult.Error("El servidor devolvió una respuesta de autenticación incompleta.")
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            exception.membershipResultFromErrorBody()?.let {
                TwoFactorSetupResult.MembershipBlocked(it.status, it.message, it.whatsappLink)
            } ?: run {
                val message = if (exception.code() == 401) {
                    "El desafío de autenticación expiró. Inicia sesión nuevamente."
                } else {
                    "No se pudo configurar la autenticación en dos pasos."
                }
                TwoFactorSetupResult.Error(message)
            }
        } catch (exception: IOException) {
            TwoFactorSetupResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            TwoFactorSetupResult.Error("Ocurrió un error inesperado.")
        }
    }

    suspend fun enableTwoFactor(challengeToken: String, code: String): LoginResult {
        return try {
            mapResponse(
                api.enableTwoFactor(
                    authorization = challengeToken.asBearerToken(),
                    request = Enable2faRequest(code = code)
                )
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            exception.membershipResultFromErrorBody() ?: run {
                val message = when (exception.code()) {
                    400 -> "El código de verificación no es válido."
                    401 -> "El desafío de autenticación expiró. Inicia sesión nuevamente."
                    else -> "No se pudo habilitar la autenticación en dos pasos."
                }
                LoginResult.Error(message)
            }
        } catch (exception: IOException) {
            LoginResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            LoginResult.Error("Ocurrió un error inesperado.")
        }
    }

    suspend fun verifyTwoFactor(challengeToken: String, code: String): LoginResult {
        return try {
            mapResponse(
                api.verifyTwoFactor(
                    Verify2faRequest(
                        challengeToken = challengeToken,
                        code = code
                    )
                )
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            exception.membershipResultFromErrorBody() ?: run {
                val message = when (exception.code()) {
                    400 -> "El código de verificación no es válido."
                    401 -> "El desafío de autenticación expiró. Inicia sesión nuevamente."
                    else -> "No se pudo verificar la autenticación en dos pasos."
                }
                LoginResult.Error(message)
            }
        } catch (exception: IOException) {
            LoginResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            LoginResult.Error("Ocurrió un error inesperado.")
        }
    }

    private fun mapResponse(response: AuthResponse): LoginResult {
        return interpretAuthResponse(response)
    }
}

internal fun interpretAuthResponse(response: AuthResponse): LoginResult {
        membershipBlock(
            membershipPending = response.membershipPending,
            membershipStatus = response.membershipStatus,
            message = response.message,
            whatsappLink = response.whatsappLink
        )?.let { return it }

        val nextStep = when {
            response.requiresTermsAcceptance == true -> LoginNextStep.TERMS
            response.requires2faSetup == true -> LoginNextStep.TWO_FA_SETUP
            response.requires2fa == true -> LoginNextStep.TWO_FA_VERIFY
            else -> null
        }

        if (nextStep != null) {
            val challengeToken = response.challengeToken
            return if (challengeToken.isNullOrBlank()) {
                LoginResult.Error("El servidor devolvió una respuesta de autenticación incompleta.")
            } else {
                LoginResult.Challenge(nextStep, challengeToken)
            }
        }

        val user = response.user
        val token = response.token
        return if (user != null && !token.isNullOrBlank()) {
            LoginResult.Authenticated(user, token)
        } else {
            LoginResult.Error("El servidor devolvió una respuesta de autenticación desconocida.")
        }
    }

private fun membershipBlock(
    membershipPending: Boolean?,
    membershipStatus: String?,
    message: String?,
    whatsappLink: String?
): LoginResult.MembershipBlocked? {
    val normalizedStatus = membershipStatus?.trim()?.lowercase()
    val isBlocked = membershipPending == true ||
        (normalizedStatus != null && normalizedStatus != MEMBERSHIP_ACTIVE)
    if (!isBlocked) return null

    return LoginResult.MembershipBlocked(
        status = if (membershipPending == true && normalizedStatus == MEMBERSHIP_ACTIVE) {
            MEMBERSHIP_PENDING
        } else {
            normalizedStatus ?: MEMBERSHIP_PENDING
        },
        message = message?.trim()?.takeIf(String::isNotEmpty),
        whatsappLink = whatsappLink?.takeIf(String::isNotBlank)
    )
}

internal const val MEMBERSHIP_ACTIVE = "active"
internal const val MEMBERSHIP_PENDING = "pending"

private fun HttpException.membershipResultFromErrorBody(): LoginResult.MembershipBlocked? =
    membershipResultFromJson(errorBodyText())

private fun membershipResultFromJson(json: String): LoginResult.MembershipBlocked? = runCatching {
    val response = com.google.gson.Gson().fromJson(json, AuthResponse::class.java)
    membershipBlock(
        membershipPending = response.membershipPending,
        membershipStatus = response.membershipStatus,
        message = response.message,
        whatsappLink = response.whatsappLink
    )
}.getOrNull()

private fun HttpException.errorBodyText(): String = runCatching {
    response()?.errorBody()?.string().orEmpty()
}.getOrDefault("")

private fun String.asBearerToken(): String = "Bearer $this"
