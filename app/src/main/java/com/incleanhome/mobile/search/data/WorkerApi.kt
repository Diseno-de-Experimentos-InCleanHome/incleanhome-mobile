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

    /** [district] of the client adds approxDistanceKm to the profile (US32, Escenario 3). */
    @GET("workers/{id}")
    suspend fun getWorker(@Path("id") workerId: Int, @Query("district") district: String? = null): Worker

    @GET("workers/{id}/availability")
    suspend fun getAvailability(
        @Path("id") workerId: Int
    ): List<AvailabilitySlot>

    /** Up to five nearby and available workers (US33, EC-BLQ01); 403 when not active for the client. */
    @GET("workers/suggestions")
    suspend fun getSuggestions(
        @Query("serviceType") serviceType: String,
        @Query("date") date: String,
        @Query("startTime") startTime: String,
        @Query("endTime") endTime: String,
        @Query("district") district: String
    ): List<Worker>
}
