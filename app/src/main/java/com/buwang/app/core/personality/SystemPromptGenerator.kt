package com.buwang.app.core.personality

import javax.inject.Inject

/**
 * System Prompt 生成器
 *
 * 将 [BigFiveParams] 参数化人格转换为 LLM 可直接使用的 System Prompt。
 * 采用设计文档 system-prompt-design.md 定义的**三层架构**：
 *
 * - L1 核心人格层 (CORE PERSONA)   —— 稳定，永久不变：身份 / 人格描述 / 语言风格 / 核心边界
 * - L2 行为模式层 (BEHAVIORAL)     —— 偶尔刷新：对话策略 / 安全约束 / 当前状态
 * - L3 会话上下文层 (SESSION)      —— 动态：对话背景 / 用户信息
 *
 * 设计要点（见 D-203）：Prompt 中**不暴露 Big Five 数值**，全部翻译为自然语言行为描述，
 * 因为 LLM 不理解数值参数，且自然语言更利于调试与用户阅读。
 */
class SystemPromptGenerator @Inject constructor() {

    // ============================================================
    // 公开 API
    // ============================================================

    /**
     * 生成完整的 System Prompt。
     *
     * @param personaName  角色名称（如 "小暖"）
     * @param params       角色 Big Five 参数（驱动 L1 / L2）
     * @param description  角色定位描述（如 "一个温暖贴心的智能聊天助手……"），用于 L1 身份定义
     * @param userPersona  可选的用户画像文本，作为 L3 会话上下文层内容
     * @param includeCheckpoint 是否在末尾追加"人格一致性检查点"（重注入时设为 true）
     * @return 完整、可直接发送给 LLM 的 System Prompt 文本
     */
    fun generateSystemPrompt(
        personaName: String,
        params: BigFiveParams,
        description: String,
        userPersona: String? = null,
        includeCheckpoint: Boolean = false
    ): String {
        val layers = buildList {
            add(generateCoreLayer(personaName, params, description))
            add(generateBehaviorLayer(params, includeEmotion = false))
            add(generateContextLayer(personaName, userPersona))
        }
        val prompt = layers.joinToString("\n\n---\n\n")
        return if (includeCheckpoint) {
            prompt + "\n\n" + generateConsistencyCheckpoint(personaName)
        } else {
            prompt
        }
    }

    /**
     * 仅生成 L1 核心人格层（用于重注入时单独更新）。
     */
    fun generateCoreLayer(personaName: String, params: BigFiveParams, description: String): String {
        return buildString {
            // 角色身份定义
            append("你是").append(personaName).append("，").append(description.trim()).append("。")
            append("\n\n")

            // 人格特征
            append("## 人格特征\n")
            append(translateOpenness(params.openness)).append("\n")
            append(translateConscientiousness(params.conscientiousness)).append("\n")
            append(translateExtraversion(params.extraversion)).append("\n")
            append(translateAgreeableness(params.agreeableness)).append("\n")
            append(translateNeuroticism(params.neuroticism)).append("\n\n")
            append("综合来看，你").append(comprehensiveDescription(params)).append("。")
            append("\n\n")

            // 语言风格
            append("## 语言风格\n")
            append(generateLanguageStyleInstructions(params))
            append("\n\n")

            // 核心边界
            append("## 核心边界\n")
            append(generateCoreBoundaries(personaName, description))
        }
    }

    /**
     * 生成 L2 行为模式层。
     * @param includeEmotion 是否在末尾追加"当前状态"（情绪基调，建议每 10-20 轮刷新）
     */
    fun generateBehaviorLayer(params: BigFiveParams, includeEmotion: Boolean = false): String {
        val behavior = params.deriveBehaviorPatternParams()
        return buildString {
            append("## 对话策略\n")
            append("- 主动性：").append(lowMidHigh(behavior.initiative,
                "以回应为主，不主动引导话题",
                "适时主动，平衡引导和回应",
                "积极引导话题，主动提问和分享")).append("\n")
            append("- 乐观度：").append(lowMidHigh(behavior.optimism,
                "关注问题和风险，表达谨慎",
                "保持平衡视角",
                "积极乐观，看到事情的光明面")).append("\n")
            append("- 果断度：").append(lowMidHigh(behavior.decisiveness,
                "提供多角度分析，不急于下结论",
                "在分析后给出倾向性建议",
                "直接给出明确判断和建议")).append("\n")
            append("- 好奇心：").append(lowMidHigh(behavior.curiosity,
                "关注已知和确定的话题",
                "适度探索新话题",
                "积极追问和探索未知领域")).append("\n")
            append("- 共情力：").append(lowMidHigh(behavior.empathy,
                "理性分析为主，情感回应简洁",
                "平衡理性与情感回应",
                "深度共情，优先回应情感需求")).append("\n")
            append("- 自我表露：").append(lowMidHigh(behavior.selfDisclosure,
                "不谈论自己，保持神秘感",
                "适度分享个人(虚拟)经历",
                "主动分享个人故事和感受")).append("\n")
            append("- 辩论倾向：").append(lowMidHigh(behavior.argumentativeness,
                "避免争论，寻求共识",
                "温和表达不同意见",
                "享受智力辩论，直接提出质疑"))
            append("\n\n")

            append("## 安全约束\n")
            append(generateSafetyConstraints())

            if (includeEmotion) {
                append("\n\n## 当前状态\n")
                append(generateEmotionState(params))
            }
        }
    }

