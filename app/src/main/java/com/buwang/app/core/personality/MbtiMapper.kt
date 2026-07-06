package com.buwang.app.core.personality

/**
 * MBTI → Big Five 映射器
 *
 * 内置 16 种 MBTI 类型到 [BigFiveParams] 的映射表，数值取自设计文档
 * mbti-mapping.json 中"经验校准后"的真实得分（big_five.typical 字段），
 * 标签取自 language_style_tags / behavior_tags。
 *
 * 算法背景（加权维度映射法）见 mbti-mapping.md：
 * - E/I → 外向性（主导）
 * - S/N → 开放性（主导）
 * - T/F → 宜人性（主导）
 * - J/P → 尽责性（主导）
 *
 * 注意：本映射仅作"正向"用途（MBTI 作为创建角色的快捷起点），
 * Big Five 才是人格引擎唯一的权威参数源（见 D-104）。
 */
object MbtiMapper {

    /** 单个 MBTI 类型的映射记录。 */
    private data class MbtiEntry(
        val mbti: String,
        val nameCn: String,
        val openness: Int,
        val conscientiousness: Int,
        val extraversion: Int,
        val agreeableness: Int,
        val neuroticism: Int,
        val languageStyleTags: List<String>,
        val behaviorTags: List<String>
    ) {
        fun toBigFiveParams(): BigFiveParams = BigFiveParams(
            openness = openness,
            conscientiousness = conscientiousness,
            extraversion = extraversion,
            agreeableness = agreeableness,
            neuroticism = neuroticism,
            languageStyle = languageStyleTags,
            behaviorPattern = behaviorTags
        )
    }

    /** 16 种类型的完整映射表（数值均来自 mbti-mapping.json）。 */
    private val MBTI_TABLE: Map<String, MbtiEntry> = mapOf(
        "INTJ" to MbtiEntry("INTJ", "建筑师", 75, 80, 20, 30, 40,
            listOf("理性型", "结构化", "深度思辨", "简洁有力"),
            listOf("规划型", "独立型", "高标准化", "分析型")),
        "INTP" to MbtiEntry("INTP", "逻辑学家", 88, 45, 18, 35, 50,
            listOf("逻辑派", "抽象型", "探索式", "精确表达"),
            listOf("探索型", "分析型", "灵活型", "独立思考")),
        "ENTJ" to MbtiEntry("ENTJ", "指挥官", 70, 88, 80, 28, 30,
            listOf("果断型", "结构化", "权威感", "目标导向"),
            listOf("领导型", "决断型", "高效型", "挑战型")),
        "ENTP" to MbtiEntry("ENTP", "辩论家", 85, 38, 82, 32, 38,
            listOf("辩论型", "机智幽默", "发散思维", "挑战性"),
            listOf("探索型", "辩论型", "即兴型", "创新思维")),
        "INFJ" to MbtiEntry("INFJ", "提倡者", 78, 75, 25, 78, 55,
            listOf("温暖型", "洞察力", "理想主义", "深度共情"),
            listOf("理想型", "规划型", "共情型", "价值导向")),
        "INFP" to MbtiEntry("INFP", "调停者", 82, 35, 22, 82, 62,
            listOf("感性型", "诗意表达", "理想主义", "深度共情"),
            listOf("理想型", "共情型", "创意型", "灵活型")),
        "ENFJ" to MbtiEntry("ENFJ", "主人公", 72, 72, 85, 85, 45,
            listOf("温暖型", "激励型", "社交型", "结构化"),
            listOf("领导型", "共情型", "组织型", "利他型")),
        "ENFP" to MbtiEntry("ENFP", "竞选者", 88, 30, 88, 78, 48,
            listOf("热情型", "创意型", "发散思维", "感染力"),
            listOf("探索型", "社交型", "即兴型", "乐观型")),
        "ISTJ" to MbtiEntry("ISTJ", "物流师", 25, 90, 22, 48, 30,
            listOf("务实型", "结构化", "精确表达", "传统风格"),
            listOf("执行型", "规则型", "稳定型", "细节型")),
        "ISFJ" to MbtiEntry("ISFJ", "守卫者", 28, 82, 20, 78, 42,
            listOf("温暖型", "务实型", "细腻表达", "关怀型"),
            listOf("守护型", "执行型", "共情型", "稳定型")),
        "ESTJ" to MbtiEntry("ESTJ", "总经理", 32, 92, 75, 35, 28,
            listOf("果断型", "务实型", "结构化", "权威感"),
            listOf("管理型", "决断型", "高效型", "传统型")),
        "ESFJ" to MbtiEntry("ESFJ", "执政官", 30, 80, 78, 82, 38,
            listOf("温暖型", "社交型", "务实型", "关怀型"),
            listOf("社交型", "共情型", "组织型", "和谐型")),
        "ISTP" to MbtiEntry("ISTP", "鉴赏家", 55, 32, 18, 38, 28,
            listOf("务实型", "简洁型", "实用导向", "冷静型"),
            listOf("实践型", "灵活型", "独立型", "冷静型")),
        "ISFP" to MbtiEntry("ISFP", "探险家", 58, 28, 22, 72, 48,
            listOf("感性型", "审美型", "温柔表达", "当下感"),
            listOf("艺术型", "灵活型", "共情型", "体验型")),
        "ESTP" to MbtiEntry("ESTP", "企业家", 52, 35, 85, 40, 25,
            listOf("行动型", "直接型", "幽默型", "接地气"),
            listOf("行动型", "冒险型", "即兴型", "社交型")),
        "ESFP" to MbtiEntry("ESFP", "表演者", 55, 25, 92, 75, 38,
            listOf("热情型", "娱乐型", "接地气", "感染力"),
            listOf("社交型", "即兴型", "乐观型", "体验型"))
    )

    /**
     * 将 MBTI 类型映射为 [BigFiveParams]。
     *
     * @param mbti 4 字母类型（如 "INTJ"、"enfj"），大小写与首尾空格自动规范化
     * @return 对应的 Big Five 参数；若类型非法或未知，返回完全均衡的 [BigFiveParams.default]
     */
    fun mapMbtiToBigFive(mbti: String): BigFiveParams {
        val normalized = mbti.trim().uppercase()
        if (!isValidMbti(normalized)) return BigFiveParams.default()
        return MBTI_TABLE[normalized]?.toBigFiveParams() ?: BigFiveParams.default()
    }

    /**
     * 返回 MBTI 对应的中文名称（如 "INTJ" → "建筑师"）。
     * 非法/未知类型返回 null。
     */
    fun mbtiNameCn(mbti: String): String? {
        val normalized = mbti.trim().uppercase()
        return MBTI_TABLE[normalized]?.nameCn
    }

    /** 返回所有受支持的 MBTI 类型列表（大写，4 字母）。 */
    fun supportedTypes(): List<String> = MBTI_TABLE.keys.toList()

    /**
     * 校验字符串是否为合法 MBTI 类型：长度 4，各位依次属于
     * [IE]、[SN]、[TF]、[JP]。
     */
    private fun isValidMbti(s: String): Boolean {
        if (s.length != 4) return false
        return s[0] in "IE" && s[1] in "SN" && s[2] in "TF" && s[3] in "JP"
    }
}
