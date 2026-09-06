package com.incleanhome.mobile.messaging.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MessagingApi {
    @GET("messages/conversations")
    suspend fun getConversations(): List<Conversation>

    @GET("messages/{userId}")
    suspend fun getThread(@Path("userId") userId: Int): List<Message>

    @POST("messages/{userId}")
    suspend fun sendMessage(
        @Path("userId") userId: Int,
        @Body request: SendMessageRequest
    ): Message
}
