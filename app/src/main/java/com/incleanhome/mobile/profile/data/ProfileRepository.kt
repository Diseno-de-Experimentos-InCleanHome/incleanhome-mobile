package com.incleanhome.mobile.profile.data

import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.network.userMessage
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

sealed interface ProfileResult<out T> { data class Success<T>(val data:T): ProfileResult<T>; data class Error(val message:String): ProfileResult<Nothing> }
class ProfileRepository(private val api: ProfileApi = RetrofitClient.retrofit.create(ProfileApi::class.java)) {
    suspend fun client() = execute { api.getClientProfile() }
    suspend fun updateClient(r: UpdateClientProfileRequest) = execute { api.updateClientProfile(r) }
    suspend fun worker() = execute { api.getWorkerProfile() }
    suspend fun updateWorker(r: UpdateWorkerProfileRequest) = execute { api.updateWorkerProfile(r) }
    suspend fun stats() = execute { api.getWorkerStats() }
    private suspend fun <T> execute(block:suspend()->T): ProfileResult<T> = try { ProfileResult.Success(block()) }
    catch (e: CancellationException) { throw e }
    catch (e: HttpException) { ProfileResult.Error(userMessage(e, "la operación de perfil")) }
    catch (e: IOException) { ProfileResult.Error("No se pudo conectar con el servidor.") }
    catch (_: Exception) { ProfileResult.Error("Ocurrió un error inesperado.") }
}
