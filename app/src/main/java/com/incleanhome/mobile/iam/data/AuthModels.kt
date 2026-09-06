package com.incleanhome.mobile.iam.data

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterClientRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String?,
    val acceptedTermsVersion: String
)

data class RegisterWorkerRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String?,
    val age: Int,
    val gender: String,
    val serviceTypes: List<String>,
    val zones: List<String>,
    val hourlyRate: java.math.BigDecimal,
    val experienceYears: Int,
    val bio: String?,
    val acceptedTermsVersion: String
)

data class AcceptTermsRequest(val version: String)

object AuthGender {
    const val FEMALE = "female"
    const val MALE = "male"
    const val OTHER = "other"
    val VALUES = listOf(FEMALE, MALE, OTHER)
}

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
