package com.incleanhome.mobile

import com.incleanhome.mobile.events.data.ApplicationStatus
import com.incleanhome.mobile.events.data.ApplyToEventRequest
import com.incleanhome.mobile.events.data.CreateEventRequest
import com.incleanhome.mobile.events.data.Event
import com.incleanhome.mobile.events.data.EventApplication
import com.incleanhome.mobile.events.data.EventResult
import com.incleanhome.mobile.events.data.EventStatus
import com.incleanhome.mobile.events.data.EventsApi
import com.incleanhome.mobile.events.data.EventsRepository
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsRepositoryTest {

    /* USER STORY 15 - Crear evento */
    @Test
    fun testCreateEvent_EnviaServiciosCuposYTarifa() = runBlocking {
        //Arrange
        val api = FakeEventsApi()
        val repository = EventsRepository(api)
        val request = CreateEventRequest(
            title = "Limpieza de oficina",
            description = "Limpieza general",
            serviceTypes = listOf("limpieza_general"),
            zone = "San Isidro",
            address = "Av. Empresa 100",
            date = "2026-10-20",
            startTime = "09:00",
            endTime = "13:00",
            hours = BigDecimal("4.00"),
            workersNeeded = 2,
            hourlyRateOffered = BigDecimal("35.00"),
            applicationDeadline = "2026-10-19T18:00:00Z"
        )
        api.event = event(status = EventStatus.OPEN)

        //Act
        val resultado = repository.create(request)

        //Assert
        assertEquals(request, api.lastCreateRequest)
        assertEquals(2, api.lastCreateRequest?.workersNeeded)
        assertTrue(resultado is EventResult.Success)
    }

    /* USER STORY 16 - Cancelar evento */
    @Test
    fun testCancelEvent_UsaIdDelEventoSeleccionado() = runBlocking {
        //Arrange
        val api = FakeEventsApi()
        val repository = EventsRepository(api)
        api.event = event(status = EventStatus.CANCELLED)

        //Act
        val resultado = repository.cancel(30)

        //Assert
        assertEquals(30, api.lastEventId)
        assertTrue(resultado is EventResult.Success)
    }

    /* USER STORY 16 - Completar evento */
    @Test
    fun testCompleteEvent_UsaIdDelEventoSeleccionado() = runBlocking {
        //Arrange
        val api = FakeEventsApi()
        val repository = EventsRepository(api)
        api.event = event(status = EventStatus.COMPLETED)

        //Act
        val resultado = repository.complete(30)

        //Assert
        assertEquals(30, api.lastEventId)
        assertTrue(resultado is EventResult.Success)
    }

    /* USER STORY 17 - Aceptar postulación */
    @Test
    fun testAcceptApplication_EnviaEventoYPostulacionCorrectos() = runBlocking {
        //Arrange
        val api = FakeEventsApi()
        val repository = EventsRepository(api)
        api.application = application(status = ApplicationStatus.ACCEPTED)

        //Act
        val resultado = repository.accept(30, 8)

        //Assert
        assertEquals(30, api.lastEventId)
        assertEquals(8, api.lastApplicationId)
        assertTrue(resultado is EventResult.Success)
    }

    /* USER STORY 17 - Rechazar postulación */
    @Test
    fun testRejectApplication_EnviaEventoYPostulacionCorrectos() = runBlocking {
        //Arrange
        val api = FakeEventsApi()
        val repository = EventsRepository(api)
        api.application = application(status = ApplicationStatus.REJECTED)

        //Act
        repository.reject(30, 8)

        //Assert
        assertEquals(30, api.lastEventId)
        assertEquals(8, api.lastApplicationId)
    }

    /* USER STORY 19 - Postular a evento */
    @Test
    fun testApplyToEvent_EnviaMensajeOpcional() = runBlocking {
        //Arrange
        val api = FakeEventsApi()
        val repository = EventsRepository(api)
        api.application = application(status = ApplicationStatus.PENDING)

        //Act
        val resultado = repository.apply(30, "Tengo disponibilidad")

        //Assert
        assertEquals(30, api.lastEventId)
        assertEquals(ApplyToEventRequest("Tengo disponibilidad"), api.lastApplyRequest)
        assertTrue(resultado is EventResult.Success)
    }

    /* USER STORY 19 - Retirar postulación */
    @Test
    fun testWithdrawApplication_EnviaIdsCorrectos() = runBlocking {
        //Arrange
        val api = FakeEventsApi()
        val repository = EventsRepository(api)
        api.application = application(status = ApplicationStatus.WITHDRAWN)

        //Act
        val resultado = repository.withdraw(30, 8)

        //Assert
        assertEquals(30, api.lastEventId)
        assertEquals(8, api.lastApplicationId)
        assertTrue(resultado is EventResult.Success)
    }

    private fun event(status: String) = Event(
        id = 30,
        clientId = 4,
        clientName = "Cliente",
        title = "Limpieza de oficina",
        description = "Limpieza general",
        serviceTypes = listOf("limpieza_general"),
        zone = "San Isidro",
        address = "Av. Empresa 100",
        date = "2026-10-20",
        startTime = "09:00",
        endTime = "13:00",
        hours = BigDecimal("4.00"),
        workersNeeded = 2,
        acceptedCount = 0,
        hourlyRateOffered = BigDecimal("35.00"),
        applicationDeadline = "2026-10-19T18:00:00Z",
        status = status,
        myApplicationStatus = null,
        createdAt = "2026-10-03T12:00:00Z"
    )

    private fun application(status: String) = EventApplication(
        id = 8,
        eventId = 30,
        eventTitle = "Limpieza de oficina",
        eventDate = "2026-10-20",
        eventZone = "San Isidro",
        eventStatus = EventStatus.OPEN,
        workerId = 20,
        workerName = "Rosa",
        message = "Tengo disponibilidad",
        status = status,
        createdAt = "2026-10-03T13:00:00Z"
    )

    private class FakeEventsApi : EventsApi {
        var event = Event(
            1, 1, "", "", "", emptyList(), "", "", "2026-10-20", "09:00", "10:00",
            BigDecimal.ONE, 1, 0, BigDecimal.ONE, "2026-10-19T18:00:00Z",
            EventStatus.OPEN, null, null
        )
        var application = EventApplication(
            1, 1, "", "2026-10-20", "", EventStatus.OPEN, 1, "", null,
            ApplicationStatus.PENDING, null
        )
        var lastCreateRequest: CreateEventRequest? = null
        var lastEventId: Int? = null
        var lastApplicationId: Int? = null
        var lastApplyRequest: ApplyToEventRequest? = null

        override suspend fun create(request: CreateEventRequest): Event {
            lastCreateRequest = request
            return event
        }

        override suspend fun searchOpen(
            serviceType: String?,
            zone: String?,
            date: String?
        ): List<Event> = listOf(event)

        override suspend fun getMyEvents(): List<Event> = listOf(event)

        override suspend fun getMyApplications(): List<EventApplication> = listOf(application)

        override suspend fun getEvent(id: Int): Event {
            lastEventId = id
            return event
        }

        override suspend fun cancel(id: Int): Event {
            lastEventId = id
            return event
        }

        override suspend fun complete(id: Int): Event {
            lastEventId = id
            return event
        }

        override suspend fun apply(id: Int, request: ApplyToEventRequest): EventApplication {
            lastEventId = id
            lastApplyRequest = request
            return application
        }

        override suspend fun getApplications(id: Int): List<EventApplication> {
            lastEventId = id
            return listOf(application)
        }

        override suspend fun accept(eventId: Int, appId: Int): EventApplication {
            lastEventId = eventId
            lastApplicationId = appId
            return application
        }

        override suspend fun reject(eventId: Int, appId: Int): EventApplication {
            lastEventId = eventId
            lastApplicationId = appId
            return application
        }

        override suspend fun withdraw(eventId: Int, appId: Int): EventApplication {
            lastEventId = eventId
            lastApplicationId = appId
            return application
        }
    }
}
