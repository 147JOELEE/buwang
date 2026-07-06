package com.buwang.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 角色表 — 存储虚拟人格的所有配置信息
 */
@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "avatar_path")
    val avatarPath: String?,

    @ColumnInfo(name = "description")
    val description: String,

    /**
     * 来源类型: "manual"(手动创建), "mbti"(MBTI生成), "custom"(自定义模板)
     */
    @ColumnInfo(name = "source_type")
    val sourceType: String,

    /**
     * 当 sourceType 为 "mbti" 时，记录对应的 MBTI 类型（如 "INTJ", "ENFP"）
     */
    @ColumnInfo(name = "source_mbti_type")
    val sourceMbtiType: String?,

    /**
     * 人格参数 JSON 字符串，包含模型生成所需的所有人格参数
     * 如: {"openness": 0.8, "conscientiousness": 0.6, ...}
     */
    @ColumnInfo(name = "personality_params")
    val personalityParams: String,

    /**
     * 系统提示词模板，用于初始化 LLM 对话
     */
    @ColumnInfo(name = "system_prompt_template")
    val systemPromptTemplate: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
