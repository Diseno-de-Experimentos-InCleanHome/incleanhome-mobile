package com.incleanhome.mobile.reviews.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReviewsApi {
    @POST("reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): Review

    @GET("reviews/worker/{id}")
    suspend fun getWorkerReviews(@Path("id") workerId: Int): List<Review>
}
