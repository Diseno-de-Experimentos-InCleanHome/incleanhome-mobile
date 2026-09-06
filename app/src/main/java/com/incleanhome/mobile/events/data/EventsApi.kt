package com.incleanhome.mobile.events.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EventsApi {
    @POST("events") suspend fun create(@Body request: CreateEventRequest): Event
    @GET("events") suspend fun searchOpen(
        @Query("serviceType") serviceType: String?, @Query("zone") zone: String?,
        @Query("date") date: String?
    ): List<Event>
    @GET("events/mine") suspend fun getMyEvents(): List<Event>
    @GET("events/mine") suspend fun getMyApplications(): List<EventApplication>
    @GET("events/{id}") suspend fun getEvent(@Path("id") id: Int): Event
    @PATCH("events/{id}/cancel") suspend fun cancel(@Path("id") id: Int): Event
    @PATCH("events/{id}/complete") suspend fun complete(@Path("id") id: Int): Event
    @POST("events/{id}/applications") suspend fun apply(
        @Path("id") id: Int, @Body request: ApplyToEventRequest
    ): EventApplication
    @GET("events/{id}/applications") suspend fun getApplications(
        @Path("id") id: Int
    ): List<EventApplication>
    @PATCH("events/{eventId}/applications/{appId}/accept") suspend fun accept(
        @Path("eventId") eventId: Int, @Path("appId") appId: Int
    ): EventApplication
    @PATCH("events/{eventId}/applications/{appId}/reject") suspend fun reject(
        @Path("eventId") eventId: Int, @Path("appId") appId: Int
    ): EventApplication
    @PATCH("events/{eventId}/applications/{appId}/withdraw") suspend fun withdraw(
        @Path("eventId") eventId: Int, @Path("appId") appId: Int
    ): EventApplication
}
