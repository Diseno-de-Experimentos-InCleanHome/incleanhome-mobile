package com.incleanhome.mobile.search.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WorkerApi {
    @GET("workers")
    suspend fun searchWorkers(
        @Query("serviceType") serviceType: String?,
        @Query("zone") zone: String?
    ): List<Worker>

    @GET("workers/{id}")
    suspend fun getWorker(@Path("id") workerId: Int): Worker

    @GET("workers/{id}/availability")
    suspend fun getAvailability(
        @Path("id") workerId: Int
    ): List<AvailabilitySlot>
}
