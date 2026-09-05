package com.incleanhome.mobile.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (
            request.header(AUTHORIZATION_HEADER) != null ||
            request.url.encodedPath.startsWith(AUTH_PATH_PREFIX)
        ) {
            return chain.proceed(request)
        }

        val token = tokenProvider()?.takeIf(String::isNotBlank)
            ?: return chain.proceed(request)
        val authenticatedRequest = request.newBuilder()
            .header(AUTHORIZATION_HEADER, "Bearer $token")
            .build()
        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val AUTH_PATH_PREFIX = "/api/auth/"
    }
}
