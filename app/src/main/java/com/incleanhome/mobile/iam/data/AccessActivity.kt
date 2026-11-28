package com.incleanhome.mobile.iam.data

import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.userMessage
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** One sign-in attempt of the own account (US40). Nothing else about the device is stored by the API. */
data class AccessEntry(
    val id: Int,
    /** ISO-8601 instant in UTC. */
    val occurredAt: String,
    /** web, mobile, api or unknown (attempts recorded before US40). */
    val platform: String,
    val twoFactorVerified: Boolean,
    /** success or failed. */
    val result: String
) {
    val succeeded: Boolean get() = result.equals("success", ignoreCase = true)
}

data class AccessActivity(
    val twoFactorEnabled: Boolean,
    val accesses: List<AccessEntry>,
    val passwordChangeRequired: Boolean
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/**
 * Access activity of incleanhome-backend v1.2.0. Every call uses the session token: the API only
 * answers the activity of the account of that session, so another account cannot be read.
 */
interface AccessActivityApi {
    @GET("auth/access-activity")
    suspend fun get(): AccessActivity

    @POST("auth/access-activity/{id}/report")
    suspend fun report(@Path("id") accessId: Int): Response<Unit>

    @PUT("auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
}

sealed interface AccessActivityResult<out T> {
    data class Success<T>(val data: T) : AccessActivityResult<T>
    data class Error(val message: String, val sessionClosed: Boolean = false) : AccessActivityResult<Nothing>
}

class AccessActivityRepository(
    private val api: () -> AccessActivityApi = { RetrofitClient.retrofit.create(AccessActivityApi::class.java) }
) {
    suspend fun load(): AccessActivityResult<AccessActivity> = call("la consulta de la actividad de acceso") {
        api().get().let { it.copy(accesses = it.accesses.take(MAX_ACCESSES)) }
    }

    /** Closes the other sessions and asks for a password change; this session stays open. */
    suspend fun report(accessId: Int): AccessActivityResult<Unit> = call("el reporte del acceso") {
        api().report(accessId).requireSuccess()
    }

    suspend fun changePassword(current: String, new: String): AccessActivityResult<Unit> =
        call("el cambio de contraseña") { api().changePassword(ChangePasswordRequest(current, new)).requireSuccess() }

    private fun Response<Unit>.requireSuccess() {
        if (!isSuccessful) throw HttpException(this)
    }

    private suspend fun <T> call(resource: String, block: suspend () -> T): AccessActivityResult<T> = try {
        AccessActivityResult.Success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: HttpException) {
        AccessActivityResult.Error(
            message = if (exception.code() == 404) NOT_FOUND_MESSAGE else userMessage(exception, resource),
            sessionClosed = exception.code() == 401
        )
    } catch (_: IOException) {
        AccessActivityResult.Error("No se pudo conectar con el servidor.")
    } catch (_: Exception) {
        AccessActivityResult.Error("Ocurrió un error inesperado.")
    }

    companion object {
        const val MAX_ACCESSES = 10
        const val NOT_FOUND_MESSAGE = "No se encontró ese acceso en tu cuenta."
    }
}
