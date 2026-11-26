package com.incleanhome.mobile.experiments

import com.incleanhome.mobile.experiments.tracking.TrackingEvent
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Endpoints the mobile app needs from the API for US42 and US43. They do not exist yet in
 * incleanhome-backend (main, 6af2c7f); the calls stay disabled through BuildConfig until the
 * backend publishes them. Contract: docs/backend-endpoints.md.
 */
interface ExperimentationApi {
    @GET("experiments/config")
    suspend fun getConfig(): RemoteExperimentConfig

    @POST("tracking/events")
    suspend fun sendEvents(@Body batch: TrackingBatch): Response<Unit>
}

data class TrackingBatch(val events: List<TrackingEvent>)
