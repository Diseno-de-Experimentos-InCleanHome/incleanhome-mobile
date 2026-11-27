package com.incleanhome.mobile

import com.google.gson.Gson
import com.incleanhome.mobile.booking.data.Booking
import com.incleanhome.mobile.booking.data.BookingApi
import com.incleanhome.mobile.booking.data.BookingEstimateRequest
import com.incleanhome.mobile.booking.data.BookingEstimateResponse
import com.incleanhome.mobile.booking.data.BookingRepository
import com.incleanhome.mobile.booking.data.BookingResult
import com.incleanhome.mobile.booking.data.CreateBookingRequest
import com.incleanhome.mobile.booking.data.UpdateBookingStatusRequest
import com.incleanhome.mobile.booking.domain.BookingEstimator
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.InMemoryStore
import com.incleanhome.mobile.experiments.RemoteExperimentConfig
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingEvent
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.experiments.tracking.TrackingUploader
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
import com.incleanhome.mobile.reviews.data.WorkerReviewSummary
import com.incleanhome.mobile.reviews.domain.ReviewHistory
import com.incleanhome.mobile.reviews.domain.ReviewSort
import com.incleanhome.mobile.ui.format.rateLimitedSeconds
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Contracts of incleanhome-backend v1.1.0 (docs/to-be-evidencia.md) used by the To-Be features. */
class BackendIntegrationContractsTest {

    private val gson = Gson()

    /* USER STORY 34 - Escenarios 1 y 2: el resumen de la API define promedio, distribución y servicios */
    @Test
    fun testReviewHistory_UsaElResumenDeLaApi() {
        //Arrange: ejemplo de GET /api/reviews/worker/2 y /summary
        val reviews = gson.fromJson(
            """[ { "id": 1, "bookingId": 4, "clientId": 1, "workerId": 2, "clientName": "Carlos Cliente",
                   "rating": 5, "comment": "Excelente", "createdAt": "2026-11-26T15:00:00+00:00",
                   "serviceType": "cocina", "serviceDate": "2026-12-05", "isVerified": true },
                 { "id": 2, "bookingId": 7, "clientId": 3, "workerId": 2, "clientName": "Otro",
                   "rating": 2, "comment": null, "createdAt": "2026-11-20T15:00:00+00:00",
                   "serviceType": null, "serviceDate": null, "isVerified": false } ]""",
            Array<Review>::class.java
        ).toList()
        val summary = gson.fromJson(
            """{ "workerId": 2, "verifiedReviewCount": 3, "averageRating": 4.33,
                 "distribution": { "5": 1, "4": 2, "3": 0, "2": 0, "1": 0 },
                 "completedServices": 4, "message": null }""",
            WorkerReviewSummary::class.java
        )

        //Act
        val history = ReviewHistory.from(reviews, summary)

        //Assert: isVerified de la API manda sobre la regla local
        assertEquals(listOf(1), history.verifiedReviews.map(Review::id))
        assertEquals(BigDecimal("4.33"), history.averageRating)
        assertEquals(mapOf(5 to 1, 4 to 2, 3 to 0, 2 to 0, 1 to 0), history.distribution)
        assertEquals(4, history.completedServices)
        assertEquals("2026-12-05", history.verifiedReviews.single().serviceDate)
    }

    /* USER STORY 34 - Escenario 3: sin reseñas verificadas la API no da promedio */
    @Test
    fun testReviewHistory_SinResenasVerificadas() {
        val summary = gson.fromJson(
            """{ "workerId": 2, "verifiedReviewCount": 0, "averageRating": null,
                 "distribution": { "5": 0, "4": 0, "3": 0, "2": 0, "1": 0 },
                 "completedServices": 1, "message": "Este trabajador todavía no tiene reseñas verificadas." }""",
            WorkerReviewSummary::class.java
        )

        val history = ReviewHistory.from(emptyList(), summary)

        assertEquals(0, history.verifiedCount)
        assertNull(history.averageRating)
        assertEquals(1, history.completedServices)
    }

    /* USER STORY 34 - Filtro por servicio sin distinguir mayúsculas, como el parámetro serviceType */
    @Test
    fun testReviewHistory_FiltroSinDistinguirMayusculas() {
        val reviews = listOf(
            Review(1, 10, 1, 2, "A", 5, "Bien", "2026-11-01T10:00:00Z", "Cocina", null, true),
            Review(2, 11, 1, 2, "B", 4, "Bien", "2026-11-02T10:00:00Z", "lavanderia", null, true)
        )

        assertEquals(listOf(1), ReviewHistory.arrange(reviews, ReviewSort.NEWEST, "cocina").map(Review::id))
    }

