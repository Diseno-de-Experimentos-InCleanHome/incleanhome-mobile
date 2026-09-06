package com.incleanhome.mobile.profile.data

import com.google.gson.JsonParser
import com.incleanhome.mobile.core.network.RetrofitClient
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
    catch (e: HttpException) { ProfileResult.Error(message(e) ?: when(e.code()){401->"La sesión no es válida.";403->"No tienes permiso.";else->"No se pudo completar la operación."}) }
    catch (e: IOException) { ProfileResult.Error("No se pudo conectar con el servidor.") }
    catch (_: Exception) { ProfileResult.Error("Ocurrió un error inesperado.") }
    private fun message(e:HttpException)=runCatching{JsonParser.parseString(e.response()?.errorBody()?.string().orEmpty()).asJsonObject.get("error")?.asString}.getOrNull()?.takeIf{it.isNotBlank()}
}
