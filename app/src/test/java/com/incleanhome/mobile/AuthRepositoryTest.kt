package com.incleanhome.mobile

import com.incleanhome.mobile.iam.data.AcceptTermsRequest
import com.incleanhome.mobile.iam.data.AuthApi
import com.incleanhome.mobile.iam.data.AuthRepository
import com.incleanhome.mobile.iam.data.AuthResponse
import com.incleanhome.mobile.iam.data.AuthUser
import com.incleanhome.mobile.iam.data.Enable2faRequest
import com.incleanhome.mobile.iam.data.LoginRequest
import com.incleanhome.mobile.iam.data.LoginResult
import com.incleanhome.mobile.iam.data.RegisterClientRequest
import com.incleanhome.mobile.iam.data.RegisterWorkerRequest
import com.incleanhome.mobile.iam.data.TwoFactorSetupResponse
import com.incleanhome.mobile.iam.data.TwoFactorSetupResult
import com.incleanhome.mobile.iam.data.Verify2faRequest
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    /* USER STORY 01 - Registro como cliente */
    @Test
    fun testRegisterClient_EnviaDatosCorrectos() = runBlocking {
        //Arrange
        val api = FakeAuthApi()
        api.authResponse = AuthResponse(requires2faSetup = true, challengeToken = "token-client")
        val repository = AuthRepository(api)
        val request = RegisterClientRequest(
            name = "Ana",
            email = "ana@mail.com",
            password = "Clave123",
            phone = "999111222",
            acceptedTermsVersion = "v1"
        )

        //Act
        val resultado = repository.registerClient(request)

        //Assert
        assertEquals(request, api.lastClientRequest)
        assertTrue(resultado is LoginResult.Challenge)
    }

    /* USER STORY 01 - Registro como prestador */
    @Test
    fun testRegisterWorker_EnviaRolYServiciosCorrectos() = runBlocking {
        //Arrange
        val api = FakeAuthApi()
        api.authResponse = AuthResponse(requires2faSetup = true, challengeToken = "token-worker")
        val repository = AuthRepository(api)
        val request = RegisterWorkerRequest(
            name = "Luis",
            email = "luis@mail.com",
            password = "Clave123",
            phone = null,
            age = 28,
            gender = "male",
            serviceTypes = listOf("limpieza_general", "jardineria"),
            zones = listOf("San Isidro"),
            hourlyRate = BigDecimal("35.00"),
            experienceYears = 3,
            bio = "Experiencia en servicios para el hogar",
            acceptedTermsVersion = "v1"
        )

        //Act
        val resultado = repository.registerWorker(request)

        //Assert
        assertEquals(request, api.lastWorkerRequest)
        assertEquals(2, api.lastWorkerRequest?.serviceTypes?.size)
        assertTrue(resultado is LoginResult.Challenge)
    }

    /* USER STORY 02 - Inicio de sesión */
    @Test
    fun testLogin_CredencialesCorrectasDevuelvenUsuarioAutenticado() = runBlocking {
        //Arrange
        val api = FakeAuthApi()
        api.authResponse = AuthResponse(
            user = AuthUser(5, "user@mail.com", "client", "Usuario", null),
            token = "jwt-123"
        )
        val repository = AuthRepository(api)

        //Act
        val resultado = repository.login("user@mail.com", "Clave123")

        //Assert
        assertEquals(LoginRequest("user@mail.com", "Clave123"), api.lastLoginRequest)
        assertTrue(resultado is LoginResult.Authenticated)
    }

    /* USER STORY 04 - Configuración de 2FA */
    @Test
    fun testSetupTwoFactor_UsaTokenBearerYDevuelveSecreto() = runBlocking {
        //Arrange
        val api = FakeAuthApi()
        api.twoFactorSetup = TwoFactorSetupResponse(
            qrCodeDataUrl = "data:image/png;base64,abc",
            secret = "ABCDEF"
        )
        val repository = AuthRepository(api)

        //Act
        val resultado = repository.setupTwoFactor("challenge-token")

        //Assert
        assertEquals("Bearer challenge-token", api.lastAuthorization)
        assertTrue(resultado is TwoFactorSetupResult.Success)
        resultado as TwoFactorSetupResult.Success
        assertEquals("ABCDEF", resultado.setup.secret)
    }

    /* USER STORY 04 - Verificación de 2FA */
    @Test
    fun testVerifyTwoFactor_EnviaChallengeYCodigo() = runBlocking {
        //Arrange
        val api = FakeAuthApi()
        api.authResponse = AuthResponse(
            user = AuthUser(7, "worker@mail.com", "worker", "Worker", null),
            token = "jwt-worker"
        )
        val repository = AuthRepository(api)

        //Act
        val resultado = repository.verifyTwoFactor("challenge789", "123456")

        //Assert
        assertEquals(Verify2faRequest("challenge789", "123456"), api.lastVerifyRequest)
        assertTrue(resultado is LoginResult.Authenticated)
    }

    private class FakeAuthApi : AuthApi {
        var authResponse: AuthResponse = AuthResponse()
        var twoFactorSetup = TwoFactorSetupResponse("", "")
        var lastLoginRequest: LoginRequest? = null
        var lastClientRequest: RegisterClientRequest? = null
        var lastWorkerRequest: RegisterWorkerRequest? = null
        var lastAuthorization: String? = null
        var lastAcceptTermsRequest: AcceptTermsRequest? = null
        var lastEnableRequest: Enable2faRequest? = null
        var lastVerifyRequest: Verify2faRequest? = null

        override suspend fun login(request: LoginRequest): AuthResponse {
            lastLoginRequest = request
            return authResponse
        }

        override suspend fun registerClient(request: RegisterClientRequest): AuthResponse {
            lastClientRequest = request
            return authResponse
        }

        override suspend fun registerWorker(request: RegisterWorkerRequest): AuthResponse {
            lastWorkerRequest = request
            return authResponse
        }

        override suspend fun acceptTerms(
            authorization: String,
            request: AcceptTermsRequest
        ): AuthResponse {
            lastAuthorization = authorization
            lastAcceptTermsRequest = request
            return authResponse
        }

        override suspend fun setupTwoFactor(authorization: String): TwoFactorSetupResponse {
            lastAuthorization = authorization
            return twoFactorSetup
        }

        override suspend fun enableTwoFactor(
            authorization: String,
            request: Enable2faRequest
        ): AuthResponse {
            lastAuthorization = authorization
            lastEnableRequest = request
            return authResponse
        }

        override suspend fun verifyTwoFactor(request: Verify2faRequest): AuthResponse {
            lastVerifyRequest = request
            return authResponse
        }
    }
}
