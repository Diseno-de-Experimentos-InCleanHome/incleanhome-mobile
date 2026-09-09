package com.incleanhome.mobile

import com.google.gson.Gson
import com.incleanhome.mobile.claims.data.ClaimCreatedResponse
import com.incleanhome.mobile.claims.data.ClaimResult
import com.incleanhome.mobile.claims.data.ClaimStatus
import com.incleanhome.mobile.claims.data.ClaimTrackResponse
import com.incleanhome.mobile.claims.data.ClaimType
import com.incleanhome.mobile.claims.data.ClaimsApi
import com.incleanhome.mobile.claims.data.ClaimsRepository
import com.incleanhome.mobile.claims.data.CreateClaimRequest
import com.incleanhome.mobile.claims.presentation.ClaimFormField
import com.incleanhome.mobile.claims.presentation.ClaimFormState
import com.incleanhome.mobile.claims.presentation.validateClaimForm
import com.incleanhome.mobile.ui.format.presentationValueResource
import com.incleanhome.mobile.ui.format.claimStatusResource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST

class ClaimsContractsTest {
    @Test
    fun validRegistrationContainsRequiredFields() = runBlocking {
        val state = ClaimFormState(
            consumerName = "Alex Doe",
            consumerEmail = "alex@example.com",
            description = "Service details"
        )
        assertTrue(validateClaimForm(state).isEmpty())

        val request = CreateClaimRequest(
            type = ClaimType.CLAIM,
            consumerName = state.consumerName,
            consumerDocument = "",
            consumerEmail = state.consumerEmail,
            consumerPhone = "",
            relatedService = null,
            description = state.description,
            consumerRequest = null
        )
        assertTrue(request.consumerName.isNotBlank())
        assertTrue(request.consumerEmail.isNotBlank())
        assertTrue(request.description.isNotBlank())
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"consumerName\""))
        assertTrue(json.contains("\"consumerEmail\""))
        assertTrue(json.contains("\"description\""))
        val api = FakeClaimsApi()
        val result = ClaimsRepository(api).create(request)
        assertEquals(request, api.createdRequest)
        assertTrue(result is ClaimResult.Success)
    }

    @Test
    fun emptyAndInvalidRequiredFieldsAreRejected() {
        assertEquals(
            setOf(ClaimFormField.NAME, ClaimFormField.EMAIL, ClaimFormField.DESCRIPTION),
            validateClaimForm(ClaimFormState())
        )
        assertTrue(
            ClaimFormField.EMAIL in validateClaimForm(
                ClaimFormState(consumerName = "A", consumerEmail = "invalid", description = "D")
            )
        )
    }

    @Test
    fun claimTypesKeepCanonicalBackendValues() {
        assertEquals(setOf("reclamo", "queja"), ClaimType.VALUES)
        assertEquals(R.string.claim_type_claim, presentationValueResource(ClaimType.CLAIM))
        assertEquals(R.string.claim_type_complaint, presentationValueResource(ClaimType.COMPLAINT))
    }

    @Test
    fun claimStatusesUseLocalizedPresentationMappings() {
        assertEquals(
            setOf("registered", "in_review", "resolved", "rejected"),
            ClaimStatus.VALUES
        )
        assertEquals(R.string.status_registered, claimStatusResource(ClaimStatus.REGISTERED))
        assertEquals(R.string.status_in_review, claimStatusResource(ClaimStatus.IN_REVIEW))
        assertEquals(R.string.status_resolved, claimStatusResource(ClaimStatus.RESOLVED))
        assertEquals(R.string.claim_status_rejected, claimStatusResource(ClaimStatus.REJECTED))
    }

    @Test
    fun trackingUsesTheExactProvidedCode() = runBlocking {
        val api = FakeClaimsApi()
        val result = ClaimsRepository(api).track("RC-2026-123456")

        assertEquals("RC-2026-123456", api.trackedCode)
        assertTrue(result is ClaimResult.Success)
        assertEquals("RC-2026-123456", (result as ClaimResult.Success).data.code)
    }

    @Test
    fun missingTrackingCodeMaps404WithoutCrash() = runBlocking {
        val api = FakeClaimsApi(trackFailure = httpException(404))
        val result = ClaimsRepository(api).track("RC-NOT-FOUND")

        assertTrue(result is ClaimResult.Error)
        assertEquals(
            com.incleanhome.mobile.claims.data.ClaimFailure.NOT_FOUND,
            (result as ClaimResult.Error).failure
        )
    }

    @Test
    fun publicClaimsEndpointsDoNotDependOnJwt() {
        assertEquals(2, ClaimsApi::class.java.declaredMethods.size)
        assertEquals(
            "claims",
            ClaimsApi::class.java.getDeclaredMethod("create", CreateClaimRequest::class.java, kotlin.coroutines.Continuation::class.java)
                .getAnnotation(POST::class.java)?.value
        )
        assertEquals(
            "claims/track/{code}",
            ClaimsApi::class.java.getDeclaredMethod("track", String::class.java, kotlin.coroutines.Continuation::class.java)
                .getAnnotation(GET::class.java)?.value
        )
        assertTrue(
            ClaimsApi::class.java.declaredMethods
                .flatMap { method ->
                    method.parameterAnnotations.flatMap { annotations -> annotations.asList() }
                }
                .none { it is Header }
        )
    }

    private class FakeClaimsApi(
        private val trackFailure: HttpException? = null
    ) : ClaimsApi {
        var trackedCode: String? = null
        var createdRequest: CreateClaimRequest? = null

        override suspend fun create(request: CreateClaimRequest): ClaimCreatedResponse {
            createdRequest = request
            return ClaimCreatedResponse("RC-2026-123456", ClaimStatus.REGISTERED)
        }

        override suspend fun track(code: String): ClaimTrackResponse {
            trackFailure?.let { throw it }
            trackedCode = code
            return ClaimTrackResponse(
                code = code,
                type = ClaimType.CLAIM,
                status = ClaimStatus.REGISTERED,
                createdAt = "2026-09-09T12:00:00Z",
                resolvedAt = null,
                adminNote = null
            )
        }
    }

    private fun httpException(status: Int): HttpException = HttpException(
        Response.error<ClaimTrackResponse>(
            status,
            "{}".toResponseBody("application/json".toMediaType())
        )
    )
}
