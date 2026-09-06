package com.incleanhome.mobile.events.data

import com.google.gson.JsonParser
import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.userMessage
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface EventResult<out T> {
    data class Success<T>(val data: T) : EventResult<T>
    data class Error(val message: String) : EventResult<Nothing>
}

class EventsRepository(private val api: EventsApi = RetrofitClient.retrofit.create(EventsApi::class.java)) {
    suspend fun create(r: CreateEventRequest) = request { api.create(r) }
    suspend fun searchOpen(service: String? = null, zone: String? = null, date: String? = null) =
        request { api.searchOpen(service, zone, date) }
    suspend fun getMyEvents() = request { api.getMyEvents() }
    suspend fun getMyApplications() = request { api.getMyApplications() }
    suspend fun getEvent(id: Int) = request { api.getEvent(id) }
    suspend fun cancel(id: Int) = request { api.cancel(id) }
    suspend fun complete(id: Int) = request { api.complete(id) }
    suspend fun apply(id: Int, message: String?) = request { api.apply(id, ApplyToEventRequest(message)) }
    suspend fun getApplications(id: Int) = request { api.getApplications(id) }
    suspend fun accept(eventId: Int, appId: Int) = request { api.accept(eventId, appId) }
    suspend fun reject(eventId: Int, appId: Int) = request { api.reject(eventId, appId) }
    suspend fun withdraw(eventId: Int, appId: Int) = request { api.withdraw(eventId, appId) }

    private suspend fun <T> request(block: suspend () -> T): EventResult<T> = try {
        EventResult.Success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: HttpException) {
        EventResult.Error(userMessage(e, "la operación del evento") ?: when (e.code()) {
            401 -> "La sesión no es válida."
            403 -> "No tienes permiso para realizar esta acción."
            404 -> "No se encontró el evento."
            else -> "No se pudo completar la operación del evento."
        })
    } catch (_: IOException) { EventResult.Error("No se pudo conectar con el servidor.")
    } catch (_: Exception) { EventResult.Error("Ocurrió un error inesperado.") }

    private fun serverMessage(e: HttpException): String? = runCatching {
        JsonParser.parseString(e.response()?.errorBody()?.string().orEmpty())
            .asJsonObject.get("error")?.asString
    }.getOrNull()?.takeIf(String::isNotBlank)
}
