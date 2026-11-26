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

    private val authInterceptor = AuthInterceptor { tokenProvider() }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor) }
        .build()

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
