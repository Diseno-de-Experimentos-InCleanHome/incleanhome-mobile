package com.incleanhome.mobile.reviews.data

import com.google.gson.JsonParser
import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.userMessage
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface ReviewResult<out T> {
    data class Success<T>(val data: T) : ReviewResult<T>
    data class Error(val message: String) : ReviewResult<Nothing>
}

class ReviewsRepository(
    private val api: ReviewsApi = RetrofitClient.retrofit.create(ReviewsApi::class.java)
) {
    suspend fun createReview(request: CreateReviewRequest): ReviewResult<Review> =
        execute { api.createReview(request) }

    suspend fun getWorkerReviews(workerId: Int): ReviewResult<List<Review>> =
        execute { api.getWorkerReviews(workerId) }

    private suspend fun <T> execute(block: suspend () -> T): ReviewResult<T> {
        return try {
            ReviewResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            ReviewResult.Error(userMessage(exception, "la operación de reseña") ?: when (exception.code()) {
                401 -> "La sesión no es válida. Vuelve a iniciar sesión."
                403 -> "No tienes permiso para publicar esta reseña."
                404 -> "No se encontró la información solicitada."
                else -> "No se pudo completar la operación de reseñas."
            })
        } catch (exception: IOException) {
            ReviewResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            ReviewResult.Error("Ocurrió un error inesperado.")
        }
    }

    private fun serverMessage(exception: HttpException): String? = runCatching {
        val body = exception.response()?.errorBody()?.string().orEmpty()
        JsonParser.parseString(body).asJsonObject.get("error")?.asString
    }.getOrNull()?.takeIf(String::isNotBlank)
}
