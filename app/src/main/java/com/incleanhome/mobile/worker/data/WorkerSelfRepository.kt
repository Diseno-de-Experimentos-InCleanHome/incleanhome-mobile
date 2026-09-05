package com.incleanhome.mobile.worker.data

import com.incleanhome.mobile.core.network.RetrofitClient
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface WorkerSelfResult<out T> {
    data class Success<T>(val data: T) : WorkerSelfResult<T>
    data class Error(val message: String) : WorkerSelfResult<Nothing>
}

class WorkerSelfRepository(
    private val api: WorkerSelfApi = RetrofitClient.retrofit.create(WorkerSelfApi::class.java)
) {
    suspend fun getMyProfile(): WorkerSelfResult<WorkerProfile> {
        return request { api.getMyProfile() }
    }

    suspend fun getAvailability(workerId: Int): WorkerSelfResult<List<WorkerAvailabilitySlot>> {
        return request { api.getAvailability(workerId) }
    }

    suspend fun replaceAvailability(
        workerId: Int,
        slots: List<AvailabilitySlotInput>
    ): WorkerSelfResult<List<WorkerAvailabilitySlot>> {
        return request {
            api.replaceAvailability(
                workerId = workerId,
                request = ReplaceAvailabilityRequest(slots = slots)
            )
        }
    }

    private suspend fun <T> request(block: suspend () -> T): WorkerSelfResult<T> {
        return try {
            WorkerSelfResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            val message = when (exception.code()) {
                400 -> "Los datos enviados no son válidos."
                401 -> "La sesión no es válida. Vuelve a iniciar sesión."
                403 -> "No tienes permiso para realizar esta operación."
                404 -> "No se encontró el perfil del trabajador."
                else -> "No se pudo obtener la información del trabajador."
            }
            WorkerSelfResult.Error(message)
        } catch (exception: IOException) {
            WorkerSelfResult.Error("No se pudo conectar con el servidor.")
        } catch (exception: Exception) {
            WorkerSelfResult.Error("Ocurrió un error inesperado.")
        }
    }
}
