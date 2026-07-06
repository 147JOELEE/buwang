package com.buwang.app.domain.model

/**
 * 用户设置领域模型
 *
 * apiKeyMode 取值: "builtin" | "custom"
 */
data class UserSettings(
    val userAvatarPath: String? = null,
    val userPersonaDesc: String? = null,
    val chatBackground: String? = null,
    val selectedModel: String = "tokens-box",
    val apiKeyMode: String = "builtin",
    val hasCustomApiKey: Boolean = false
)
