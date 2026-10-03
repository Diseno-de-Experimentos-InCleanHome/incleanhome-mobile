package com.incleanhome.mobile

import com.incleanhome.mobile.worker.data.AvailabilitySlotInput
import com.incleanhome.mobile.worker.data.ReplaceAvailabilityRequest
import com.incleanhome.mobile.worker.data.WorkerAvailabilitySlot
import com.incleanhome.mobile.worker.data.WorkerProfile
import com.incleanhome.mobile.worker.data.WorkerSelfApi
import com.incleanhome.mobile.worker.data.WorkerSelfRepository
import com.incleanhome.mobile.worker.data.WorkerSelfResult
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkerAvailabilityRepositoryTest {

    /* USER STORY 09 - Registrar disponibilidad */
    @Test
    fun testReplaceAvailability_EnviaDiasYHorariosConfigurados() = runBlocking {
        //Arrange
        val api = FakeWorkerSelfApi()
        val repository = WorkerSelfRepository(api)
        val slots = listOf(
            AvailabilitySlotInput(1, "08:00", "12:00", true),
            AvailabilitySlotInput(2, "09:00", "13:00", true)
        )
        api.availability = listOf(
            WorkerAvailabilitySlot(1, 1, "08:00", "12:00", true),
            WorkerAvailabilitySlot(2, 2, "09:00", "13:00", true)
        )

        //Act
        val resultado = repository.replaceAvailability(7, slots)

        //Assert
        assertEquals(7, api.lastWorkerId)
        assertEquals(ReplaceAvailabilityRequest(slots), api.lastRequest)
        assertTrue(resultado is WorkerSelfResult.Success)
    }

    /* USER STORY 09 - Día no disponible */
    @Test
    fun testReplaceAvailability_PermiteMarcarDiaNoDisponible() = runBlocking {
        //Arrange
        val api = FakeWorkerSelfApi()
        val repository = WorkerSelfRepository(api)
        val slot = AvailabilitySlotInput(0, "08:00", "18:00", false)
        api.availability = listOf(WorkerAvailabilitySlot(1, 0, "08:00", "18:00", false))

        //Act
        val resultado = repository.replaceAvailability(7, listOf(slot))

        //Assert
        assertFalse(api.lastRequest!!.slots.first().isAvailable)
        assertTrue(resultado is WorkerSelfResult.Success)
    }

    private class FakeWorkerSelfApi : WorkerSelfApi {
        var availability: List<WorkerAvailabilitySlot> = emptyList()
        var lastWorkerId: Int? = null
        var lastRequest: ReplaceAvailabilityRequest? = null

        override suspend fun getMyProfile(): WorkerProfile = WorkerProfile(
            7, "Worker", null, 30, "other", emptyList(), emptyList(),
            BigDecimal("20.00"), 1, "", BigDecimal.ZERO, 0
        )

        override suspend fun getAvailability(workerId: Int): List<WorkerAvailabilitySlot> {
            lastWorkerId = workerId
            return availability
        }

        override suspend fun replaceAvailability(
            workerId: Int,
            request: ReplaceAvailabilityRequest
        ): List<WorkerAvailabilitySlot> {
            lastWorkerId = workerId
            lastRequest = request
            return availability
        }
    }
}
