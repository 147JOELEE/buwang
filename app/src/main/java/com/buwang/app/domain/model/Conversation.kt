package com.buwang.app.domain.model

/**
 * 会话领域模型
 */
data class Conversation(
    val id: String,
    val personaId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val lastMessage: Message? = null
)
