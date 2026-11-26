package com.incleanhome.mobile

import com.incleanhome.mobile.profile.data.ClientProfile
import com.incleanhome.mobile.profile.data.MonthlyServiceCount
import com.incleanhome.mobile.profile.data.ProfileApi
import com.incleanhome.mobile.profile.data.ProfileRepository
import com.incleanhome.mobile.profile.data.ProfileResult
import com.incleanhome.mobile.profile.data.UpdateClientProfileRequest
import com.incleanhome.mobile.profile.data.UpdateWorkerProfileRequest
import com.incleanhome.mobile.profile.data.WorkerStats
import com.incleanhome.mobile.worker.data.WorkerProfile
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRepositoryTest {

    /* USER STORY 05 - Editar perfil de cliente */
    @Test
    fun testUpdateClient_EnviaNombreYTelefonoActualizados() = runBlocking {
        //Arrange
        val api = FakeProfileApi()
        val repository = ProfileRepository(api)
        val request = UpdateClientProfileRequest("María", "988777666")
        api.clientProfile = ClientProfile(1, 10, "María", "988777666")

        //Act
        val resultado = repository.updateClient(request)

        //Assert
        assertEquals(request, api.lastClientRequest)
        assertTrue(resultado is ProfileResult.Success)
        resultado as ProfileResult.Success
        assertEquals("María", resultado.data.name)
    }

    /* USER STORY 05 - Editar perfil de prestador */
    @Test
    fun testUpdateWorker_EnviaServiciosZonasYTarifa() = runBlocking {
        //Arrange
        val api = FakeProfileApi()
        val repository = ProfileRepository(api)
        val request = UpdateWorkerProfileRequest(
            name = "Pedro",
            phone = "977111222",
            age = 30,
            experienceYears = 5,
            hourlyRate = BigDecimal("40.00"),
            serviceTypes = listOf("jardineria"),
            zones = listOf("Miraflores"),
            bio = "Cinco años de experiencia"
        )
        api.workerProfile = workerProfile()

        //Act
        val resultado = repository.updateWorker(request)

        //Assert
        assertEquals(request, api.lastWorkerRequest)
        assertEquals(listOf("jardineria"), api.lastWorkerRequest?.serviceTypes)
        assertEquals(BigDecimal("40.00"), api.lastWorkerRequest?.hourlyRate)
        assertTrue(resultado is ProfileResult.Success)
    }

    private fun workerProfile() = WorkerProfile(
        id = 2,
        name = "Pedro",
        phone = "977111222",
        age = 30,
        gender = "male",
        serviceTypes = listOf("jardineria"),
        zones = listOf("Miraflores"),
        hourlyRate = BigDecimal("40.00"),
        experienceYears = 5,
        bio = "Cinco años de experiencia",
        averageRating = BigDecimal("4.8"),
        totalServices = 25
    )

    private class FakeProfileApi : ProfileApi {
        var clientProfile = ClientProfile(1, 10, "Cliente", null)
        var workerProfile = WorkerProfile(
            2, "Worker", null, 25, "male", emptyList(), emptyList(),
            BigDecimal("30.00"), 2, "", BigDecimal("0.0"), 0
        )
        var lastClientRequest: UpdateClientProfileRequest? = null
        var lastWorkerRequest: UpdateWorkerProfileRequest? = null

        override suspend fun getClientProfile(): ClientProfile = clientProfile

        override suspend fun updateClientProfile(request: UpdateClientProfileRequest): ClientProfile {
            lastClientRequest = request
            return clientProfile
        }

        override suspend fun getWorkerProfile(): WorkerProfile = workerProfile

        override suspend fun updateWorkerProfile(request: UpdateWorkerProfileRequest): WorkerProfile {
            lastWorkerRequest = request
            return workerProfile
        }

        override suspend fun getWorkerStats(): WorkerStats = WorkerStats(
            completedServices = 0,
            averageRating = BigDecimal.ZERO,
            monthlyServiceCounts = listOf(MonthlyServiceCount("2026-09", 0))
        )
    }
}
