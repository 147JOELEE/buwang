package com.buwang.app.domain.repository

import com.buwang.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    suspend fun createConversation(personaId: String, title: String = "新对话"): String
    suspend fun deleteConversation(conversationId: String)
    suspend fun sendMessage(
        modelKey: String,
        apiKey: String,
        systemPrompt: String,
        conversationId: String,
        userMessage: String,
        historyMessages: List<Message> = emptyList()
    ): Flow<String>
    suspend fun sendMessageNonStreaming(
        modelKey: String,
        apiKey: String,
        systemPrompt: String,
        conversationId: String,
        userMessage: String,
        historyMessages: List<Message> = emptyList()
    ): String
    suspend fun deleteMessage(messageId: String)
    suspend fun clearConversationMessages(conversationId: String)

    /**
     * 通用一次性分析调用（不落库、不关联会话）。
     *
     * 用于"聊天文本导入"等场景：把分析类 Prompt 直接发给 LLM 并返回纯文本结果。
     * 模型与鉴权 Key 取自当前用户设置（内置回退 / 自定义解密）。
     *
     * @param systemPrompt 系统角色设定（如"你是一个人格分析助手"）
     * @param userText 用户侧输入（如分析 Prompt + 聊天文本）
     * @return LLM 返回的文本内容（已剥离推理标签）
     */
    suspend fun analyzeText(systemPrompt: String, userText: String): String
}
