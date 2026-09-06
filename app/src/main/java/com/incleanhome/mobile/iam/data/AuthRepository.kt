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

    data class Error(val message: String) : LoginResult
}

sealed interface TwoFactorSetupResult {
    data class Success(val setup: TwoFactorSetupResponse) : TwoFactorSetupResult
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
            val message = when (exception.code()) {
                400, 401 -> "Email o contraseña incorrectos."
                else -> "No se pudo iniciar sesión. Inténtalo nuevamente."
            }
            LoginResult.Error(message)
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
            val bodyMessage = runCatching {
                val body = exception.response()?.errorBody()?.string().orEmpty()
                com.google.gson.JsonParser.parseString(body).asJsonObject.get("error")?.asString
            }.getOrNull()
            LoginResult.Error(bodyMessage ?: when (exception.code()) {
                400, 401 -> "Los datos no son válidos o el desafío expiró."
                else -> "No se pudo completar la autenticación."
            })
        } catch (exception: IOException) {
            LoginResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            LoginResult.Error("Ocurrió un error inesperado.")
        }
    }

    suspend fun setupTwoFactor(challengeToken: String): TwoFactorSetupResult {
        return try {
            val response = api.setupTwoFactor(authorization = challengeToken.asBearerToken())
            TwoFactorSetupResult.Success(response)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            val message = if (exception.code() == 401) {
                "El desafío de autenticación expiró. Inicia sesión nuevamente."
            } else {
                "No se pudo configurar la autenticación en dos pasos."
            }
            TwoFactorSetupResult.Error(message)
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
            val message = when (exception.code()) {
                400 -> "El código de verificación no es válido."
                401 -> "El desafío de autenticación expiró. Inicia sesión nuevamente."
                else -> "No se pudo habilitar la autenticación en dos pasos."
            }
            LoginResult.Error(message)
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
            val message = when (exception.code()) {
                400 -> "El código de verificación no es válido."
                401 -> "El desafío de autenticación expiró. Inicia sesión nuevamente."
                else -> "No se pudo verificar la autenticación en dos pasos."
            }
            LoginResult.Error(message)
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

private fun String.asBearerToken(): String = "Bearer $this"
