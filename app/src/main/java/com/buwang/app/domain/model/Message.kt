package com.buwang.app.domain.model

/**
 * 消息领域模型
 *
 * role 取值: "user" | "assistant" | "system"
 */
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val role: String = "user",
    val content: String = "",
    val timestamp: Long = 0L,
    /** JSON 格式元数据: model_name, token_count, latency_ms */
    val metadata: String? = null
)
