package com.buwang.app.domain.model

/**
 * 角色领域模型 — 用于 UI 层和业务逻辑层
 *
 * sourceType 取值: "MANUAL" | "MBTI_IMPORT" | "CHAT_IMPORT" | "ST_IMPORT"
 */
data class Persona(
    val id: String = "",
    val name: String = "",
    val avatarPath: String? = null,
    val description: String = "",
    val sourceType: String = "MANUAL",
    val sourceMbtiType: String? = null,
    /** JSON 格式的 BigFive + 语言风格 + 行为模式参数 */
    val personalityParams: String = "{}",
    val systemPromptTemplate: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
