package com.buwang.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 统一 LLM 请求 DTO — 适配 OpenAI 兼容格式
 */
@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessage>,
    @Json(name = "temperature") val temperature: Double = 0.8,
    @Json(name = "max_tokens") val maxTokens: Int = 2048,
    @Json(name = "top_p") val topP: Double = 0.9,
    @Json(name = "frequency_penalty") val frequencyPenalty: Double = 0.3,
    @Json(name = "presence_penalty") val presencePenalty: Double = 0.3,
    @Json(name = "stream") val stream: Boolean = false
)

/**
 * 单条消息 DTO
 */
@JsonClass(generateAdapter = true)
data class ChatMessage(
    @Json(name = "role") val role: String,      // system / user / assistant
    @Json(name = "content") val content: String
)

/**
 * 非流式响应 DTO
 */
@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "id") val id: String,
    @Json(name = "object") val obj: String,
    @Json(name = "created") val created: Long,
    @Json(name = "model") val model: String,
    @Json(name = "choices") val choices: List<ChatChoice>,
    @Json(name = "usage") val usage: TokenUsage?
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    @Json(name = "index") val index: Int,
    @Json(name = "message") val message: ChatMessage,
    @Json(name = "finish_reason") val finishReason: String?
)

@JsonClass(generateAdapter = true)
data class TokenUsage(
    @Json(name = "prompt_tokens") val promptTokens: Int,
    @Json(name = "completion_tokens") val completionTokens: Int,
    @Json(name = "total_tokens") val totalTokens: Int
)

/**
 * SSE 流式响应块 DTO
 */
@JsonClass(generateAdapter = true)
data class ChatStreamChunk(
    @Json(name = "id") val id: String,
    @Json(name = "object") val obj: String,
    @Json(name = "created") val created: Long,
    @Json(name = "model") val model: String,
    @Json(name = "choices") val choices: List<StreamChoice>
)

@JsonClass(generateAdapter = true)
data class StreamChoice(
    @Json(name = "index") val index: Int,
    @Json(name = "delta") val delta: StreamDelta,
    @Json(name = "finish_reason") val finishReason: String?
)

@JsonClass(generateAdapter = true)
data class StreamDelta(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String? = null
)

/**
 * API Key 验证测试请求
 */
@JsonClass(generateAdapter = true)
data class TestApiRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessage>,
    @Json(name = "max_tokens") val maxTokens: Int = 5,
    @Json(name = "stream") val stream: Boolean = false
)

/**
 * 可用模型列表响应
 */
@JsonClass(generateAdapter = true)
data class ModelListResponse(
    @Json(name = "object") val obj: String,
    @Json(name = "data") val data: List<ModelInfo>
)

@JsonClass(generateAdapter = true)
data class ModelInfo(
    @Json(name = "id") val id: String,
    @Json(name = "object") val obj: String? = null,
    @Json(name = "created") val created: Long? = null,
    @Json(name = "owned_by") val ownedBy: String? = null
)
