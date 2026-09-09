package com.incleanhome.mobile.claims.data

import com.incleanhome.mobile.core.network.RetrofitClient
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

enum class ClaimFailure {
    INVALID_DATA,
    NOT_FOUND,
    NETWORK,
    SERVER,
    INVALID_RESPONSE,
    UNKNOWN
}

sealed interface ClaimResult<out T> {
    data class Success<T>(val data: T) : ClaimResult<T>
    data class Error(val failure: ClaimFailure) : ClaimResult<Nothing>
}

interface ClaimsDataSource {
    suspend fun create(request: CreateClaimRequest): ClaimResult<ClaimCreatedResponse>
    suspend fun track(code: String): ClaimResult<ClaimTrackResponse>
}

class ClaimsRepository(
    private val api: ClaimsApi = RetrofitClient.retrofit.create(ClaimsApi::class.java)
) : ClaimsDataSource {
    override suspend fun create(request: CreateClaimRequest): ClaimResult<ClaimCreatedResponse> =
        execute(validate = { it.code.isNotBlank() }) { api.create(request) }

    override suspend fun track(code: String): ClaimResult<ClaimTrackResponse> =
        execute(validate = { it.code.isNotBlank() }) { api.track(code) }

    private suspend fun <T> execute(
        validate: (T) -> Boolean,
        call: suspend () -> T
    ): ClaimResult<T> = try {
        val response = call()
        if (validate(response)) ClaimResult.Success(response)
        else ClaimResult.Error(ClaimFailure.INVALID_RESPONSE)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: HttpException) {
        ClaimResult.Error(
            when (exception.code()) {
                400 -> ClaimFailure.INVALID_DATA
                404 -> ClaimFailure.NOT_FOUND
                in 500..599 -> ClaimFailure.SERVER
                else -> ClaimFailure.UNKNOWN
            }
        )
    } catch (exception: IOException) {
        ClaimResult.Error(ClaimFailure.NETWORK)
    } catch (exception: Exception) {
        ClaimResult.Error(ClaimFailure.UNKNOWN)
    }
}
