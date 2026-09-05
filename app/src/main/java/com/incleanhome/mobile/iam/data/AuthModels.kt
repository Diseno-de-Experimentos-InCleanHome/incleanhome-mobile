package com.incleanhome.mobile.iam.data

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val requiresTermsAcceptance: Boolean? = null,
    val requires2fa: Boolean? = null,
    val requires2faSetup: Boolean? = null,
    val challengeToken: String? = null,
    val user: AuthUser? = null,
    val token: String? = null
)

data class AuthUser(
    val id: Int,
    val email: String,
    val role: String,
    val name: String,
    val phone: String?
)

data class TwoFactorSetupResponse(
    val qrCodeDataUrl: String,
    val secret: String
)

data class Enable2faRequest(
    val code: String
)

data class Verify2faRequest(
    val challengeToken: String,
    val code: String
)
