package com.incleanhome.mobile.worker.data

import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.userMessage
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * Service area of US32. Only the district and the radius are handled: the tracking plan forbids
 * coordinates and clients must only see the district and an approximate distance (Escenario 3).
 */
data class ServiceArea(val district: String, val radiusKm: Int)

/** Pending endpoints of incleanhome-backend (see docs/backend-endpoints.md). */
interface ServiceAreaApi {
    @GET("workers/me/service-area")
    suspend fun get(): ServiceArea

    @PUT("workers/me/service-area")
    suspend fun update(@Body area: ServiceArea): ServiceArea
}

sealed interface ServiceAreaResult {
    data class Success(val area: ServiceArea?) : ServiceAreaResult
    data class Error(val message: String) : ServiceAreaResult
    data object Unavailable : ServiceAreaResult
}

object ServiceAreaRules {
    fun radiusError(radiusKm: Int?, minKm: Int, maxKm: Int): Boolean = radiusKm == null || radiusKm !in minKm..maxKm
}

class ServiceAreaRepository(
    private val apiEnabled: Boolean,
    private val api: () -> ServiceAreaApi = { RetrofitClient.retrofit.create(ServiceAreaApi::class.java) }
) {
    suspend fun load(): ServiceAreaResult = call { ServiceAreaResult.Success(api().get()) }

    suspend fun save(area: ServiceArea): ServiceAreaResult = call { ServiceAreaResult.Success(api().update(area)) }

    private suspend fun call(block: suspend () -> ServiceAreaResult): ServiceAreaResult {
        if (!apiEnabled) return ServiceAreaResult.Unavailable
        return try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            if (exception.code() == 404) ServiceAreaResult.Success(null)
            else ServiceAreaResult.Error(userMessage(exception, "la actualización del área de atención"))
        } catch (_: IOException) {
            ServiceAreaResult.Error("No se pudo conectar con el servidor.")
        } catch (_: Exception) {
            ServiceAreaResult.Error("Ocurrió un error inesperado.")
        }
    }
}
