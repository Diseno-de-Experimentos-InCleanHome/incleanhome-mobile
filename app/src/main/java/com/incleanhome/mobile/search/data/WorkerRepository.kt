package com.incleanhome.mobile.search.data

import com.incleanhome.mobile.core.network.RetrofitClient
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface WorkerResult<out T> {
    data class Success<T>(val data: T) : WorkerResult<T>
    data class Error(val message: String) : WorkerResult<Nothing>
}

class WorkerRepository(
    private val api: WorkerApi = RetrofitClient.retrofit.create(WorkerApi::class.java)
) {
    suspend fun searchWorkers(serviceType: String?, zone: String?): WorkerResult<List<Worker>> {
        return request {
            api.searchWorkers(
                serviceType = serviceType?.trim()?.takeIf(String::isNotEmpty),
                zone = zone?.trim()?.takeIf(String::isNotEmpty)
            )
        }
    }

    suspend fun getWorker(workerId: Int): WorkerResult<Worker> {
        return request { api.getWorker(workerId) }
    }

    suspend fun getAvailability(workerId: Int): WorkerResult<List<AvailabilitySlot>> {
        return request { api.getAvailability(workerId) }
    }

    private suspend fun <T> request(block: suspend () -> T): WorkerResult<T> {
        return try {
            WorkerResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            val message = when (exception.code()) {
                401 -> "La sesión no es válida. Vuelve a iniciar sesión."
                403 -> "No tienes permiso para consultar esta información."
                404 -> "No se encontró el trabajador."
                else -> "No se pudo obtener la información de trabajadores."
            }
            WorkerResult.Error(message)
        } catch (exception: IOException) {
            WorkerResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            WorkerResult.Error("Ocurrió un error inesperado.")
        }
    }
}
