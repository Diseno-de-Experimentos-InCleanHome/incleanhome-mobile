package com.incleanhome.mobile.worker.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface WorkerSelfApi {
    @GET("workers/me/profile")
    suspend fun getMyProfile(): WorkerProfile

    @GET("workers/{id}/availability")
    suspend fun getAvailability(
        @Path("id") workerId: Int
    ): List<WorkerAvailabilitySlot>

    @PUT("workers/{id}/availability")
    suspend fun replaceAvailability(
        @Path("id") workerId: Int,
        @Body request: ReplaceAvailabilityRequest
    ): List<WorkerAvailabilitySlot>
}