    /* USER STORY 41 - Escenario 1: el monto mostrado es el que calcula POST /api/bookings/estimate */
    @Test
    fun testEstimate_SeObtieneDeLaApi() = runBlocking {
        //Arrange: ejemplo del contrato (tarifa S/ 33.33, 09:00 a 10:40)
        val api = EstimateBookingApi(BookingEstimateResponse(BigDecimal("33.33"), BigDecimal("1.67"), BigDecimal("55.66")))
        val request = BookingEstimateRequest(2, "limpieza_general", "2026-12-03", "09:00", "10:40")

        //Act
        val result = BookingRepository(api).estimate(request)

        //Assert
        assertEquals(request, api.lastEstimate)
        result as BookingResult.Success
        val estimate = BookingEstimator.fromApi(result.data)
        assertTrue(estimate.fromApi)
        assertEquals(BigDecimal("55.66"), estimate.totalAmount)
        // La regla local coincide con la de la API para el mismo horario.
        assertEquals(estimate.totalAmount, BookingEstimator.estimate(BigDecimal("33.33"), LocalTime.of(9, 0), LocalTime.of(10, 40))?.totalAmount)
    }

    /* USER STORY 41 - La API rechaza la estimación con su motivo (400) */
    @Test
    fun testEstimate_RechazoDeLaApiConservaElMotivo() = runBlocking {
        val api = EstimateBookingApi(error = httpError(400, """{ "error": "El trabajador no ofrece ese servicio" }"""))

        val result = BookingRepository(api).estimate(BookingEstimateRequest(2, "cocina", "2026-12-03", "09:00", "10:00"))

        assertEquals(BookingResult.Error("El trabajador no ofrece ese servicio", "http_400"), result)
    }

    /* USER STORY 41 - Duración mínima de 30 minutos, igual que la API */
    @Test
    fun testEstimate_DuracionMinima() {
        assertNull(BookingEstimator.estimate(BigDecimal("25"), LocalTime.of(9, 0), LocalTime.of(9, 20)))
        assertFalse(BookingEstimator.meetsMinimumDuration(LocalTime.of(9, 0), LocalTime.of(9, 29)))
        assertTrue(BookingEstimator.meetsMinimumDuration(LocalTime.of(9, 0), LocalTime.of(9, 30)))
    }

    /* USER STORY 41 - La fecha pasada se evalúa con la fecha de Perú (19:00-23:59 en Lima) */
    @Test
    fun testEstimate_FechaDePeru() {
        // 2026-11-28 02:00 UTC = 2026-11-27 21:00 en Lima
        val clock = Clock.fixed(Instant.parse("2026-11-28T02:00:00Z"), ZoneOffset.UTC)

        assertEquals(LocalDate.of(2026, 11, 27), BookingEstimator.todayInPeru(clock))
    }

    /* USER STORY 39 - Escenarios 1, 2 y 4: el 429 informa el tiempo de espera de la API */
    @Test
    fun testAuth_TiempoDeEsperaDeLaApi() = runBlocking {
        val header = AuthRepository(RejectingAuthApi(rateLimited(retryAfterHeader = "30", body = "{}")))
        val body = AuthRepository(RejectingAuthApi(rateLimited(retryAfterHeader = null,
            body = """{ "error": "Demasiados intentos. Vuelve a intentarlo en 45 segundos.", "retryAfterSeconds": 45 }""")))
        val minute = AuthRepository(RejectingAuthApi(rateLimited(retryAfterHeader = "60",
            body = """{ "error": "Demasiados intentos.", "retryAfterSeconds": 60 }""")))

        val login = header.login("qa@incleanhome.test", "x")
        val totp = body.verifyTwoFactor("challenge", "000000")

        assertEquals(LoginResult.Error("Demasiados intentos. Espera 30 segundos antes de volver a intentarlo."), login)
        assertEquals(LoginResult.Error("Demasiados intentos. Espera 45 segundos antes de volver a intentarlo."), totp)
        assertEquals(LoginResult.Error(RATE_LIMITED_MESSAGE), minute.login("qa@incleanhome.test", "x"))
        assertEquals(30, rateLimitedSeconds((login as LoginResult.Error).message))
    }

    /* USER STORY 42 - Los eventos sin usuario no se envían con la sesión de otra cuenta */
    @Test
    fun testTracker_NoEnviaEventosSinUsuario() = runBlocking {
        //Arrange
        var user: TrackingUser? = null
        val uploaded = mutableListOf<TrackingEvent>()
        val tracker = EventTracker(
            InMemoryStore(), { user }, "1.0", "staging",
            uploader = TrackingUploader { uploaded += it; true },
            clock = Clock.fixed(Instant.parse("2026-11-27T15:00:00Z"), ZoneOffset.UTC)
        )
        tracker.track(TrackingPlan.FAQ_VIEWED, mapOf("category" to "bookings", "faqId" to "bookings-cancel"))
        user = TrackingUser(5, "client")
        tracker.track(TrackingPlan.FAQ_VIEWED, mapOf("category" to "account", "faqId" to "account-attempts"))

        //Act
        val sent = tracker.flush()

        //Assert
        assertEquals(1, sent)
        assertEquals(EventTracker.pseudonymize(5), uploaded.single().userRef)
        assertEquals(1, tracker.pendingEvents().size)
        assertNull(tracker.pendingEvents().single().userRef)
    }

