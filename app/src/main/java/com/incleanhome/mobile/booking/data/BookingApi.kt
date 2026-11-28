package com.incleanhome.mobile.booking.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface BookingApi {
    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Booking

    /** US41: client-only estimate with the rules the API applies when registering the booking. */
    @POST("bookings/estimate")
    suspend fun estimate(@Body request: BookingEstimateRequest): BookingEstimateResponse

    @GET("bookings")
    suspend fun getMyBookings(): List<Booking>

    @PATCH("bookings/{id}/status")
    suspend fun updateStatus(
        @Path("id") bookingId: Int,
        @Body request: UpdateBookingStatusRequest
    ): Booking
}
