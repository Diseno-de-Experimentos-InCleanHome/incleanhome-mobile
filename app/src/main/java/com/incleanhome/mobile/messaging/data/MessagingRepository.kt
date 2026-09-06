package com.incleanhome.mobile.messaging.data

import com.google.gson.JsonParser
import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.userMessage
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface MessagingResult<out T> {
    data class Success<T>(val data: T) : MessagingResult<T>
    data class Error(val message: String) : MessagingResult<Nothing>
}

class MessagingRepository(
    private val api: MessagingApi = RetrofitClient.retrofit.create(MessagingApi::class.java)
) {
    suspend fun getConversations(): MessagingResult<List<Conversation>> =
        execute { api.getConversations() }

    suspend fun getThread(userId: Int): MessagingResult<List<Message>> =
        execute { api.getThread(userId) }

    suspend fun sendMessage(userId: Int, content: String): MessagingResult<Message> =
        execute { api.sendMessage(userId, SendMessageRequest(content)) }

    private suspend fun <T> execute(block: suspend () -> T): MessagingResult<T> {
        return try {
            MessagingResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            MessagingResult.Error(userMessage(exception, "la operación de mensajería") ?: when (exception.code()) {
                401 -> "La sesión no es válida. Vuelve a iniciar sesión."
                403 -> "No tienes permiso para consultar esta conversación."
                404 -> "No se encontró la conversación."
                else -> "No se pudo completar la operación de mensajes."
            })
        } catch (exception: IOException) {
            MessagingResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            MessagingResult.Error("Ocurrió un error inesperado.")
        }
    }

    private fun serverMessage(exception: HttpException): String? = runCatching {
        val body = exception.response()?.errorBody()?.string().orEmpty()
        JsonParser.parseString(body).asJsonObject.get("error")?.asString
    }.getOrNull()?.takeIf(String::isNotBlank)
}
