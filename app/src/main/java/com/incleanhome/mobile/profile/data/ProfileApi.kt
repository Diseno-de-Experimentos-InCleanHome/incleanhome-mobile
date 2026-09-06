package com.incleanhome.mobile.profile.data

import com.incleanhome.mobile.worker.data.WorkerProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

data class ClientProfile(val id: Int, val userId: Int, val name: String, val phone: String?)
data class UpdateClientProfileRequest(val name: String, val phone: String?)
data class UpdateWorkerProfileRequest(
    val name: String, val phone: String?, val age: Int, val experienceYears: Int,
    val hourlyRate: java.math.BigDecimal, val serviceTypes: List<String>, val zones: List<String>, val bio: String?
)
data class WorkerStats(val completedServices: Int, val averageRating: java.math.BigDecimal, val monthlyServiceCounts: List<MonthlyServiceCount>)
data class MonthlyServiceCount(val month: String, val count: Int)

interface ProfileApi {
    @GET("my-profile") suspend fun getClientProfile(): ClientProfile
    @PATCH("my-profile") suspend fun updateClientProfile(@Body request: UpdateClientProfileRequest): ClientProfile
    @GET("workers/me/profile") suspend fun getWorkerProfile(): WorkerProfile
    @PUT("workers/me/profile") suspend fun updateWorkerProfile(@Body request: UpdateWorkerProfileRequest): WorkerProfile
    @GET("workers/me/stats") suspend fun getWorkerStats(): WorkerStats
}
