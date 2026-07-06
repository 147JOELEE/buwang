package com.buwang.app.core.personality

import org.json.JSONArray
import org.json.JSONObject

/**
 * Big Five 人格参数模型
 *
 * 五个核心维度均使用 0-100 的 Int 取值范围（详见设计文档 bigfive-model.md）：
 * - openness          开放性 (O)
 * - conscientiousness 尽责性 (C)
 * - extraversion      外向性 (E)
 * - agreeableness     宜人性 (A)
 * - neuroticism       神经质 (N)
 *
 * [languageStyle] 与 [behaviorPattern] 为"衍生标签列表"，可由 Big Five 自动推导，
 * 也允许调用方显式覆盖（例如从 MBTI 映射表或预设角色中提取的真实标签）。
 *
 * 序列化说明：
 * - [toJson] 输出扁平结构，顶层为五个核心维度键，便于与现有 `personalityParams`
 *   字段（见 PersonaEntity / ExportEngine）直接互操作；同时附带两个标签数组键。
 * - [fromJson] 兼容"扁平结构"与"嵌套 big_five 结构"两种历史格式。
 */
data class BigFiveParams(
    val openness: Int,
    val conscientiousness: Int,
    val extraversion: Int,
    val agreeableness: Int,
    val neuroticism: Int,
    /** 语言风格标签列表，如 ["理性型", "结构化", "简洁有力"] */
    val languageStyle: List<String> = emptyList(),
    /** 行为模式标签列表，如 ["引导型", "协作型", "理性共情"] */
    val behaviorPattern: List<String> = emptyList()
) {

    init {
        require(openness in 0..100) { "openness 必须在 0-100 范围内，实际：$openness" }
        require(conscientiousness in 0..100) { "conscientiousness 必须在 0-100 范围内，实际：$conscientiousness" }
        require(extraversion in 0..100) { "extraversion 必须在 0-100 范围内，实际：$extraversion" }
        require(agreeableness in 0..100) { "agreeableness 必须在 0-100 范围内，实际：$agreeableness" }
        require(neuroticism in 0..100) { "neuroticism 必须在 0-100 范围内，实际：$neuroticism" }
    }

    /**
     * 根据 bigfive-model.md 3.x 的派生公式，从 Big Five 自动计算 8 项语言风格参数。
     * 当调用方未提供显式 [languageStyle] 标签时，生成器会使用这些数值生成风格指令。
     */
    fun deriveLanguageStyleParams(): LanguageStyleParams {
        val o = openness.toDouble()
        val c = conscientiousness.toDouble()
        val e = extraversion.toDouble()
        val a = agreeableness.toDouble()
        val n = neuroticism.toDouble()
        return LanguageStyleParams(
            formality = ((c * 0.6) + ((100 - e) * 0.2) + (o * 0.2)).roundToInt(),
            verbosity = ((e * 0.5) + (o * 0.5)).roundToInt(),
            rationality = ((c * 0.5) + ((100 - n) * 0.5)).roundToInt(),
            humor = ((e * 0.4) + (o * 0.6)).roundToInt(),
            warmth = ((a * 0.6) + (e * 0.4)).roundToInt(),
            intellectualDepth = ((o * 0.7) + (c * 0.3)).roundToInt(),
            directness = (((100 - a) * 0.6) + (c * 0.4)).roundToInt(),
            emotionalExpressiveness = ((e * 0.5) + (n * 0.5)).roundToInt()
        )
    }

    /**
     * 根据 bigfive-model.md 4.x 的派生公式，从 Big Five 自动计算 8 项行为模式参数。
     */
    fun deriveBehaviorPatternParams(): BehaviorPatternParams {
        val o = openness.toDouble()
        val c = conscientiousness.toDouble()
        val e = extraversion.toDouble()
        val a = agreeableness.toDouble()
        val n = neuroticism.toDouble()
        return BehaviorPatternParams(
            initiative = ((e * 0.6) + (o * 0.4)).roundToInt(),
            optimism = ((e * 0.5) + ((100 - n) * 0.5)).roundToInt(),
            decisiveness = ((c * 0.5) + ((100 - n) * 0.5)).roundToInt(),
            curiosity = o.roundToInt(),
            empathy = ((a * 0.7) + (n * 0.3)).roundToInt(),
            riskTaking = ((o * 0.5) + (e * 0.5)).roundToInt(),
            selfDisclosure = ((e * 0.6) + (a * 0.4)).roundToInt(),
            argumentativeness = ((o * 0.5) + ((100 - a) * 0.5)).roundToInt()
        )
    }

    /**
     * 序列化为 JSON 字符串。
     * 顶层同时包含扁平的核心维度键与标签数组键，保证与现有代码互操作。
     */
    fun toJson(): String = JSONObject().apply {
        put("openness", openness)
        put("conscientiousness", conscientiousness)
        put("extraversion", extraversion)
        put("agreeableness", agreeableness)
        put("neuroticism", neuroticism)
        put("language_style", JSONArray(languageStyle))
        put("behavior_patterns", JSONArray(behaviorPattern))
    }.toString()

    companion object {

        /** 完全均衡的默认人格（5 维均为 50），用于新角色初始化。 */
        fun default(): BigFiveParams = BigFiveParams(50, 50, 50, 50, 50)

        /**
         * 由 5 个核心维度创建实例，标签列表留空（交由生成器按公式推导）。
         */
        fun fromBigFive(
            openness: Int,
            conscientiousness: Int,
            extraversion: Int,
            agreeableness: Int,
            neuroticism: Int,
            languageStyle: List<String> = emptyList(),
            behaviorPattern: List<String> = emptyList()
        ): BigFiveParams = BigFiveParams(
            openness = openness.coerceIn(0, 100),
            conscientiousness = conscientiousness.coerceIn(0, 100),
            extraversion = extraversion.coerceIn(0, 100),
            agreeableness = agreeableness.coerceIn(0, 100),
            neuroticism = neuroticism.coerceIn(0, 100),
            languageStyle = languageStyle,
            behaviorPattern = behaviorPattern
        )

        /**
         * 从 MBTI 类型加载人格参数（委托给 [MbtiMapper]）。
         * @param mbti 如 "INTJ"、"enfj"，大小写不敏感
         */
        fun fromMbti(mbti: String): BigFiveParams = MbtiMapper.mapMbtiToBigFive(mbti)

        /**
         * 从 JSON 字符串反序列化。兼容两种格式：
         * 1. 扁平结构：{"openness":75, ...}
         * 2. 嵌套结构：{"big_five": {"openness":75, ...}, ...}
         */
        fun fromJson(json: String): BigFiveParams {
            val root = JSONObject(json)
            val o: Int
            val c: Int
            val e: Int
            val a: Int
            val n: Int
            if (root.has("big_five") && root.get("big_five") is JSONObject) {
                val bf = root.getJSONObject("big_five")
                o = bf.optInt("openness", 50)
                c = bf.optInt("conscientiousness", 50)
                e = bf.optInt("extraversion", 50)
                a = bf.optInt("agreeableness", 50)
                n = bf.optInt("neuroticism", 50)
            } else {
                o = root.optInt("openness", 50)
                c = root.optInt("conscientiousness", 50)
                e = root.optInt("extraversion", 50)
                a = root.optInt("agreeableness", 50)
                n = root.optInt("neuroticism", 50)
            }
            val langStyle = root.optJSONArray("language_style").toTagList()
            val behaviorPattern = root.optJSONArray("behavior_patterns").toTagList()
            return BigFiveParams(o, c, e, a, n, langStyle, behaviorPattern)
        }

        /** 将 JSONArray 安全地转换为字符串标签列表（null 时返回空列表）。 */
        internal fun JSONArray?.toTagList(): List<String> {
            if (this == null) return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until length()) {
                list.add(optString(i, ""))
            }
            return list.filter { it.isNotBlank() }
        }
    }
}

/**
 * 派生的语言风格参数（0-100）。
 * 字段定义见 bigfive-model.md 3.1。
 */
data class LanguageStyleParams(
    val formality: Int,
    val verbosity: Int,
    val rationality: Int,
    val humor: Int,
    val warmth: Int,
    val intellectualDepth: Int,
    val directness: Int,
    val emotionalExpressiveness: Int
)

/**
 * 派生的行为模式参数（0-100）。
 * 字段定义见 bigfive-model.md 4.1。
 */
data class BehaviorPatternParams(
    val initiative: Int,
    val optimism: Int,
    val decisiveness: Int,
    val curiosity: Int,
    val empathy: Int,
    val riskTaking: Int,
    val selfDisclosure: Int,
    val argumentativeness: Int
)

/** Double 四舍五入为 Int（仅用于派生计算，结果已在 0-100 安全范围内）。 */
private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt().coerceIn(0, 100)
