package com.incleanhome.mobile

import com.incleanhome.mobile.messaging.data.Conversation
import com.incleanhome.mobile.messaging.data.Message
import com.incleanhome.mobile.messaging.data.MessagingApi
import com.incleanhome.mobile.messaging.data.MessagingRepository
import com.incleanhome.mobile.messaging.data.MessagingResult
import com.incleanhome.mobile.messaging.data.SendMessageRequest
import com.incleanhome.mobile.messaging.presentation.ChatViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingRepositoryTest {

    /* USER STORY 14 - Enviar mensaje */
    @Test
    fun testSendMessage_EnviaContenidoAlUsuarioCorrecto() = runBlocking {
        //Arrange
        val api = FakeMessagingApi()
        val repository = MessagingRepository(api)
        api.message = Message(9, 1, 25, "Hola", null, null)

        //Act
        val resultado = repository.sendMessage(25, "Hola")

        //Assert
        assertEquals(25, api.lastUserId)
        assertEquals(SendMessageRequest("Hola"), api.lastRequest)
        assertTrue(resultado is MessagingResult.Success)
    }

    /* USER STORY 14 - Consultar conversación */
    @Test
    fun testGetThread_DevuelveHistorialDelContacto() = runBlocking {
        //Arrange
        val api = FakeMessagingApi()
        api.thread = listOf(
            Message(1, 1, 25, "Hola", "2026-10-03T10:00:00Z", null),
            Message(2, 25, 1, "Hola, ¿cómo estás?", "2026-10-03T10:01:00Z", null)
        )
        val repository = MessagingRepository(api)

        //Act
        val resultado = repository.getThread(25)

        //Assert
        assertEquals(25, api.lastUserId)
        assertTrue(resultado is MessagingResult.Success)
        resultado as MessagingResult.Success
        assertEquals(2, resultado.data.size)
    }

    /* USER STORY 14 - Límite del mensaje */
    @Test
    fun testMessage_MaximoPermitidoEsCuatroMilCaracteres() {
        //Arrange
        val esperado = 4000

        //Act
        val resultado = ChatViewModel.MAX_CONTENT_LENGTH

        //Assert
        assertEquals(esperado, resultado)
    }

    private class FakeMessagingApi : MessagingApi {
        var thread: List<Message> = emptyList()
        var message = Message(1, 1, 2, "", null, null)
        var lastUserId: Int? = null
        var lastRequest: SendMessageRequest? = null

        override suspend fun getConversations(): List<Conversation> = emptyList()

        override suspend fun getThread(userId: Int): List<Message> {
            lastUserId = userId
            return thread
        }

        override suspend fun sendMessage(userId: Int, request: SendMessageRequest): Message {
            lastUserId = userId
            lastRequest = request
            return message
        }
    }
}
