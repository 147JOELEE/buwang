package com.buwang.app.core.personality

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * 聊天文本人格分析器
 *
 * 用于从用户的聊天文本中分析其 Big Five 人格特征（设计文档 chat-analysis-prompt.md）。
 * 本类只负责**构建分析 Prompt** 与**解析 LLM 返回的 JSON 结果**；真正的分析推理
 * 由 LLM 完成（本地或云端，符合「不忘」纯本地存储与隐私优先理念，见 D-405）。
 *
 * 重要约束（D-401）：仅分析用户发言，[buildAnalysisPrompt] 调用方应已剔除 AI 回复。
 */
class ChatAnalysisAnalyzer @Inject constructor() {

    /**
     * 构建发送给 LLM 的人格分析 Prompt。
     *
     * @param chatText 清洗后的用户聊天文本（仅用户发言）
     * @return 完整的分析 Prompt（含输出 JSON 格式约定）
     */
    fun buildAnalysisPrompt(chatText: String): String = buildString {
        append("你是一个心理学专家，擅长通过语言行为分析人格特征。请基于以下用户的聊天文本，分析其 Big Five 人格特征。\n\n")
        append("## 分析框架\n")
        append("请从以下五个维度进行评估，每个维度给出0-100的分数：\n\n")
        append("1. **开放性 (Openness)**：思维开放程度、好奇心、对新鲜事物的接受度\n")
        append("   - 高分特征：使用丰富的词汇、讨论抽象话题、展现想象力\n")
        append("   - 低分特征：语言平实直接、偏好具体话题、表达务实\n\n")
        append("2. **尽责性 (Conscientiousness)**：条理性、自律性、对细节的关注\n")
        append("   - 高分特征：表达有条理、关注细节、使用结构化表达\n")
        append("   - 低分特征：表达随性、话题跳跃、不拘泥于形式\n\n")
        append("3. **外向性 (Extraversion)**：社交活跃度、情绪外放程度\n")
        append("   - 高分特征：话多、使用感叹词、主动分享、热情表达\n")
        append("   - 低分特征：简洁、内敛、被动回应、情绪表达克制\n\n")
        append("4. **宜人性 (Agreeableness)**：合作性、共情倾向、友善程度\n")
        append("   - 高分特征：使用肯定词、表达共情、回避冲突、温暖友善\n")
        append("   - 低分特征：直接质疑、表达异议、较少情感回应\n\n")
        append("5. **神经质 (Neuroticism)**：情绪稳定性（反向）、焦虑倾向\n")
        append("   - 高分特征：表达担忧、自我怀疑、负面情绪词汇多\n")
        append("   - 低分特征：情绪稳定、乐观积极、较少负面表达\n\n")
        append("## 语言风格评估\n")
        append("请额外评估以下语言风格维度：\n")
        append("- 正式度 (0-100)：口语化←→书面化\n")
        append("- 冗长度 (0-100)：简洁←→详细\n")
        append("- 理性度 (0-100)：感性←→理性\n")
        append("- 幽默度 (0-100)：严肃←→幽默\n")
        append("- 温暖度 (0-100)：冷淡←→温暖\n\n")
        append("## 用户聊天文本\n")
        append("```\n")
        append(chatText.trim())
        append("\n```\n\n")
        append("## 输出要求\n")
        append("请严格按以下 JSON 格式输出分析结果，不要包含任何其他内容：\n")
        append(OUTPUT_JSON_TEMPLATE)
        append("\n\n## 注意事项\n")
        append("- 只分析用户文本，忽略 AI 回复\n")
        append("- 基于实际语言行为评分，不要受对话内容主题的过度影响\n")
        append("- 如果某些维度的文本证据不足，降低该维度的置信度\n")
        append("- 综合置信度取各维度置信度的平均值")
    }

