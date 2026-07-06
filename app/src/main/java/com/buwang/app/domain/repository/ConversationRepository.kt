package com.buwang.app.domain.repository

import com.buwang.app.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getConversationsByPersona(personaId: String): Flow<List<Conversation>>
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(conversationId: String): Conversation?
    suspend fun createConversation(conversation: Conversation)
    suspend fun updateConversation(conversation: Conversation)
    suspend fun deleteConversation(conversationId: String)
}
