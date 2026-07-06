package com.buwang.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户设置表 — 单例模式，始终只有一条记录 id=1
 */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "user_avatar_path")
    val userAvatarPath: String?,

    /**
     * 用户自我描述，用于帮助 AI 理解用户
     */
    @ColumnInfo(name = "user_persona_desc")
    val userPersonaDesc: String?,

    @ColumnInfo(name = "chat_background")
    val chatBackground: String?,

    @ColumnInfo(name = "selected_model")
    val selectedModel: String,

    /**
     * API Key 模式: "free"(免费/内置), "custom"(用户自定义)
     */
    @ColumnInfo(name = "api_key_mode")
    val apiKeyMode: String,

    /**
     * 用户自定义的 API Key（加密存储）
     */
    @ColumnInfo(name = "custom_api_key_encrypted")
    val customApiKeyEncrypted: String?
)
