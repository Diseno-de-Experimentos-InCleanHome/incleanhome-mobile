package com.incleanhome.mobile

import com.incleanhome.mobile.search.data.AvailabilitySlot
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerApi
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkerRepositoryTest {

    /* USER STORY 06 - Buscar prestadores con filtros */
    @Test
    fun testSearchWorkers_LimpiaFiltrosAntesDeConsultar() = runBlocking {
        //Arrange
        val api = FakeWorkerApi()
        api.workers = listOf(worker())
        val repository = WorkerRepository(api)

        //Act
        val resultado = repository.searchWorkers("  limpieza_general  ", "  San Isidro  ")

        //Assert
        assertEquals("limpieza_general", api.lastServiceType)
        assertEquals("San Isidro", api.lastZone)
        assertTrue(resultado is WorkerResult.Success)
    }

    /* USER STORY 06 - Buscar sin filtros */
    @Test
    fun testSearchWorkers_FiltrosVaciosSeEnvíanComoNull() = runBlocking {
        //Arrange
        val api = FakeWorkerApi()
        val repository = WorkerRepository(api)

        //Act
        repository.searchWorkers("   ", "")

        //Assert
        assertNull(api.lastServiceType)
        assertNull(api.lastZone)
    }

    /* USER STORY 07 - Consultar perfil de prestador */
    @Test
    fun testGetWorker_DevuelvePerfilDelPrestador() = runBlocking {
        //Arrange
        val api = FakeWorkerApi()
        api.worker = worker()
        val repository = WorkerRepository(api)

        //Act
        val resultado = repository.getWorker(20)

        //Assert
        assertEquals(20, api.lastWorkerId)
        assertTrue(resultado is WorkerResult.Success)
        resultado as WorkerResult.Success
        assertEquals("Rosa", resultado.data.name)
    }

    /* USER STORY 07 - Consultar disponibilidad del prestador */
    @Test
    fun testGetAvailability_DevuelveHorariosConfigurados() = runBlocking {
        //Arrange
        val api = FakeWorkerApi()
        api.availability = listOf(AvailabilitySlot(1, 1, "09:00", "13:00", true))
        val repository = WorkerRepository(api)

        //Act
        val resultado = repository.getAvailability(20)

        //Assert
        assertEquals(20, api.lastWorkerId)
        assertTrue(resultado is WorkerResult.Success)
        resultado as WorkerResult.Success
        assertEquals("09:00", resultado.data.first().startTime)
    }

    private fun worker() = Worker(
        id = 20,
        name = "Rosa",
        phone = "999000111",
        age = 35,
        gender = "female",
        serviceTypes = listOf("limpieza_general"),
        zones = listOf("San Isidro"),
        hourlyRate = BigDecimal("30.00"),
        experienceYears = 4,
        bio = "Experiencia en limpieza",
        averageRating = BigDecimal("4.7"),
        totalServices = 40
    )

    private class FakeWorkerApi : WorkerApi {
        var workers: List<Worker> = emptyList()
        var worker: Worker = Worker(
            1, "Worker", null, 18, "other", emptyList(), emptyList(),
            BigDecimal.ZERO, 0, "", BigDecimal.ZERO, 0
        )
        var availability: List<AvailabilitySlot> = emptyList()
        var lastServiceType: String? = null
        var lastZone: String? = null
        var lastWorkerId: Int? = null

        override suspend fun searchWorkers(serviceType: String?, zone: String?): List<Worker> {
            lastServiceType = serviceType
            lastZone = zone
            return workers
        }

        override suspend fun getWorker(workerId: Int): Worker {
            lastWorkerId = workerId
            return worker
        }

        override suspend fun getAvailability(workerId: Int): List<AvailabilitySlot> {
            lastWorkerId = workerId
            return availability
        }
    }
}
