package com.incleanhome.mobile.claims.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ClaimsApi {
    @POST("claims")
    suspend fun create(@Body request: CreateClaimRequest): ClaimCreatedResponse

    @GET("claims/track/{code}")
    suspend fun track(@Path("code") code: String): ClaimTrackResponse
}
