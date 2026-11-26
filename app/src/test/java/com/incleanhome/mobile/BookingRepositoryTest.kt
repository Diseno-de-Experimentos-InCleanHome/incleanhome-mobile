package com.incleanhome.mobile

import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingApi
import com.incleanhome.mobile.booking.data.BookingRepository
import com.incleanhome.mobile.booking.data.BookingResult
import com.incleanhome.mobile.booking.data.BookingStatus
import com.incleanhome.mobile.booking.data.CreateBookingRequest
import com.incleanhome.mobile.booking.data.UpdateBookingStatusRequest
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingRepositoryTest {

    /* USER STORY 10 - Reservar servicio */
    @Test
    fun testCreateBooking_EnviaSolicitudConEstadoPendienteEnRespuesta() = runBlocking {
        //Arrange
        val api = FakeBookingApi()
        val repository = BookingRepository(api)
        val request = CreateBookingRequest(
            workerId = 20,
            serviceType = "limpieza_general",
            date = "2026-10-10",
            startTime = "09:00",
            endTime = "11:00",
            hours = BigDecimal("2.00"),
            address = "Av. Principal 123",
            notes = "Tocar el timbre"
        )
        api.booking = booking(status = BookingStatus.PENDING)

        //Act
        val resultado = repository.create(request)

        //Assert
        assertEquals(request, api.lastCreateRequest)
        assertTrue(resultado is BookingResult.Success)
        resultado as BookingResult.Success
        assertEquals(BookingStatus.PENDING, resultado.data.status)
    }

    /* USER STORY 11 - Cancelar reserva como cliente */
    @Test
    fun testUpdateStatus_CancelarReservaEnviaEstadoCancelled() = runBlocking {
        //Arrange
        val api = FakeBookingApi()
        api.booking = booking(status = BookingStatus.CANCELLED)
        val repository = BookingRepository(api)

        //Act
        val resultado = repository.updateStatus(15, BookingStatus.CANCELLED)

        //Assert
        assertEquals(15, api.lastBookingId)
        assertEquals(UpdateBookingStatusRequest(BookingStatus.CANCELLED), api.lastStatusRequest)
        assertTrue(resultado is BookingResult.Success)
    }

    /* USER STORY 12 - Gestionar solicitudes como prestador */
    @Test
    fun testUpdateStatus_AceptarRechazarYCompletarUsaEstadosCorrectos() = runBlocking {
        //Arrange
        val api = FakeBookingApi()
        val repository = BookingRepository(api)
        val estados = listOf(
            BookingStatus.ACCEPTED,
            BookingStatus.REJECTED,
            BookingStatus.COMPLETED
        )

        //Act
        val recibidos = mutableListOf<String>()
        for (estado in estados) {
            api.booking = booking(status = estado)
            repository.updateStatus(15, estado)
            recibidos += api.lastStatusRequest!!.status
        }

        //Assert
        assertEquals(estados, recibidos)
    }

    private fun booking(status: String) = Booking(
        id = 15,
        clientId = 3,
        workerId = 20,
        clientName = "Cliente",
        workerName = "Rosa",
        serviceType = "limpieza_general",
        date = "2026-10-10",
        startTime = "09:00",
        endTime = "11:00",
        hours = BigDecimal("2.00"),
        address = "Av. Principal 123",
        notes = "",
        hourlyRate = BigDecimal("30.00"),
        totalAmount = BigDecimal("60.00"),
        status = status,
        hasReview = false,
        createdAt = "2026-10-03T12:00:00Z"
    )

    private class FakeBookingApi : BookingApi {
        var booking: Booking = Booking(
            1, 1, 1, "", "", "", "2026-10-10", "09:00", "10:00",
            BigDecimal.ONE, "", "", BigDecimal.ONE, BigDecimal.ONE,
            BookingStatus.PENDING, false, null
        )
        var lastCreateRequest: CreateBookingRequest? = null
        var lastBookingId: Int? = null
        var lastStatusRequest: UpdateBookingStatusRequest? = null

        override suspend fun createBooking(request: CreateBookingRequest): Booking {
            lastCreateRequest = request
            return booking
        }

        override suspend fun getMyBookings(): List<Booking> = listOf(booking)

        override suspend fun updateStatus(
            bookingId: Int,
            request: UpdateBookingStatusRequest
        ): Booking {
            lastBookingId = bookingId
            lastStatusRequest = request
            return booking
        }
    }
}
