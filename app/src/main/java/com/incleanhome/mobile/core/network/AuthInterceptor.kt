package com.incleanhome.mobile.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    /** Called with the rejected session token, for example a session closed by US40. */
    private val onSessionRejected: (String) -> Unit = {}
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (
            request.header(AUTHORIZATION_HEADER) != null ||
            (path.startsWith(AUTH_PATH_PREFIX) && SESSION_AUTH_PATHS.none(path::startsWith))
        ) {
            return chain.proceed(request)
        }

        val token = tokenProvider()?.takeIf(String::isNotBlank)
            ?: return chain.proceed(request)
        val authenticatedRequest = request.newBuilder()
            .header(AUTHORIZATION_HEADER, "Bearer $token")
            .build()
        val response = chain.proceed(authenticatedRequest)
        // Only a 401 for the token still in use ends the local session; a newer login is kept.
        if (response.code == 401 && tokenProvider() == token) onSessionRejected(token)
        return response
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val AUTH_PATH_PREFIX = "/api/auth/"

        /** Auth routes of US40 that act on the open session instead of signing in. */
        val SESSION_AUTH_PATHS = listOf("/api/auth/access-activity", "/api/auth/password")
    }
}
