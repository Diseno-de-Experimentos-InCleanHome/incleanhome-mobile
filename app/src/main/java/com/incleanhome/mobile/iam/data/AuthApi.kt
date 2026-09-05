package com.incleanhome.mobile.iam.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/2fa/setup")
    suspend fun setupTwoFactor(
        @Header("Authorization") authorization: String
    ): TwoFactorSetupResponse

    @POST("auth/2fa/enable")
    suspend fun enableTwoFactor(
        @Header("Authorization") authorization: String,
        @Body request: Enable2faRequest
    ): AuthResponse

    @POST("auth/2fa/verify")
    suspend fun verifyTwoFactor(
        @Body request: Verify2faRequest
    ): AuthResponse
}
