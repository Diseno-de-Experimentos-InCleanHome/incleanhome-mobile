package com.incleanhome.mobile.search.domain

import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.serverErrorMessage
import com.incleanhome.mobile.core.network.userMessage
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerApi
import com.incleanhome.mobile.worker.data.ServiceAreaRules
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

data class SuggestionRequest(
    val serviceType: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    /** Canonical district key of the service (US32 catalog). */
    val district: String
)

sealed interface SuggestionResult {
    /** At most five workers, closest first; empty means no suggestions (Escenario 2). */
    data class Success(val workers: List<Worker>) : SuggestionResult

    /** 403: EC-BLQ01 is off or the client is in the control group (Escenario 3). */
    data object NotActive : SuggestionResult

    /** 400 or another failure; [validDistricts] comes with an unknown district. */
    data class Error(val message: String, val validDistricts: List<String> = emptyList()) : SuggestionResult
}

/**
 * Suggestion of US33 from GET workers/suggestions. The API decides who is suggested (service,
 * service area, availability and other bookings) and the order (distance, then rating); the app
 * neither filters nor computes distances, so the result is the same as in the web.
 */
class WorkerSuggestionService(
    private val api: () -> WorkerApi = { RetrofitClient.retrofit.create(WorkerApi::class.java) }
) {
    suspend fun suggest(request: SuggestionRequest): SuggestionResult = try {
        SuggestionResult.Success(
            api().getSuggestions(
                serviceType = request.serviceType,
                date = request.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                startTime = request.start.format(TIME_FORMAT),
                endTime = request.end.format(TIME_FORMAT),
                district = request.district
            ).take(MAX_SUGGESTIONS)
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: HttpException) {
        when (exception.code()) {
            403 -> SuggestionResult.NotActive
            400 -> {
                val body = runCatching { exception.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
                SuggestionResult.Error(
                    serverErrorMessage(body) ?: "Los datos enviados no son válidos.",
                    ServiceAreaRules.validDistricts(body)
                )
            }
            else -> SuggestionResult.Error(userMessage(exception, "la búsqueda de sugerencias"))
        }
    } catch (_: IOException) {
        SuggestionResult.Error("No se pudo conectar con el servidor.")
    } catch (_: Exception) {
        SuggestionResult.Error("Ocurrió un error inesperado.")
    }

    companion object {
        const val MAX_SUGGESTIONS = 5
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
