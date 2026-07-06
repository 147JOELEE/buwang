package com.buwang.app.domain.model

/**
 * 角色来源类型常量
 *
 * 与 [com.buwang.app.data.local.entity.PersonaEntity.sourceType] /
 * [Persona.sourceType] 一一对应，作为单一数据源避免散落的魔法字符串。
 */
object PersonaSourceType {
    /** 预设角色（小暖等内置角色） */
    const val PRESET = "PRESET"

    /** 手动创建（名称 + 描述） */
    const val MANUAL = "MANUAL"

    /** MBTI 类型导入 */
    const val MBTI_IMPORT = "MBTI_IMPORT"

    /** 聊天文本分析导入 */
    const val CHAT_IMPORT = "CHAT_IMPORT"

    /** ST 角色卡导入 */
    const val ST_IMPORT = "ST_IMPORT"
}
