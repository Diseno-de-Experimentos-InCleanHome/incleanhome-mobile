package com.incleanhome.mobile.core.network

import com.incleanhome.mobile.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    @Volatile
    private var tokenProvider: () -> String? = { null }

    @Volatile
    private var sessionRejectedHandler: (String) -> Unit = {}

    private val authInterceptor = AuthInterceptor({ tokenProvider() }, { token -> sessionRejectedHandler(token) })

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor) }
        .build()

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    /** US40: a closed session (401) returns the app to the sign-in screen. Runs on an OkHttp thread. */
    fun setSessionRejectedHandler(handler: (rejectedToken: String) -> Unit) {
        sessionRejectedHandler = handler
    }

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
