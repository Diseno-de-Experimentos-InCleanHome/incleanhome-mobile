package com.incleanhome.mobile

import com.incleanhome.mobile.iam.data.AuthResponse
import com.incleanhome.mobile.iam.data.AuthUser
import com.incleanhome.mobile.iam.data.LoginNextStep
import com.incleanhome.mobile.iam.data.LoginResult
import com.incleanhome.mobile.iam.data.interpretAuthResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {

    /* USER STORY 01 - Registro de usuario según rol */
    @Test
    fun testInterpretAuthResponse_RequiereConfigurar2FA() {
        //Arrange
        val response = AuthResponse(
            requires2faSetup = true,
            challengeToken = "token123"
        )

        //Act
        val resultado = interpretAuthResponse(response)

        //Assert
        assertTrue(resultado is LoginResult.Challenge)
        resultado as LoginResult.Challenge
        assertEquals(LoginNextStep.TWO_FA_SETUP, resultado.nextStep)
        assertEquals("token123", resultado.challengeToken)
    }

    /* USER STORY 02 - Gestionar inicio de sesión */
    @Test
    fun testInterpretAuthResponse_UsuarioAutenticado() {
        //Arrange
        val usuario = AuthUser(
            id = 10,
            email = "cliente@incleanhome.com",
            role = "client",
            name = "Cliente",
            phone = "999999999"
        )
        val response = AuthResponse(user = usuario, token = "jwt-token")

        //Act
        val resultado = interpretAuthResponse(response)

        //Assert
        assertTrue(resultado is LoginResult.Authenticated)
        resultado as LoginResult.Authenticated
        assertEquals(usuario, resultado.user)
        assertEquals("jwt-token", resultado.token)
    }

    /* USER STORY 04 - Autenticación de dos factores */
    @Test
    fun testInterpretAuthResponse_RequiereVerificar2FA() {
        //Arrange
        val response = AuthResponse(
            requires2fa = true,
            challengeToken = "challenge456"
        )

        //Act
        val resultado = interpretAuthResponse(response)

        //Assert
        assertTrue(resultado is LoginResult.Challenge)
        resultado as LoginResult.Challenge
        assertEquals(LoginNextStep.TWO_FA_VERIFY, resultado.nextStep)
        assertEquals("challenge456", resultado.challengeToken)
    }

    /* USER STORY 04 - Autenticación de dos factores */
    @Test
    fun testInterpretAuthResponse_ChallengeSinTokenDevuelveError() {
        //Arrange
        val response = AuthResponse(requires2fa = true)

        //Act
        val resultado = interpretAuthResponse(response)

        //Assert
        assertTrue(resultado is LoginResult.Error)
    }
}
