package com.incleanhome.mobile

import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingStatus
import com.incleanhome.mobile.booking.domain.BookingEstimator
import com.incleanhome.mobile.experiments.ExperimentConfig
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.InMemoryStore
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.experiments.tracking.TrackingUser
import com.incleanhome.mobile.iam.data.AcceptTermsRequest
import com.incleanhome.mobile.iam.data.AuthApi
import com.incleanhome.mobile.iam.data.AuthRepository
import com.incleanhome.mobile.iam.data.AuthResponse
import com.incleanhome.mobile.iam.data.Enable2faRequest
import com.incleanhome.mobile.iam.data.LoginRequest
import com.incleanhome.mobile.iam.data.LoginResult
import com.incleanhome.mobile.iam.data.RATE_LIMITED_MESSAGE
import com.incleanhome.mobile.iam.data.RegisterClientRequest
import com.incleanhome.mobile.iam.data.RegisterWorkerRequest
import com.incleanhome.mobile.iam.data.TwoFactorSetupResponse
import com.incleanhome.mobile.iam.data.Verify2faRequest
import com.incleanhome.mobile.reviews.data.Review
import com.incleanhome.mobile.reviews.domain.ReviewHistory
import com.incleanhome.mobile.reviews.domain.ReviewReminderPolicy
import com.incleanhome.mobile.reviews.domain.ReviewReminderService
import com.incleanhome.mobile.reviews.domain.ReviewSort
import com.incleanhome.mobile.search.data.AvailabilitySlot
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerApi
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import com.incleanhome.mobile.search.domain.SuggestionRequest
import com.incleanhome.mobile.search.domain.WorkerSuggestionService
import com.incleanhome.mobile.worker.data.ServiceArea
import com.incleanhome.mobile.worker.data.ServiceAreaRepository
import com.incleanhome.mobile.worker.data.ServiceAreaResult
import com.incleanhome.mobile.worker.data.ServiceAreaRules
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ToBeFeaturesTest {

    /* USER STORY 41 - Escenario 1: tarifa, duración y monto estimado */
    @Test
    fun testEstimate_TarifaPorDuracion() {
        //Act
        val estimate = BookingEstimator.estimate(BigDecimal("25.00"), LocalTime.of(9, 0), LocalTime.of(11, 30))

        //Assert
        assertEquals(BigDecimal("2.50"), estimate?.hours)
        assertEquals(BigDecimal("62.50"), estimate?.totalAmount)
    }

    /* USER STORY 41 - Escenario 3: mismo redondeo que la API (Math.Round, modo bancario) */
    @Test
    fun testEstimate_RedondeoCoincideConLaApi() {
        //Act: 24.25 x 0.50 = 12.125 -> la API registra 12.12
        val total = BookingEstimator.total(BigDecimal("24.25"), BigDecimal("0.50"))

        //Assert
        assertEquals(BigDecimal("12.12"), total)
        assertTrue(BookingEstimator.matches(BigDecimal("12.12"), BigDecimal("12.120")))
    }

    /* USER STORY 41 - Escenario 2: horario inválido no produce estimación */
    @Test
    fun testEstimate_HorarioInvalido() {
        assertNull(BookingEstimator.estimate(BigDecimal("20"), LocalTime.of(11, 0), LocalTime.of(9, 0)))
        assertNull(BookingEstimator.estimate(BigDecimal.ZERO, LocalTime.of(9, 0), LocalTime.of(10, 0)))
    }

    /* USER STORY 33 - Escenario 1: hasta cinco prestadores disponibles, ordenados por calificación */
    @Test
    fun testSuggestions_FiltraDisponibilidadYOrdena() = runBlocking {
        //Arrange: 2026-12-01 es martes (dayOfWeek 2 en la API)
        val api = SuggestionWorkerApi(
            workers = (1..7).map { worker(it, rating = "4.${it}") },
            availability = (1..7).associateWith { id ->
                if (id == 3) listOf(slot(2, "13:00", "18:00")) else listOf(slot(2, "08:00", "12:00"))
            }
        )
        val service = WorkerSuggestionService(WorkerRepository(api))

        //Act
        val result = service.suggest(request(LocalDate.of(2026, 12, 1)))

        //Assert
        assertEquals("limpieza_general", api.lastServiceType)
        assertEquals("Miraflores", api.lastZone)
        result as WorkerResult.Success
        assertEquals(listOf(7, 6, 5, 4, 2), result.data.map(Worker::id))
    }

    /* USER STORY 33 - Escenario 2: sin prestadores cercanos y disponibles */
    @Test
    fun testSuggestions_SinCoincidencias() = runBlocking {
        //Arrange: sábado sin disponibilidad registrada
        val api = SuggestionWorkerApi(listOf(worker(1, "5.0")), mapOf(1 to listOf(slot(2, "08:00", "12:00"))))

        //Act
        val result = WorkerSuggestionService(WorkerRepository(api)).suggest(request(LocalDate.of(2026, 12, 5)))

        //Assert
        assertTrue((result as WorkerResult.Success).data.isEmpty())
    }

    /* USER STORY 33 - Convención de días de la API (0 = domingo) */
    @Test
    fun testSuggestions_DiaDeLaSemanaDeLaApi() {
        assertEquals(0, WorkerSuggestionService.backendDayOfWeek(LocalDate.of(2026, 11, 29)))
        assertEquals(6, WorkerSuggestionService.backendDayOfWeek(LocalDate.of(2026, 11, 28)))
    }

    /* USER STORY 34 - Escenarios 1 y 2: reseñas verificadas, promedio y distribución */
    @Test
    fun testReviewHistory_PromedioYDistribucion() {
        //Arrange
        val reviews = listOf(review(1, 10, 5, "2026-11-01T10:00:00+00:00"), review(2, 11, 4, "2026-11-10T10:00:00+00:00"), review(3, 12, 4, "2026-10-01T10:00:00+00:00"))

        //Act
        val history = ReviewHistory.from(reviews, summary = null)

        //Assert: sin resumen de la API se usan las cifras locales (2 decimales, como la API)
        assertEquals(3, history.verifiedCount)
        assertEquals(BigDecimal("4.33"), history.averageRating)
        assertEquals(mapOf(5 to 1, 4 to 2, 3 to 0, 2 to 0, 1 to 0), history.distribution)
        assertNull(history.completedServices)
        assertEquals(listOf(2, 1, 3), ReviewHistory.arrange(reviews, ReviewSort.NEWEST, null).map(Review::id))
        assertEquals(listOf(3, 1, 2), ReviewHistory.arrange(reviews, ReviewSort.OLDEST, null).map(Review::id))
    }

    /* USER STORY 34 - Escenario 3: sin reseñas no presenta promedio */
    @Test
    fun testReviewHistory_SinResenas() {
        val history = ReviewHistory.from(emptyList(), summary = null)

        assertEquals(0, history.verifiedCount)
        assertNull(history.averageRating)
    }

    /* USER STORY 34 - Filtro por tipo de servicio cuando la API lo informe */
    @Test
    fun testReviewHistory_FiltroPorServicio() {
        val reviews = listOf(review(1, 10, 5, "a", "limpieza_general"), review(2, 11, 3, "b", "lavanderia"))

        assertEquals(listOf(2), ReviewHistory.arrange(reviews, ReviewSort.NEWEST, "lavanderia").map(Review::id))
        assertEquals(listOf("lavanderia", "limpieza_general"), ReviewHistory.from(reviews, null).serviceTypes)
    }

    /* USER STORY 35 - Escenario 1: recordatorio tras el plazo configurado */
    @Test
    fun testReminderPolicy_RespetaPlazoYEstado() {
        val now = LocalDateTime.of(2026, 11, 25, 12, 0)

        assertTrue(ReviewReminderPolicy.isDue(booking(1, "2026-11-24", "11:00"), now, 24))
        assertFalse(ReviewReminderPolicy.isDue(booking(2, "2026-11-24", "13:00"), now, 24))
        assertFalse(ReviewReminderPolicy.isDue(booking(3, "2026-11-20", "11:00", hasReview = true), now, 24))
        assertFalse(ReviewReminderPolicy.isDue(booking(4, "2026-11-20", "11:00", status = BookingStatus.ACCEPTED), now, 24))
    }

    /* USER STORY 35 - Escenarios 3 y 5: un recordatorio por reserva y solo en el grupo experimental */
    @Test
    fun testReminderService_UnaVezPorReservaYSoloTratamiento() {
        //Arrange
        val clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC)
        val bookings = listOf(booking(1, "2026-11-24", "10:00"), booking(2, "2026-11-23", "10:00"))
        val treatment = reminderService("EC-BLQ03=on:100", clock)
        val control = reminderService("EC-BLQ03=on:0", clock)

        //Act
        val first = treatment.first.deliverDue(bookings)
        val second = treatment.first.deliverDue(bookings)
        val controlDelivered = control.first.deliverDue(bookings)

        //Assert
        assertEquals(listOf(1, 2), first.map(Booking::id))
        assertTrue(second.isEmpty())
        assertTrue(controlDelivered.isEmpty())
        val sent = treatment.second.pendingEvents().filter { it.eventName == TrackingPlan.REVIEW_REMINDER_SENT }
        assertEquals(2, sent.size)
        assertEquals("in_app", sent.first().properties["channel"])
        assertEquals("EC-BLQ03", sent.first().experimentId)
    }

    /* USER STORY 32 - Escenario 2: radio fuera de rango */
    @Test
    fun testServiceArea_RangoPermitido() {
        assertTrue(ServiceAreaRules.radiusError(0, 1, 15))
        assertTrue(ServiceAreaRules.radiusError(16, 1, 15))
        assertTrue(ServiceAreaRules.radiusError(null, 1, 15))
        assertFalse(ServiceAreaRules.radiusError(5, 1, 15))
    }

    /* USER STORY 32 - Sin endpoint publicado no se declara guardado */
    @Test
    fun testServiceArea_SinEndpointNoGuarda() = runBlocking {
        val repository = ServiceAreaRepository(apiEnabled = false) { error("API must not be called") }

        assertEquals(ServiceAreaResult.Unavailable, repository.save(ServiceArea("miraflores", 5)))
    }

    /* USER STORY 39 - Escenarios 1 y 2: respuesta 429 del límite de intentos */
    @Test
    fun testAuth_LimiteDeIntentosInformaEspera() = runBlocking {
        val repository = AuthRepository(RateLimitedAuthApi())

        val login = repository.login("cliente@incleanhome.com", "Clave-123")
        val totp = repository.verifyTwoFactor("challenge", "123456")

        assertEquals(LoginResult.Error(RATE_LIMITED_MESSAGE), login)
        assertEquals(LoginResult.Error(RATE_LIMITED_MESSAGE), totp)
    }

    private fun reminderService(config: String, clock: Clock): Pair<ReviewReminderService, EventTracker> {
        val store = InMemoryStore()
        val user = TrackingUser(8, "client")
        val tracker = EventTracker(store, { user }, "1.1.0", "staging", clock = clock)
        val experiments = ExperimentService(store, { user }, { ExperimentConfig.parse(config) }, tracker)
        return ReviewReminderService(store, experiments, tracker, { user.userId }, 24, clock, ZoneOffset.UTC) to tracker
    }

    private fun request(date: LocalDate) =
        SuggestionRequest("limpieza_general", date, LocalTime.of(9, 0), LocalTime.of(11, 0), "Miraflores")

    private fun worker(id: Int, rating: String) = Worker(
        id, "Prestador $id", null, 30, "female", listOf("limpieza_general"), listOf("miraflores"),
        BigDecimal("25"), 3, "", BigDecimal(rating), id
    )

    private fun slot(day: Int, start: String, end: String) = AvailabilitySlot(day, day, start, end, true)

    private fun review(id: Int, bookingId: Int, rating: Int, createdAt: String, serviceType: String? = null) =
        Review(id, bookingId, 1, 2, "Cliente", rating, "", createdAt, serviceType, null)

    private fun booking(
        id: Int,
        date: String,
        endTime: String,
        hasReview: Boolean = false,
        status: String = BookingStatus.COMPLETED
    ) = Booking(
        id, 8, 2, "Cliente", "Prestador", "limpieza_general", date, "08:00", endTime, BigDecimal("2"),
        "Dirección", "", BigDecimal("25"), BigDecimal("50"), status, hasReview, null
    )

    private class SuggestionWorkerApi(
        val workers: List<Worker>,
        val availability: Map<Int, List<AvailabilitySlot>>
    ) : WorkerApi {
        var lastServiceType: String? = null
        var lastZone: String? = null

        override suspend fun searchWorkers(serviceType: String?, zone: String?): List<Worker> {
            lastServiceType = serviceType
            lastZone = zone
            return workers
        }

        override suspend fun getWorker(workerId: Int): Worker = workers.first { it.id == workerId }

        override suspend fun getAvailability(workerId: Int): List<AvailabilitySlot> = availability[workerId].orEmpty()
    }

    private class RateLimitedAuthApi : AuthApi {
        private fun tooMany(): Nothing = throw HttpException(Response.error<Any>(429, "".toResponseBody(null)))
        override suspend fun login(request: LoginRequest): AuthResponse = tooMany()
        override suspend fun registerClient(request: RegisterClientRequest): AuthResponse = tooMany()
        override suspend fun registerWorker(request: RegisterWorkerRequest): AuthResponse = tooMany()
        override suspend fun acceptTerms(authorization: String, request: AcceptTermsRequest): AuthResponse = tooMany()
        override suspend fun setupTwoFactor(authorization: String): TwoFactorSetupResponse = tooMany()
        override suspend fun enableTwoFactor(authorization: String, request: Enable2faRequest): AuthResponse = tooMany()
        override suspend fun verifyTwoFactor(request: Verify2faRequest): AuthResponse = tooMany()
    }
}
