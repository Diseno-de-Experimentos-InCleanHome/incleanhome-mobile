package com.incleanhome.mobile

import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentAssigner
import com.incleanhome.mobile.experiments.ExperimentConfig
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.ExperimentSetting
import com.incleanhome.mobile.experiments.InMemoryStore
import com.incleanhome.mobile.experiments.RemoteExperimentConfig
import com.incleanhome.mobile.experiments.RemoteExperimentSetting
import com.incleanhome.mobile.experiments.Variant
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.PrivacyGuard
import com.incleanhome.mobile.experiments.tracking.TrackResult
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import com.incleanhome.mobile.experiments.tracking.TrackingUploader
import com.incleanhome.mobile.experiments.tracking.TrackingUser
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentationTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-25T15:00:00Z"), ZoneOffset.UTC)

    /* USER STORY 43 - Asignación estable y reproducible en web y móvil */
    @Test
    fun testAssigner_MismaEntradaMismoGrupo() {
        //Arrange
        val first = ExperimentAssigner.variantFor("EC-BLQ02", 42, 50)

        //Act
        val repeated = (1..20).map { ExperimentAssigner.variantFor("EC-BLQ02", 42, 50) }

        //Assert
        assertTrue(repeated.all { it == first })
        assertTrue(ExperimentAssigner.bucket("EC-BLQ02", 42) in 0..99)
    }

    /* USER STORY 43 - Proporción 50/50 de la asignación aleatoria */
    @Test
    fun testAssigner_ProporcionCercanaA50PorCiento() {
        //Arrange
        val users = 1..2000

        //Act
        val treatment = users.count { ExperimentAssigner.variantFor("EC-BLQ06", it, 50) == Variant.TREATMENT }

        //Assert
        assertTrue("treatment=$treatment", treatment in 900..1100)
        assertTrue(users.all { ExperimentAssigner.variantFor("EC-BLQ06", it, 0) == Variant.CONTROL })
        assertTrue(users.all { ExperimentAssigner.variantFor("EC-BLQ06", it, 100) == Variant.TREATMENT })
    }

    /* USER STORY 43 - Asignaciones independientes de BLQ02 y BLQ06 (diseño factorial 2x2) */
    @Test
    fun testAssigner_ExperimentosIndependientes() {
        //Arrange
        val users = 1..2000

        //Act
        val bothTreatment = users.count {
            ExperimentAssigner.variantFor("EC-BLQ02", it, 50) == Variant.TREATMENT &&
                ExperimentAssigner.variantFor("EC-BLQ06", it, 50) == Variant.TREATMENT
        }

        //Assert: cada celda del 2x2 debería recibir cerca del 25 %
        assertTrue("bothTreatment=$bothTreatment", bothTreatment in 400..600)
    }

    /* USER STORY 43 - Configuración por texto e identificadores desconocidos */
    @Test
    fun testConfig_ParseaYDescartaEntradasInvalidas() {
        //Act
        val config = ExperimentConfig.parse("EC-BLQ01=on:30;EC-XX=on:50;EC-BLQ02=maybe:50;EC-BLQ06=off")

        //Assert
        assertEquals(ExperimentSetting(true, 30), config.settingFor(Experiment.WORKER_SUGGESTIONS))
        assertEquals(ExperimentSetting.OFF, config.settingFor(Experiment.VERIFIED_REVIEWS))
        assertEquals(ExperimentSetting(false, 50), config.settingFor(Experiment.BOOKING_ESTIMATE))
        val roundTrip = ExperimentConfig.parse(config.encode())
        assertEquals(config.settingFor(Experiment.WORKER_SUGGESTIONS), roundTrip.settingFor(Experiment.WORKER_SUGGESTIONS))
        assertEquals(config.settingFor(Experiment.BOOKING_ESTIMATE), roundTrip.settingFor(Experiment.BOOKING_ESTIMATE))
    }

    /* USER STORY 43 - Escenario 4: desactivación restablece el comportamiento vigente */
    @Test
    fun testService_ExperimentoApagadoMantieneFlujoVigente() {
        //Arrange
        val service = service(config = "EC-BLQ06=off:50", user = TrackingUser(7, "client"))

        //Act
        val assignment = service.assignment(Experiment.BOOKING_ESTIMATE)

        //Assert
        assertFalse(assignment.inExperiment)
        assertFalse(assignment.featureActive)
    }

    /* USER STORY 43 - Unidad de asignación: solo clientes en pruebas A/B */
    @Test
    fun testService_PrestadorNoParticipaEnPruebaABDeClientes() {
        //Arrange
        val service = service(config = "EC-BLQ06=on:100;EC-BLQ01-SA=on:100", user = TrackingUser(9, "worker"))

        //Act
        val estimate = service.assignment(Experiment.BOOKING_ESTIMATE)
        val serviceArea = service.assignment(Experiment.SERVICE_AREA)

        //Assert
        assertFalse(estimate.inExperiment)
        assertTrue(serviceArea.featureActive)
    }

    /* USER STORY 43 - Escenario 2: conserva la asignación aunque cambie el porcentaje */
    @Test
    fun testService_ConservaVarianteDuranteElExperimento() {
        //Arrange
        val store = InMemoryStore()
        var config = "EC-BLQ02=on:100"
        val tracker = tracker(store, TrackingUser(11, "client"))
        val service = ExperimentService(store, { TrackingUser(11, "client") }, { ExperimentConfig.parse(config) }, tracker)
        val before = service.assignment(Experiment.VERIFIED_REVIEWS)

        //Act
        config = "EC-BLQ02=on:0"
        val after = service.assignment(Experiment.VERIFIED_REVIEWS)

        //Assert
        assertEquals(Variant.TREATMENT, before.variant)
        assertEquals(before.variant, after.variant)
    }

    /* USER STORY 43 - Configuración remota reemplaza la de la compilación */
    @Test
    fun testService_ConfiguracionRemotaActivaSinNuevaVersion() {
        //Arrange
        val service = service(config = "EC-BLQ06=off:50", user = TrackingUser(5, "client"))
        val remote = RemoteExperimentConfig(listOf(RemoteExperimentSetting("EC-BLQ06", true, 100)))

        //Act
        service.cacheRemoteConfig(remote.toConfig())

        //Assert
        assertTrue(service.assignment(Experiment.BOOKING_ESTIMATE).featureActive)
    }

    /* USER STORY 43 - Los ajustes locales solo existen en el ambiente de pruebas */
    @Test(expected = IllegalStateException::class)
    fun testService_OverrideLocalBloqueadoEnProduccion() {
        service(config = "", user = TrackingUser(5, "client"))
            .setLocalOverride(Experiment.BOOKING_ESTIMATE, ExperimentSetting(true, 100))
    }

    /* USER STORY 43 - Escenario 3: experiment_exposure una sola vez con experimento y variante */
    @Test
    fun testService_ExposicionUnicaConIdentificadores() {
        //Arrange
        val store = InMemoryStore()
        val user = TrackingUser(21, "client")
        val tracker = tracker(store, user)
        val service = ExperimentService(store, { user }, { ExperimentConfig.parse("EC-BLQ01=on:50") }, tracker)

        //Act
        repeat(3) { service.recordExposure(Experiment.WORKER_SUGGESTIONS, "worker_search") }

        //Assert
        val exposures = tracker.pendingEvents().filter { it.eventName == TrackingPlan.EXPERIMENT_EXPOSURE }
        assertEquals(1, exposures.size)
        assertEquals("EC-BLQ01", exposures.single().experimentId)
        assertEquals(service.assignment(Experiment.WORKER_SUGGESTIONS).variant.wire, exposures.single().variant)
    }

    /* USER STORY 43 - La exposición no se repite aunque el registro de operaciones se renueve */
    @Test
    fun testService_ExposicionNoSeRepiteTrasMuchasOperaciones() {
        //Arrange
        val store = InMemoryStore()
        val user = TrackingUser(22, "client")
        val recorded = mutableListOf<String>()
        val tracker = EventTracker(store, { user }, "1.1.0", "staging", logger = { recorded += it }, clock = clock)
        val service = ExperimentService(store, { user }, { ExperimentConfig.parse("EC-BLQ06=on:50") }, tracker)
        service.recordExposure(Experiment.BOOKING_ESTIMATE, "booking_form")

        //Act
        repeat(2100) { tracker.track(TrackingPlan.BOOKING_FORM_OPENED, mapOf("formId" to "f$it"), operationKey = "f$it") }
        service.recordExposure(Experiment.BOOKING_ESTIMATE, "booking_form")

        //Assert
        assertEquals(1, recorded.count { it.contains("\"eventName\":\"${TrackingPlan.EXPERIMENT_EXPOSURE}\"") })
    }

    /* USER STORY 42 - Escenario 3: el distrito no puede ser una dirección */
    @Test
    fun testTracker_DistritoNoAceptaDirecciones() {
        val tracker = tracker(InMemoryStore(), TrackingUser(1, "client"))

        tracker.track(TrackingPlan.WORKER_SEARCH_PERFORMED, mapOf("searchId" to "s1", "district" to "Av. Arequipa 123"))
        tracker.track(TrackingPlan.WORKER_SEARCH_PERFORMED, mapOf("searchId" to "s2", "district" to "San Isidro"))

        val districts = tracker.pendingEvents().map { it.properties["district"] }
        assertEquals(listOf(null, "San Isidro"), districts)
    }

    /* USER STORY 42 - Escenario 1: propiedades comunes del evento */
    @Test
    fun testTracker_EventoConPropiedadesComunes() {
        //Arrange
        val tracker = tracker(InMemoryStore(), TrackingUser(33, "Client"))

        //Act
        val result = tracker.track(TrackingPlan.WORKER_PROFILE_VIEWED, mapOf("workerId" to 4, "source" to "filters"))

        //Assert
        val event = tracker.pendingEvents().single()
        assertEquals(TrackResult.RECORDED, result)
        assertEquals("mobile", event.platform)
        assertEquals("android", event.os)
        assertEquals("client", event.userRole)
        assertEquals("2026-11-25T15:00:00Z", event.occurredAt)
        assertEquals("staging", event.environment)
        assertNotEquals("33", event.userRef)
        assertEquals(EventTracker.pseudonymize(33), event.userRef)
        assertNull(event.experimentId)
    }

    /* USER STORY 42 - Escenario 3: no registra contraseñas, tokens, direcciones ni mensajes */
    @Test
    fun testTracker_FiltraDatosSensibles() {
        //Arrange
        val tracker = tracker(InMemoryStore(), TrackingUser(1, "client"))

        //Act
        tracker.track(
            TrackingPlan.BOOKING_FORM_OPENED,
            mapOf(
                "formId" to "form-1",
                "workerId" to 3,
                "address" to "Av. Siempre Viva 123",
                "password" to "secret",
                "token" to "eyJhbGciOi.eyJzdWIiOi.c2lnbmF0dXJl",
                "origin" to "cliente@correo.com"
            )
        )

        //Assert
        val properties = tracker.pendingEvents().single().properties
        assertEquals(setOf("formId", "workerId"), properties.keys)
        assertTrue(PrivacyGuard.isSensitiveKey("consumerEmail"))
        assertFalse(PrivacyGuard.isSafeValue("987654321"))
    }

    /* USER STORY 42 - Escenario 4: no duplica la misma operación */
    @Test
    fun testTracker_NoDuplicaOperacion() {
        //Arrange
        val tracker = tracker(InMemoryStore(), TrackingUser(1, "client"))

        //Act
        val first = tracker.track(TrackingPlan.BOOKING_FORM_OPENED, mapOf("formId" to "f1"), operationKey = "f1")
        val second = tracker.track(TrackingPlan.BOOKING_FORM_OPENED, mapOf("formId" to "f1"), operationKey = "f1")

        //Assert
        assertEquals(TrackResult.RECORDED, first)
        assertEquals(TrackResult.DUPLICATE, second)
        assertEquals(1, tracker.pendingEvents().size)
    }

    /* USER STORY 42 - Solo eventos del plan de tracking */
    @Test
    fun testTracker_RechazaEventoFueraDelPlan() {
        val tracker = tracker(InMemoryStore(), TrackingUser(1, "client"))

        assertEquals(TrackResult.REJECTED, tracker.track("password_changed"))
        assertTrue(tracker.pendingEvents().isEmpty())
    }

    /* USER STORY 42 - Envío a la API: conserva los eventos si falla */
    @Test
    fun testTracker_ConservaEventosSiElEnvioFalla() = runBlocking {
        //Arrange
        var accept = false
        val store = InMemoryStore()
        val tracker = EventTracker(
            store, { TrackingUser(1, "client") }, "1.1.0", "staging",
            uploader = TrackingUploader { accept }, clock = clock
        )
        tracker.track(TrackingPlan.FAQ_VIEWED, mapOf("category" to "bookings", "faqId" to "bookings-cancel"))

        //Act
        val failed = tracker.flush()
        accept = true
        val sent = tracker.flush()

        //Assert
        assertEquals(0, failed)
        assertEquals(1, sent)
        assertTrue(tracker.pendingEvents().isEmpty())
    }

    private fun tracker(store: InMemoryStore, user: TrackingUser?) =
        EventTracker(store, { user }, "1.1.0", "staging", clock = clock)

    private fun service(config: String, user: TrackingUser?): ExperimentService {
        val store = InMemoryStore()
        return ExperimentService(store, { user }, { ExperimentConfig.parse(config) }, tracker(store, user))
    }
}