    /**
     * 将 LLM 返回的 JSON 字符串解析为 [PersonalityAnalysisResult]。
     * 解析失败时抛出 [IllegalArgumentException]。
     */
    fun parseAnalysisResult(json: String): PersonalityAnalysisResult {
        val root = JSONObject(stripCodeFences(json))
        val bf = root.getJSONObject("big_five")
        fun dim(name: String): DimensionResult {
            val d = bf.getJSONObject(name)
            val evidenceArr = d.getJSONArray("evidence")
            val evidence = (0 until evidenceArr.length()).map { evidenceArr.getString(it) }
            return DimensionResult(
                score = d.getInt("score").coerceIn(0, 100),
                evidence = evidence,
                confidence = d.getDouble("confidence").coerceIn(0.0, 1.0)
            )
        }
        val ls = root.getJSONObject("language_style")
        val languageStyle = LanguageStyleAnalysis(
            formality = ls.getInt("formality").coerceIn(0, 100),
            verbosity = ls.getInt("verbosity").coerceIn(0, 100),
            rationality = ls.getInt("rationality").coerceIn(0, 100),
            humor = ls.getInt("humor").coerceIn(0, 100),
            warmth = ls.getInt("warmth").coerceIn(0, 100)
        )
        return PersonalityAnalysisResult(
            bigFive = BigFiveAnalysis(
                openness = dim("openness"),
                conscientiousness = dim("conscientiousness"),
                extraversion = dim("extraversion"),
                agreeableness = dim("agreeableness"),
                neuroticism = dim("neuroticism")
            ),
            languageStyle = languageStyle,
            summary = root.optString("summary", ""),
            overallConfidence = root.optDouble("overall_confidence", 0.0).coerceIn(0.0, 1.0),
            textQualityAssessment = root.optString("text_quality_assessment", "")
        )
    }

    companion object {
        /** 输出 JSON 格式约定（嵌入分析 Prompt）。 */
        /**
         * 去除 LLM 可能在 JSON 外层包裹的 ```json ... ``` 代码围栏，
         * 并跳过首个 '{' 之前与末个 '}' 之后的无关文本。
         */
        private fun stripCodeFences(raw: String): String {
            var s = raw.trim()
            val fenceStart = s.indexOf("{")
            val fenceEnd = s.lastIndexOf("}")
            if (fenceStart >= 0 && fenceEnd > fenceStart) {
                s = s.substring(fenceStart, fenceEnd + 1)
            }
            return s
        }

        private const val OUTPUT_JSON_TEMPLATE = """```json
{
  "big_five": {
    "openness": { "score": 0, "evidence": ["文本证据1", "文本证据2"], "confidence": 0.0 },
    "conscientiousness": { "score": 0, "evidence": ["文本证据1", "文本证据2"], "confidence": 0.0 },
    "extraversion": { "score": 0, "evidence": ["文本证据1", "文本证据2"], "confidence": 0.0 },
    "agreeableness": { "score": 0, "evidence": ["文本证据1", "文本证据2"], "confidence": 0.0 },
    "neuroticism": { "score": 0, "evidence": ["文本证据1", "文本证据2"], "confidence": 0.0 }
  },
  "language_style": {
    "formality": 0, "verbosity": 0, "rationality": 0, "humor": 0, "warmth": 0
  },
  "summary": "对用户人格特征的综合描述，50-100字",
  "overall_confidence": 0.0,
  "text_quality_assessment": "对分析文本质量的简要评估"
}
```"""
    }
}

/** 单个 Big Five 维度的分析结果。 */
data class DimensionResult(
    val score: Int,
    val evidence: List<String>,
    val confidence: Double
)

/** Big Five 五维度的完整分析结果。 */
data class BigFiveAnalysis(
    val openness: DimensionResult,
    val conscientiousness: DimensionResult,
    val extraversion: DimensionResult,
    val agreeableness: DimensionResult,
    val neuroticism: DimensionResult
) {
    /** 转换为可直接用于创建角色的 [BigFiveParams]（不含标签，交由生成器推导）。 */
    fun toBigFiveParams(): BigFiveParams = BigFiveParams(
        openness = openness.score,
        conscientiousness = conscientiousness.score,
        extraversion = extraversion.score,
        agreeableness = agreeableness.score,
        neuroticism = neuroticism.score
    )
}

/** 语言风格维度的分析结果（0-100）。 */
data class LanguageStyleAnalysis(
    val formality: Int,
    val verbosity: Int,
    val rationality: Int,
    val humor: Int,
    val warmth: Int
)

/** 聊天文本人格分析的完整结果。 */
data class PersonalityAnalysisResult(
    val bigFive: BigFiveAnalysis,
    val languageStyle: LanguageStyleAnalysis,
    val summary: String,
    val overallConfidence: Double,
    val textQualityAssessment: String
)
