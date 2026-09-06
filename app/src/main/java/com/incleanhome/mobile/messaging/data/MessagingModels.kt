package com.incleanhome.mobile.messaging.data

data class SendMessageRequest(val content: String)

data class Message(
    val id: Int,
    val senderId: Int,
    val recipientId: Int,
    val content: String,
    val createdAt: String?,
    val readAt: String?
)

data class Conversation(
    val userId: Int,
    val userName: String,
    val lastMessage: String,
    val lastMessageAt: String?,
    val unreadCount: Int
)
