package com.incleanhome.mobile.worker.data

import com.google.gson.JsonParser
import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.serverErrorMessage
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
 * The API answers the canonical district key (for example jesus_maria).
 */
data class ServiceArea(val district: String, val radiusKm: Int)

/** GET and PUT workers/me/service-area (incleanhome-backend v1.2.0, experiment EC-BLQ01-SA). */
interface ServiceAreaApi {
    @GET("workers/me/service-area")
    suspend fun get(): ServiceArea

    @PUT("workers/me/service-area")
    suspend fun update(@Body area: ServiceArea): ServiceArea
}

sealed interface ServiceAreaResult {
    /** [area] is null when the worker has not registered one yet (404 of the GET). */
    data class Success(val area: ServiceArea?) : ServiceAreaResult

    /** 400: radius out of range or unknown district; [validDistricts] comes with the second. */
    data class Rejected(val message: String, val validDistricts: List<String> = emptyList()) : ServiceAreaResult

    /** 403: EC-BLQ01-SA is not active for this account. Nothing was read or saved. */
    data object NotActive : ServiceAreaResult
    data class Error(val message: String) : ServiceAreaResult
}

object ServiceAreaRules {
    fun radiusError(radiusKm: Int?, minKm: Int, maxKm: Int): Boolean = radiusKm == null || radiusKm !in minKm..maxKm

    /** validDistricts of a 400 for an unknown district; empty for any other body. */
    internal fun validDistricts(body: String): List<String> = runCatching {
        JsonParser.parseString(body).asJsonObject.getAsJsonArray("validDistricts")
            ?.mapNotNull { it.takeIf { value -> value.isJsonPrimitive }?.asString }
            .orEmpty()
    }.getOrDefault(emptyList())
}

class ServiceAreaRepository(
    private val api: () -> ServiceAreaApi = { RetrofitClient.retrofit.create(ServiceAreaApi::class.java) }
) {
    suspend fun load(): ServiceAreaResult = call(notFoundIsEmpty = true) { ServiceAreaResult.Success(api().get()) }

    suspend fun save(area: ServiceArea): ServiceAreaResult =
        call(notFoundIsEmpty = false) { ServiceAreaResult.Success(api().update(area)) }

    private suspend fun call(notFoundIsEmpty: Boolean, block: suspend () -> ServiceAreaResult): ServiceAreaResult {
        return try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            when {
                // Only the GET reads 404 as "no area yet": a saved area is never assumed.
                exception.code() == 404 && notFoundIsEmpty -> ServiceAreaResult.Success(null)
                exception.code() == 403 -> ServiceAreaResult.NotActive
                exception.code() == 400 -> {
                    val body = runCatching { exception.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
                    ServiceAreaResult.Rejected(
                        serverErrorMessage(body) ?: "Los datos enviados no son válidos.",
                        ServiceAreaRules.validDistricts(body)
                    )
                }
                else -> ServiceAreaResult.Error(userMessage(exception, "la actualización del área de atención"))
            }
        } catch (_: IOException) {
            ServiceAreaResult.Error("No se pudo conectar con el servidor.")
        } catch (_: Exception) {
            ServiceAreaResult.Error("Ocurrió un error inesperado.")
        }
    }
}