    /* USER STORY 42 - El lote enviado tiene el formato de POST /api/tracking/events */
    @Test
    fun testTracker_FormatoDelLote() {
        val tracker = EventTracker(
            InMemoryStore(), { TrackingUser(5, "Client") }, "1.0-staging", "staging",
            clock = Clock.fixed(Instant.parse("2026-11-27T15:00:00Z"), ZoneOffset.UTC)
        )
        tracker.track(TrackingPlan.WORKER_PROFILE_VIEWED, mapOf("workerId" to 3, "source" to "filters"))

        val json = gson.toJsonTree(com.incleanhome.mobile.experiments.TrackingBatch(tracker.pendingEvents())).asJsonObject
        val event = json.getAsJsonArray("events")[0].asJsonObject

        listOf("eventId", "eventName", "occurredAt", "platform", "os", "appVersion", "environment", "userRole", "userRef", "sessionId")
            .forEach { assertTrue("Falta $it", event.has(it)) }
        assertEquals("mobile", event.get("platform").asString)
        assertEquals("client", event.get("userRole").asString)
        assertEquals("2026-11-27T15:00:00Z", event.get("occurredAt").asString)
    }

    /* USER STORY 43 - Escenarios 1 y 4: la configuración por defecto de la API deja todo apagado */
    @Test
    fun testRemoteConfig_ContratoDeLaApi() {
        val off = gson.fromJson(
            """{ "experiments": [
              { "id": "EC-BLQ01", "enabled": false, "treatmentPercent": 50 },
              { "id": "EC-BLQ02", "enabled": false, "treatmentPercent": 50 },
              { "id": "EC-BLQ03", "enabled": false, "treatmentPercent": 50 },
              { "id": "EC-BLQ06", "enabled": false, "treatmentPercent": 50 },
              { "id": "EC-BLQ04", "enabled": false, "treatmentPercent": 100 },
              { "id": "EC-BLQ01-SA", "enabled": false, "treatmentPercent": 100 } ] }""",
            RemoteExperimentConfig::class.java
        ).toConfig()
        val blq06 = gson.fromJson(
            """{ "experiments": [ { "id": "EC-BLQ06", "enabled": true, "treatmentPercent": 50 } ] }""",
            RemoteExperimentConfig::class.java
        ).toConfig()

        assertTrue(Experiment.entries.none { off.settingFor(it).enabled })
        assertTrue(blq06.settingFor(Experiment.BOOKING_ESTIMATE).enabled)
        assertEquals(50, blq06.settingFor(Experiment.BOOKING_ESTIMATE).treatmentPercent)
        assertFalse(blq06.settingFor(Experiment.VERIFIED_REVIEWS).enabled)
    }

    private fun httpError(code: Int, body: String, headers: Map<String, String> = emptyMap()): HttpException {
        val raw = okhttp3.Response.Builder()
            .code(code)
            .message("error")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/api/").build())
            .headers(headersOf(*headers.flatMap { listOf(it.key, it.value) }.toTypedArray()))
            .build()
        return HttpException(Response.error<Any>(body.toResponseBody("application/json".toMediaType()), raw))
    }

    private fun rateLimited(retryAfterHeader: String?, body: String): HttpException =
        httpError(429, body, retryAfterHeader?.let { mapOf("Retry-After" to it) } ?: emptyMap())

    private class EstimateBookingApi(
        private val response: BookingEstimateResponse? = null,
        private val error: HttpException? = null
    ) : BookingApi {
        var lastEstimate: BookingEstimateRequest? = null

        override suspend fun estimate(request: BookingEstimateRequest): BookingEstimateResponse {
            lastEstimate = request
            error?.let { throw it }
            return checkNotNull(response)
        }

        override suspend fun createBooking(request: CreateBookingRequest): Booking = error("not used")
        override suspend fun getMyBookings(): List<Booking> = error("not used")
        override suspend fun updateStatus(bookingId: Int, request: UpdateBookingStatusRequest): Booking = error("not used")
    }

    private class RejectingAuthApi(private val exception: HttpException) : AuthApi {
        override suspend fun login(request: LoginRequest): AuthResponse = throw exception
        override suspend fun registerClient(request: RegisterClientRequest): AuthResponse = throw exception
        override suspend fun registerWorker(request: RegisterWorkerRequest): AuthResponse = throw exception
        override suspend fun acceptTerms(authorization: String, request: AcceptTermsRequest): AuthResponse = throw exception
        override suspend fun setupTwoFactor(authorization: String): TwoFactorSetupResponse = throw exception
        override suspend fun enableTwoFactor(authorization: String, request: Enable2faRequest): AuthResponse = throw exception
        override suspend fun verifyTwoFactor(request: Verify2faRequest): AuthResponse = throw exception
    }
}
