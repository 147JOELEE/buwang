package com.buwang.app.domain.repository

import com.buwang.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, content: String): Message
    suspend fun insertMessage(message: Message)
    suspend fun deleteMessagesByConversation(conversationId: String)
}
