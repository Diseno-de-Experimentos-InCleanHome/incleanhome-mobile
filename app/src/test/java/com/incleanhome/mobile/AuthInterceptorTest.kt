package com.incleanhome.mobile

import com.incleanhome.mobile.core.network.AuthInterceptor
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInterceptorTest {

    /* USER STORY 40 - Las rutas de actividad y contraseña usan la sesión; el inicio de sesión no */
    @Test
    fun testToken_SoloEnRutasConSesion() {
        val interceptor = AuthInterceptor({ "token-a" })

        assertEquals("Bearer token-a", authorizationSent(interceptor, "/api/auth/access-activity"))
        assertEquals("Bearer token-a", authorizationSent(interceptor, "/api/auth/access-activity/7/report"))
        assertEquals("Bearer token-a", authorizationSent(interceptor, "/api/auth/password"))
        assertEquals("Bearer token-a", authorizationSent(interceptor, "/api/workers/suggestions"))
        assertNull(authorizationSent(interceptor, "/api/auth/login"))
        assertNull(authorizationSent(interceptor, "/api/auth/2fa/verify"))
    }

    /* USER STORY 40 - Una sesión cerrada (401) informa el token rechazado */
    @Test
    fun testSesionRechazada_InformaElToken() {
        val rejected = mutableListOf<String>()
        val interceptor = AuthInterceptor({ "token-a" }, { rejected += it })

        FakeChain("/api/auth/access-activity", code = 401).let(interceptor::intercept)
        FakeChain("/api/auth/login", code = 401).let(interceptor::intercept)

        assertEquals(listOf("token-a"), rejected)
    }

    /* USER STORY 40 - Un 401 de un token anterior no cierra una sesión nueva */
    @Test
    fun testSesionRechazada_IgnoraTokenAnterior() {
        var current = "token-a"
        val rejected = mutableListOf<String>()
        val interceptor = AuthInterceptor({ current }, { rejected += it })

        // The user signs in again while the request with the old token is in flight.
        FakeChain("/api/bookings", code = 401) { current = "token-b" }.let(interceptor::intercept)

        assertTrue(rejected.isEmpty())
    }

    private fun authorizationSent(interceptor: AuthInterceptor, path: String): String? =
        FakeChain(path).also(interceptor::intercept).sent!!.header("Authorization")

    private class FakeChain(
        path: String,
        private val code: Int = 200,
        private val onProceed: () -> Unit = {}
    ) : Interceptor.Chain {
        private val original = Request.Builder().url("http://10.0.2.2:5000$path").build()
        var sent: Request? = null

        override fun request(): Request = original

        override fun proceed(request: Request): Response {
            sent = request
            onProceed()
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .body("{}".toResponseBody(null))
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = error("not used")
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