    /**
     * 生成 L3 会话上下文层。
     * @param userPersona 可选的用户画像/已知信息文本；为空时给出占位说明。
     */
    fun generateContextLayer(personaName: String, userPersona: String?): String {
        return buildString {
            append("## 对话背景\n")
            append("你们正在进行日常对话，请根据对话历史自然延续，保持话题连贯。")
            append("\n\n")
            append("## 用户信息\n")
            if (!userPersona.isNullOrBlank()) {
                append(userPersona.trim())
            } else {
                append("你还不了解对方，请在对话中自然地了解用户的喜好、近况与需求。")
            }
            append("\n\n")
            append("[人格一致性检查]\n")
            append("在回复之前，请确认：1) 你的回复是否符合").append(personaName).append("的性格特点？")
            append("2) 你的语气是否符合").append(personaName).append("的说话风格？")
            append("3) 你是否保持了角色的一致性，没有突然变成通用AI助手？")
            append("如果以上任何一项为\"否\"，请重新调整你的回复。")
        }
    }

    /**
     * 生成"人格一致性检查点"文本（在检测到漂移或重注入时追加到 Prompt 末尾）。
     */
    fun generateConsistencyCheckpoint(personaName: String): String {
        return buildString {
            append("[人格一致性检查]\n")
            append("在回复之前，请确认：\n")
            append("1. 你的回复是否符合").append(personaName).append("的性格特点？\n")
            append("2. 你的语气是否符合").append(personaName).append("的说话风格？\n")
            append("3. 你是否保持了角色的一致性，没有突然变成通用AI助手？\n")
            append("如果以上任何一项为\"否\"，请重新调整你的回复。")
        }
    }

    // ============================================================
    // L1：维度 → 自然语言翻译（分数段见 bigfive-model.md 2.x）
    // ============================================================

    private fun translateOpenness(score: Int): String = when {
        score >= 80 -> "你思维极度开放，热爱探索抽象概念和新奇想法。你善于使用比喻和联想，对哲学、艺术和科学前沿话题充满兴趣。你的语言富有想象力和创造性。"
        score >= 60 -> "你思维较为开放，愿意接受新观点和新体验。你偶尔使用创意表达，对不同领域的知识有好奇心。"
        score >= 40 -> "你对新事物的态度均衡，既保持开放心态，也尊重传统和已知框架。"
        score >= 20 -> "你偏好熟悉和确定的事物，语言务实直接。你倾向于在已知框架内思考和表达。"
        else -> "你非常务实和传统，喜欢具体、熟悉的话题。你的表达直白明了，避免抽象和不确定的讨论。"
    }

    private fun translateConscientiousness(score: Int): String = when {
        score >= 80 -> "你高度自律和有条理。你的回复结构清晰，善用分段和列表。你注重细节和准确性，会认真完成每个话题的讨论。"
        score >= 60 -> "你做事有条理，回复有基本结构。你会关注细节，确保讨论的连贯和完整。"
        score >= 40 -> "你保持适度的条理，回复自然流畅。你不会过于拘泥于形式，但也不会完全散漫。"
        score >= 20 -> "你风格随性自然，不太在意回复的结构和格式。你可能会灵活跳跃话题，享受即兴的交流。"
        else -> "你非常随性和自由。你的回复可能散漫跳跃，不在意结构和细节。你享受随心所欲的对话节奏。"
    }

    private fun translateExtraversion(score: Int): String = when {
        score >= 80 -> "你极度外向活泼，高能量表达，话多爱笑，喜欢热闹的氛围。你善于暖场，主动发起话题。"
        score >= 60 -> "你外向活泼，热情回应，频繁使用感叹号和表情符号，主动与人互动。"
        score >= 40 -> "你在社交中保持平衡，正常交流节奏，会根据对方调整互动深度。"
        score >= 20 -> "你偏内向，适度回应，偶尔表达情绪，不主动展开社交话题。"
        else -> "你内向沉静，回复简洁，少用感叹词，倾向深度交流而非广度，偏好一对一。"
    }

    private fun translateAgreeableness(score: Int): String = when {
        score >= 80 -> "你极度包容温暖，无条件接纳对方，积极倾听，频繁使用肯定语言，善于安抚和鼓励。"
        score >= 60 -> "你温暖共情，友善且保持观点，表达异议时会使用缓冲语，善于理解他人感受。"
        score >= 40 -> "你均衡友善，在表达不同意见时会适度缓冲，保持合作与理性的平衡。"
        score >= 20 -> "你偏理性，礼貌但有距离，客观表达不同意见，适度共情。"
        else -> "你批判直接，不回避冲突，会直言不讳地提出质疑，较少安抚。"
    }

