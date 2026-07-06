package com.buwang.app.data.repository

import com.buwang.app.data.local.dao.ConversationDao
import com.buwang.app.data.local.dao.MessageDao
import com.buwang.app.data.local.entity.ConversationEntity
import com.buwang.app.data.local.entity.MessageEntity
import com.buwang.app.data.remote.api.LlmApiFactory
import com.buwang.app.data.remote.api.SseParser
import com.buwang.app.data.remote.api.SseResult
import com.buwang.app.data.remote.api.toContentFlow
import com.buwang.app.data.remote.dto.ChatMessage
import com.buwang.app.data.remote.dto.ChatRequest
import com.buwang.app.data.remote.interceptor.ApiException
import com.buwang.app.domain.model.Conversation
import com.buwang.app.domain.model.Message
import com.buwang.app.domain.repository.ChatRepository
import com.buwang.app.domain.repository.UserSettingsRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val llmApiFactory: LlmApiFactory,
    private val userSettingsRepository: UserSettingsRepository,
    moshi: Moshi
) : ChatRepository {

    private val sseParser = SseParser(moshi)

    override fun getConversationsForPersona(personaId: String): Flow<List<Conversation>> {
        return conversationDao.getConversationsForPersona(personaId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun createConversation(personaId: String, title: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val entity = ConversationEntity(
            id = id,
            personaId = personaId,
            title = title,
            createdAt = now,
            updatedAt = now
        )
        conversationDao.insert(entity)
        return id
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteById(conversationId)
    }

    override suspend fun sendMessage(
        modelKey: String,
        apiKey: String,
        systemPrompt: String,
        conversationId: String,
        userMessage: String,
        historyMessages: List<Message>
    ): Flow<String> = flow {
        val now = System.currentTimeMillis()

        // 1. 保存用户消息
        val userMsgId = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = userMsgId,
                conversationId = conversationId,
                role = "user",
                content = userMessage,
                timestamp = now
            )
        )

        // 2. 构建消息列表
        val messages = buildList {
            add(ChatMessage("system", systemPrompt))
            historyMessages.forEach { msg ->
                add(ChatMessage(msg.role, msg.content))
            }
            add(ChatMessage("user", userMessage))
        }

        // 3. 构建请求
        val config = llmApiFactory.getModelConfig(modelKey)
        val request = ChatRequest(
            model = config?.modelName ?: "deepseek-chat",
            messages = messages,
            temperature = 0.8,
            maxTokens = 2048,
            stream = true
        )

        // 4. 调用 API 流式对话
        val api = llmApiFactory.create(modelKey, apiKey)

        try {
            val response = api.chatCompletionStream(request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "未知错误"
                throw ApiException.Unknown("API 请求失败 (${response.code()}): $errorBody")
            }

            val body = response.body()
                ?: throw ApiException.Unknown("响应体为空")

            // 5. 收集流式内容
            val fullContent = StringBuilder()
            val assistantMsgId = UUID.randomUUID().toString()

            // 先保存一个占位消息
            messageDao.insert(
                MessageEntity(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    role = "assistant",
                    content = "",
                    timestamp = System.currentTimeMillis()
                )
            )

            // 逐字发送内容（过滤推理模型的 <think>...</think> 标签，仅输出干净文本）
            var displayedSoFar = ""
            sseParser.parse(body).toContentFlow().collect { token ->
                fullContent.append(token)
                val clean = stripThinkBlocks(fullContent.toString())
                val delta = clean.removePrefix(displayedSoFar)
                if (delta.isNotEmpty()) {
                    displayedSoFar = clean
                    emit(delta)
                }
            }

            // 6. 更新完整消息内容
            messageDao.updateContent(assistantMsgId, fullContent.toString())

            // 7. 更新会话时间
            conversationDao.updateTimestamp(conversationId, System.currentTimeMillis())

        } catch (e: ApiException) {
            // 保存错误消息
            messageDao.insert(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "system",
                    content = "错误: ${e.userMessage}",
                    timestamp = System.currentTimeMillis(),
                    metadata = """{"error": "${e.javaClass.simpleName}"}"""
                )
            )
            throw e
        }
    }

    override suspend fun sendMessageNonStreaming(
        modelKey: String,
        apiKey: String,
        systemPrompt: String,
        conversationId: String,
        userMessage: String,
        historyMessages: List<Message>
    ): String {
        val now = System.currentTimeMillis()

        // 保存用户消息
        val userMsgId = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = userMsgId,
                conversationId = conversationId,
                role = "user",
                content = userMessage,
                timestamp = now
            )
        )

        val messages = buildList {
            add(ChatMessage("system", systemPrompt))
            historyMessages.forEach { msg ->
                add(ChatMessage(msg.role, msg.content))
            }
            add(ChatMessage("user", userMessage))
        }

        val config = llmApiFactory.getModelConfig(modelKey)
        val request = ChatRequest(
            model = config?.modelName ?: "deepseek-chat",
            messages = messages,
            temperature = 0.8,
            maxTokens = 2048,
            stream = false
        )

        val api = llmApiFactory.create(modelKey, apiKey)
        val response = api.chatCompletion(request)

        if (!response.isSuccessful) {
            throw ApiException.Unknown("API 请求失败 (${response.code()})")
        }

        val content = response.body()?.choices?.firstOrNull()?.message?.content
            ?.let { stripThinkBlocks(it) }
            ?: throw ApiException.Unknown("响应内容为空")

        // 保存助手消息
        val assistantMsgId = UUID.randomUUID().toString()
        messageDao.insert(
            MessageEntity(
                id = assistantMsgId,
                conversationId = conversationId,
                role = "assistant",
                content = content,
                timestamp = System.currentTimeMillis(),
                metadata = response.body()?.usage?.let { usage ->
                    """{"model": "${response.body()?.model}", "prompt_tokens": ${usage.promptTokens}, "completion_tokens": ${usage.completionTokens}}"""
                }
            )
        )

        conversationDao.updateTimestamp(conversationId, System.currentTimeMillis())
        return content
    }

    override suspend fun deleteMessage(messageId: String) {
        messageDao.deleteById(messageId)
    }

    override suspend fun clearConversationMessages(conversationId: String) {
        messageDao.deleteByConversationId(conversationId)
    }

    override suspend fun analyzeText(systemPrompt: String, userText: String): String {
        val settings = userSettingsRepository.getSettingsOnce()
        val modelKey = settings?.selectedModel ?: LlmApiFactory.DEFAULT_MODEL
        val apiKey = userSettingsRepository.getEffectiveApiKey()
        val config = llmApiFactory.getModelConfig(modelKey)
        val request = ChatRequest(
            model = config?.modelName ?: "default",
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userText)
            ),
            temperature = 0.7,
            maxTokens = 1024,
            stream = false
        )
        val api = llmApiFactory.create(modelKey, apiKey)
        val response = api.chatCompletion(request)
        if (!response.isSuccessful) {
            throw ApiException.Unknown("人格分析请求失败 (${response.code()})")
        }
        return response.body()?.choices?.firstOrNull()?.message?.content
            ?.let { stripThinkBlocks(it) }
            ?: throw ApiException.Unknown("分析结果为空")
    }
}

private fun ConversationEntity.toDomainModel(): Conversation {
    return Conversation(
        id = id,
        personaId = personaId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * 移除推理模型（如 MiniMax-M2.7）输出的 <think>...</think> 思考过程区块。
 *
 * 使用 DOTALL 正则，既匹配完整区块，也匹配末尾尚未闭合的 <think> 片段，
 * 因此可安全用于流式逐字累积的缓冲区，避免出现半截标签。
 */
private fun stripThinkBlocks(text: String): String {
    return THINK_BLOCK_REGEX.replace(text, "")
}

private val THINK_BLOCK_REGEX = Regex("<think>.*?(</think>|\$)", RegexOption.DOTALL)

private fun MessageEntity.toDomainModel(): Message {
    return Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        metadata = metadata
    )
}
