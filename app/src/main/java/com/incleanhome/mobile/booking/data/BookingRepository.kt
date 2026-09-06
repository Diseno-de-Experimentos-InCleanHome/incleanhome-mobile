package com.incleanhome.mobile.booking.data

import com.google.gson.JsonParser
import com.incleanhome.mobile.core.network.RetrofitClient
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface BookingResult<out T> {
    data class Success<T>(val data: T) : BookingResult<T>
    data class Error(val message: String) : BookingResult<Nothing>
}

class BookingRepository(
    private val api: BookingApi = RetrofitClient.retrofit.create(BookingApi::class.java)
) {
    suspend fun create(request: CreateBookingRequest): BookingResult<Booking> =
        execute { api.createBooking(request) }

    suspend fun getMine(): BookingResult<List<Booking>> = execute { api.getMyBookings() }

    suspend fun updateStatus(bookingId: Int, status: String): BookingResult<Booking> =
        execute { api.updateStatus(bookingId, UpdateBookingStatusRequest(status)) }

    private suspend fun <T> execute(block: suspend () -> T): BookingResult<T> {
        return try {
            BookingResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            BookingResult.Error(serverMessage(exception) ?: when (exception.code()) {
                401 -> "La sesión no es válida. Vuelve a iniciar sesión."
                403 -> "No tienes permiso para realizar esta acción."
                404 -> "No se encontró la reserva."
                else -> "No se pudo completar la operación de reserva."
            })
        } catch (exception: IOException) {
            BookingResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            BookingResult.Error("Ocurrió un error inesperado.")
        }
    }

    private fun serverMessage(exception: HttpException): String? = runCatching {
        val body = exception.response()?.errorBody()?.string().orEmpty()
        JsonParser.parseString(body).asJsonObject.get("error")?.asString
    }.getOrNull()?.takeIf(String::isNotBlank)
}