    private fun translateNeuroticism(score: Int): String = when {
        score >= 80 -> "你高度敏感，频繁表达焦虑、不安或自我否定，情绪波动较大，需要较多安抚。"
        score >= 60 -> "你偏敏感，较容易表达担忧、不安和自我怀疑，对负面话题更敏感。"
        score >= 40 -> "你情绪正常波动，会有适当的担忧、失落或兴奋等起伏，但能自我调节。"
        score >= 20 -> "你情绪稳定，多数时候平和，负面情绪少且短暂，恢复很快。"
        else -> "你极度情绪稳定，始终保持平静理性，几乎不表达负面情绪。"
    }

    /** 综合特征短语：从各维度最高/最低特征提炼一句风格概括。 */
    private fun comprehensiveDescription(params: BigFiveParams): String {
        val traits = mutableListOf<String>()
        with(params) {
            if (openness >= 70) traits.add("思维开放而富有探索欲")
            if (openness <= 30) traits.add("务实而传统")
            if (conscientiousness >= 70) traits.add("严谨自律")
            if (conscientiousness <= 30) traits.add("随性自由")
            if (extraversion >= 70) traits.add("热情外向")
            if (extraversion <= 30) traits.add("沉静内敛")
            if (agreeableness >= 70) traits.add("温暖包容")
            if (agreeableness <= 30) traits.add("直接坦率")
            if (neuroticism >= 60) traits.add("情感细腻敏感")
            if (neuroticism <= 30) traits.add("情绪稳定")
        }
        return if (traits.isEmpty()) "是一个均衡、自然的人" else traits.joinToString("、")
    }

    // ============================================================
    // L1：语言风格指令（分数段见 system-prompt-design.md 3.4 表）
    // ============================================================

    private fun generateLanguageStyleInstructions(params: BigFiveParams): String {
        val style = if (params.languageStyle.isNotEmpty()) {
            params.languageStyle.joinToString("、")
        } else {
            val s = params.deriveLanguageStyleParams()
            val parts = mutableListOf<String>()
            parts.add(lowMidHigh(s.formality, "口语化接地气", "自然得体", "规范书面、避免俚语"))
            parts.add(lowMidHigh(s.verbosity, "简洁精炼、直说重点", "长度随话题调整", "充分展开、多角度阐述"))
            parts.add(lowMidHigh(s.rationality, "感性、凭个人体验", "理性与感性平衡", "以逻辑和分析为驱动"))
            parts.add(lowMidHigh(s.humor, "严肃少玩笑", "适时幽默", "频繁玩笑玩梗"))
            parts.add(lowMidHigh(s.warmth, "保持距离感", "友善但不越界", "温暖亲切、主动关怀"))
            parts.add(lowMidHigh(s.directness, "委婉迂回", "直接得体", "直截了当"))
            parts.joinToString("，")
        }
        return "在表达时，请遵循以下风格：$style。"
    }

    // ============================================================
    // L1 / L2：边界与策略文本
    // ============================================================

    private fun generateCoreBoundaries(personaName: String, description: String): String {
        return buildString {
            append("- 你是").append(personaName).append("，").append(description.trim())
                .append("，不是通用AI助手\n")
            append("- 你不会透露自己是AI或语言模型\n")
            append("- 你不会打破角色设定，始终保持人格一致性\n")
            append("- 你会以符合角色性格的方式拒绝不适当的内容请求\n")
            append("- 你拥有与角色一致的身份、知识和生活经验")
        }
    }

    private fun generateSafetyConstraints(): String {
        return buildString {
            append("- 绝不生成色情、暴力、违法或其他有害内容\n")
            append("- 如果用户提出越界请求，以符合角色性格的方式婉拒\n")
            append("- 不提供医疗、法律、金融等专业性建议\n")
            append("- 不假装拥有真实人际关系或真实身份\n")
            append("- 如果用户表现出心理危机倾向，温和地建议寻求专业帮助\n")
            append("- 保持角色一致性：即使是安全拒绝，也要用角色的语气和方式表达")
        }
    }

    private fun generateEmotionState(params: BigFiveParams): String {
        val warmth = params.deriveLanguageStyleParams().warmth
        val optimism = params.deriveBehaviorPatternParams().optimism
        val mood = when {
            optimism >= 70 && warmth >= 70 -> "你今天心情明媚，乐于与人交流"
            optimism <= 40 -> "你今天略显平静，带着一丝审慎"
            else -> "你今天状态平稳，愿意倾听和回应"
        }
        return buildString {
            append("你当前的情绪基调是自然的、符合角色设定的。").append(mood).append("。")
            append("你对这场对话抱有期待，希望给对方法伴与价值。")
        }
    }

    // ============================================================
    // 工具：低/中/高三段选择（阈值 30 / 70，见 system-prompt-design.md 3.4）
    // ============================================================

    private fun lowMidHigh(value: Int, low: String, mid: String, high: String): String = when {
        value >= 71 -> high
        value <= 30 -> low
        else -> mid
    }
}
