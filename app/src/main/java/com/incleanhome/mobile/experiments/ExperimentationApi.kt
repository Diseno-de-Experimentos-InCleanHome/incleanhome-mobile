package com.incleanhome.mobile.experiments

import com.incleanhome.mobile.experiments.tracking.TrackingEvent
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Endpoints of US42 and US43, published by incleanhome-backend since v1.1.0 (docs/to-be-evidencia.md
 * of that repository; contract summary in docs/backend-endpoints.md). Each environment enables them
 * through BuildConfig: on in staging, off in production until authorised.
 * - GET experiments/config: with or without session.
 * - POST tracking/events: with session; 202 even for repeated eventIds, 503 while the salt is missing.
 */
interface ExperimentationApi {
    @GET("experiments/config")
    suspend fun getConfig(): RemoteExperimentConfig

    @POST("tracking/events")
    suspend fun sendEvents(@Body batch: TrackingBatch): Response<Unit>
}

data class TrackingBatch(val events: List<TrackingEvent>)
