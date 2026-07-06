package com.buwang.app.presentation.persona

import androidx.lifecycle.ViewModel
import com.buwang.app.core.personality.BigFiveParams
import com.buwang.app.core.personality.ChatAnalysisAnalyzer
import com.buwang.app.core.personality.MbtiMapper
import com.buwang.app.core.personality.PresetCharacters
import com.buwang.app.core.personality.SystemPromptGenerator
import com.buwang.app.domain.model.Persona
import com.buwang.app.domain.model.PersonaSourceType
import com.buwang.app.domain.repository.ChatRepository
import com.buwang.app.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 创建角色 ViewModel
 *
 * 统一封装 5 种角色创建来源（预设 / 手动 / MBTI / 聊天文本 / ST 角色卡），
 * 全部经由人格引擎（[SystemPromptGenerator] + [MbtiMapper] + [ChatAnalysisAnalyzer]）
 * 生成 BigFive 参数与 System Prompt，并落库。
 *
 * 每个创建方法均为 suspend，返回新建角色的 **真实 ID**，由 UI 层在协程中调用后
 * 通过 onCreated(id) 进入对话（修复此前"用角色名当作 ID"的导航缺陷）。
 */
@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val personaRepository: PersonaRepository,
    private val chatRepository: ChatRepository,
    private val systemPromptGenerator: SystemPromptGenerator,
    private val chatAnalysisAnalyzer: ChatAnalysisAnalyzer
) : ViewModel() {

    /** 从预设角色创建（每次生成新 UUID，允许重复添加） */
    suspend fun createFromPreset(presetId: String): String {
        val preset = PresetCharacters.getById(presetId) ?: PresetCharacters.getDefault()
        return personaRepository.createPersona(
            Persona(
                id = "",  // 由仓库生成 UUID
                name = preset.name,
                description = preset.description,
                sourceType = PersonaSourceType.PRESET,
                personalityParams = preset.params.toJson(),
                systemPromptTemplate = preset.systemPromptTemplate
            )
        )
    }

    /** 手动创建（名称 + 描述，使用均衡默认人格） */
    suspend fun createFromManual(name: String, description: String): String =
        buildAndSave(
            name = name,
            description = description,
            params = BigFiveParams.default(),
            sourceType = PersonaSourceType.MANUAL,
            sourceMbtiType = null
        )

    /** MBTI 导入：类型 → BigFive → System Prompt */
    suspend fun createFromMbti(name: String, mbti: String): String {
        val normalized = mbti.trim().uppercase()
        val params = BigFiveParams.fromMbti(normalized)
        val cnName = MbtiMapper.mbtiNameCn(normalized)
        val desc = if (!cnName.isNullOrBlank()) {
            "$name（MBTI $normalized · $cnName）"
        } else {
            "$name（MBTI $normalized）"
        }
        return buildAndSave(
            name = name,
            description = desc,
            params = params,
            sourceType = PersonaSourceType.MBTI_IMPORT,
            sourceMbtiType = normalized
        )
    }

    /**
     * 聊天文本导入：调用 LLM 分析用户发言 → 解析 BigFive → 生成角色。
     * 分析失败（网络/解析异常）时降级为"以聊天文本作描述"的均衡人格，保证流程可用。
     */
    suspend fun createFromChat(name: String, chatText: String): String {
        return try {
            val analysisPrompt = chatAnalysisAnalyzer.buildAnalysisPrompt(chatText)
            val raw = chatRepository.analyzeText(ANALYSIS_SYSTEM, analysisPrompt)
            val result = chatAnalysisAnalyzer.parseAnalysisResult(raw)
            val params = result.bigFive.toBigFiveParams()
            val description = result.summary.ifBlank { "由聊天文本分析生成的虚拟角色" }
            buildAndSave(
                name = name.ifBlank { "聊天中的TA" },
                description = description,
                params = params,
                sourceType = PersonaSourceType.CHAT_IMPORT,
                sourceMbtiType = null
            )
        } catch (e: Exception) {
            buildAndSave(
                name = name.ifBlank { "聊天中的TA" },
                description = chatText.take(200).ifBlank { "由聊天记录生成的虚拟角色" },
                params = BigFiveParams.default(),
                sourceType = PersonaSourceType.CHAT_IMPORT,
                sourceMbtiType = null
            )
        }
    }

    /** ST 角色卡导入：解析常见中文字段 → 描述 → 均衡人格 */
    suspend fun createFromStCard(text: String): String {
        val (name, description) = parseStCard(text)
        return buildAndSave(
            name = name,
            description = description,
            params = BigFiveParams.default(),
            sourceType = PersonaSourceType.ST_IMPORT,
            sourceMbtiType = null
        )
    }

    // ============================================================
    // 内部工具
    // ============================================================

    /** 生成 System Prompt 并落库，返回新建角色 ID。 */
    private suspend fun buildAndSave(
        name: String,
        description: String,
        params: BigFiveParams,
        sourceType: String,
        sourceMbtiType: String?
    ): String {
        val safeName = name.ifBlank { "自定义角色" }
        val safeDesc = description.ifBlank { "$safeName，一个由用户创建的虚拟陪伴角色。" }
        val systemPrompt = systemPromptGenerator.generateSystemPrompt(safeName, params, safeDesc)
        return personaRepository.createPersona(
            Persona(
                name = safeName,
                description = safeDesc,
                sourceType = sourceType,
                sourceMbtiType = sourceMbtiType,
                personalityParams = params.toJson(),
                systemPromptTemplate = systemPrompt
            )
        )
    }

    /**
     * 解析 ST 角色卡文本。
     * 支持以 `：`/`:`/`=` 分隔的键值行，常见键：姓名/性格/外貌/背景/简介 等。
     */
    private fun parseStCard(text: String): Pair<String, String> {
        var name = ""
        val descParts = mutableListOf<String>()
        for (line in text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }) {
            val (key, value) = splitKeyValue(line) ?: continue
            when {
                NAME_KEYS.containsMatchIn(key) && name.isBlank() -> name = value
                TRAIT_KEYS.containsMatchIn(key) -> descParts.add("性格：$value")
                APPEARANCE_KEYS.containsMatchIn(key) -> descParts.add("外貌：$value")
                BACKGROUND_KEYS.containsMatchIn(key) -> descParts.add("背景：$value")
                else -> descParts.add(value)
            }
        }
        val description = descParts.joinToString("；").ifBlank { text.take(200) }
        return (name.ifBlank { "自定义角色" }) to description
    }

    private fun splitKeyValue(line: String): Pair<String, String>? {
        val idx = line.indexOfFirst { it == '：' || it == ':' || it == '=' }
        if (idx <= 0) return null
        val key = line.substring(0, idx).trim()
        val value = line.substring(idx + 1).trim()
        if (value.isEmpty()) return null
        return key to value
    }

    companion object {
        /** 聊天文本分析时注入 LLM 的系统角色 */
        private const val ANALYSIS_SYSTEM =
            "你是一个人格分析助手。用户会给你一段分析要求（含聊天文本与输出 JSON 格式约定），" +
                "请严格按该格式仅输出 JSON，不要添加任何额外文字或解释。"

        private val NAME_KEYS = Regex("姓名|名字|名称|昵称|代号")
        private val TRAIT_KEYS = Regex("性格|人格|特质|脾气")
        private val APPEARANCE_KEYS = Regex("外貌|形象|长相|外表|样貌")
        private val BACKGROUND_KEYS = Regex("背景|设定|经历|故事|身世|身份")
    }
}
