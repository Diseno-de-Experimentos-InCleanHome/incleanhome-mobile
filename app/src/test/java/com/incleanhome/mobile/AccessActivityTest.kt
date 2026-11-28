package com.incleanhome.mobile

import com.google.gson.Gson
import com.incleanhome.mobile.iam.data.AccessActivity
import com.incleanhome.mobile.iam.data.AccessActivityApi
import com.incleanhome.mobile.iam.data.AccessActivityRepository
import com.incleanhome.mobile.iam.data.AccessActivityResult
import com.incleanhome.mobile.iam.data.AccessEntry
import com.incleanhome.mobile.iam.data.ChangePasswordRequest
import com.incleanhome.mobile.iam.presentation.AccessActivityFormat
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AccessActivityTest {

    /* USER STORY 40 - Escenario 1: últimos diez accesos con fecha, plataforma, resultado y segundo factor */
    @Test
    fun testActividad_ContratoDeLaApi() = runBlocking {
        //Arrange: respuesta con la forma documentada por incleanhome-backend v1.2.0
        val json = """{"twoFactorEnabled":true,"accesses":[
            {"id":69,"occurredAt":"2026-11-28T01:32:30.43+00:00","platform":"mobile","twoFactorVerified":true,"result":"success"},
            {"id":68,"occurredAt":"2026-11-28T01:31:02.11+00:00","platform":"web","twoFactorVerified":false,"result":"failed"}],
            "passwordChangeRequired":false}"""
        val api = FakeAccessApi(activity = Gson().fromJson(json, AccessActivity::class.java))

        //Act
        val result = AccessActivityRepository { api }.load()

        //Assert
        result as AccessActivityResult.Success
        assertTrue(result.data.twoFactorEnabled)
        assertEquals(listOf(69, 68), result.data.accesses.map(AccessEntry::id))
        assertTrue(result.data.accesses[0].succeeded)
        assertFalse(result.data.accesses[1].succeeded)
        assertEquals("27/11/2026 20:32", AccessActivityFormat.localTime(result.data.accesses[0].occurredAt, ZoneId.of("America/Lima")))
    }

    /* USER STORY 40 - Escenario 1: nunca más de diez aunque la API enviara más */
    @Test
    fun testActividad_MaximoDiez() = runBlocking {
        val accesses = (1..12).map { AccessEntry(it, "2026-11-28T01:00:00Z", "mobile", true, "success") }
        val result = AccessActivityRepository { FakeAccessApi(AccessActivity(true, accesses, false)) }.load()

        assertEquals(10, (result as AccessActivityResult.Success).data.accesses.size)
    }

    /* USER STORY 40 - Escenario 3: cuenta sin actividad previa */
    @Test
    fun testActividad_SinAccesos() = runBlocking {
        val result = AccessActivityRepository { FakeAccessApi(AccessActivity(false, emptyList(), false)) }.load()

        assertTrue((result as AccessActivityResult.Success).data.accesses.isEmpty())
    }

    /* USER STORY 40 - Escenario 2: reportar un acceso propio (204) y uno ajeno o inexistente (404) */
    @Test
    fun testReporte_PropioYAjeno() = runBlocking {
        val api = FakeAccessApi(AccessActivity(true, emptyList(), false), ownIds = setOf(69))
        val repository = AccessActivityRepository { api }

        assertEquals(AccessActivityResult.Success(Unit), repository.report(69))
        assertEquals(AccessActivityResult.Error(AccessActivityRepository.NOT_FOUND_MESSAGE), repository.report(1))
        assertEquals(listOf(69, 1), api.reported)
    }

    /* USER STORY 40 - Una sesión cerrada desde otro dispositivo responde 401 */
    @Test
    fun testSesionCerrada() = runBlocking {
        val api = FakeAccessApi(failure = HttpException(Response.error<Any>(401, """{"error":"Tu sesión se cerró. Inicia sesión nuevamente"}""".toResponseBody(JSON))))

        val result = AccessActivityRepository { api }.load()

        assertEquals(AccessActivityResult.Error("Tu sesión se cerró. Inicia sesión nuevamente", sessionClosed = true), result)
    }

    /* USER STORY 40 - Cambio de contraseña: la política la valida la API y su mensaje se muestra */
    @Test
    fun testCambioDeContrasena() = runBlocking {
        val api = FakeAccessApi(AccessActivity(true, emptyList(), true))
        val repository = AccessActivityRepository { api }

        val ok = repository.changePassword("Actual-123", "Nueva-456")
        api.passwordError = """{"error":"La contraseña actual no es correcta"}"""
        val wrong = repository.changePassword("otra", "Nueva-789")

        assertEquals(AccessActivityResult.Success(Unit), ok)
        assertEquals(ChangePasswordRequest("otra", "Nueva-789"), api.lastPassword)
        assertEquals(AccessActivityResult.Error("La contraseña actual no es correcta"), wrong)
    }

    /* USER STORY 40 - Plataformas del contrato, incluidas api y unknown */
    @Test
    fun testPlataformas() {
        assertEquals(R.string.access_platform_mobile, AccessActivityFormat.platformLabel("mobile"))
        assertEquals(R.string.access_platform_web, AccessActivityFormat.platformLabel("WEB"))
        assertEquals(R.string.access_platform_api, AccessActivityFormat.platformLabel("api"))
        assertEquals(R.string.access_platform_unknown, AccessActivityFormat.platformLabel("unknown"))
        assertEquals("sin-fecha", AccessActivityFormat.localTime("sin-fecha"))
    }

    private class FakeAccessApi(
        val activity: AccessActivity? = null,
        val ownIds: Set<Int> = emptySet(),
        val failure: HttpException? = null
    ) : AccessActivityApi {
        val reported = mutableListOf<Int>()
        var lastPassword: ChangePasswordRequest? = null
        var passwordError: String? = null

        override suspend fun get(): AccessActivity {
            failure?.let { throw it }
            return activity!!
        }

        override suspend fun report(accessId: Int): Response<Unit> {
            reported += accessId
            return if (accessId in ownIds) Response.success(204, Unit)
            else Response.error(404, """{"error":"No encontramos ese acceso en tu cuenta"}""".toResponseBody(JSON))
        }

        override suspend fun changePassword(request: ChangePasswordRequest): Response<Unit> {
            lastPassword = request
            return passwordError?.let { Response.error(400, it.toResponseBody(JSON)) } ?: Response.success(204, Unit)
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
