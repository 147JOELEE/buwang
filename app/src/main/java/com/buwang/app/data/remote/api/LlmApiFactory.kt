package com.buwang.app.data.remote.api

import com.buwang.app.BuildConfig
import com.buwang.app.data.remote.dto.ChatRequest
import com.buwang.app.data.remote.dto.ChatResponse
import com.buwang.app.data.remote.dto.ModelListResponse
import com.buwang.app.data.remote.dto.TestApiRequest
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 多模型 API 适配器工厂
 *
 * 根据用户选择的模型，创建指向对应厂商 Base URL 的 LlmApi 实例。
 * 每个模型使用独立的 Retrofit 实例，避免 Base URL 切换问题。
 *
 * 使用方式：
 * ```kotlin
 * val api = factory.create("deepseek-v3", apiKey)
 * val response = api.chatCompletion(request)
 * ```
 */
class LlmApiFactory(
    private val moshi: Moshi
) {

    /** 缓存已创建的 Retrofit 实例，避免重复创建 */
    private val cache = mutableMapOf<String, LlmApi>()

    /**
     * 支持的模型配置
     */
    companion object {
        /** DeepSeek V3 — PRD 默认选型（保留为可选模型） */
        const val MODEL_DEEPSEEK_V3 = "deepseek-v3"
        const val MODEL_DEEPSEEK_R1 = "deepseek-r1"

        /** 通义千问 */
        const val MODEL_QWEN_TURBO = "qwen-turbo"
        const val MODEL_QWEN_PLUS = "qwen-plus"
        const val MODEL_QWEN_MAX = "qwen-max"

        /** 硅基流动（免费模型聚合平台） */
        const val MODEL_SILICON_DEEPSEEK = "silicon-deepseek"
        const val MODEL_SILICON_QWEN = "silicon-qwen"

        /** Tokens-Box — 联调聚合网关（提供 MiniMax 系列模型） */
        const val MODEL_TOKENSBOX = "tokens-box"
        const val MODEL_TOKENSBOX_M3 = "tokens-box-m3"

        /** 自定义 — 用户自行配置 */
        const val MODEL_CUSTOM = "custom"

        /**
         * 内置 API Key（Tokens-Box 网关）已从源码中移除。
         *
         * 安全交付：密钥不再硬编码，改由 [com.buwang.app.BuildConfig.BUILTIN_API_KEY]
         * 在构建期从 local.properties（gitignore 排除）或环境变量 BUWANG_BUILTIN_API_KEY
         * 注入，避免明文进入版本库。运行时统一通过 BuildConfig.BUILTIN_API_KEY 读取。
         */

        /** 默认模型标识（首次启动 / 恢复默认时选用） */
        const val DEFAULT_MODEL = MODEL_TOKENSBOX

        /** 模型配置映射：模型标识 → (显示名称, Base URL, 默认模型名) */
        val MODEL_CONFIGS = mapOf(
            MODEL_DEEPSEEK_V3 to ModelConfig(
                displayName = "DeepSeek V3",
                baseUrl = "https://api.deepseek.com/v1/",
                modelName = "deepseek-chat"
            ),
            MODEL_DEEPSEEK_R1 to ModelConfig(
                displayName = "DeepSeek R1",
                baseUrl = "https://api.deepseek.com/v1/",
                modelName = "deepseek-reasoner"
            ),
            MODEL_QWEN_TURBO to ModelConfig(
                displayName = "通义千问 Turbo",
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                modelName = "qwen-turbo"
            ),
            MODEL_QWEN_PLUS to ModelConfig(
                displayName = "通义千问 Plus",
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                modelName = "qwen-plus"
            ),
            MODEL_QWEN_MAX to ModelConfig(
                displayName = "通义千问 Max",
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                modelName = "qwen-max"
            ),
            MODEL_SILICON_DEEPSEEK to ModelConfig(
                displayName = "硅基流动·DeepSeek",
                baseUrl = "https://api.siliconflow.cn/v1/",
                modelName = "deepseek-ai/DeepSeek-V3"
            ),
            MODEL_SILICON_QWEN to ModelConfig(
                displayName = "硅基流动·Qwen",
                baseUrl = "https://api.siliconflow.cn/v1/",
                modelName = "Qwen/Qwen2.5-72B-Instruct"
            ),
            MODEL_TOKENSBOX to ModelConfig(
                displayName = "Tokens-Box · MiniMax-M2.7",
                baseUrl = "https://tokens-box.com/v1/",
                modelName = "MiniMax-M2.7"
            ),
            MODEL_TOKENSBOX_M3 to ModelConfig(
                displayName = "Tokens-Box · MiniMax-M3",
                baseUrl = "https://tokens-box.com/v1/",
                modelName = "MiniMax-M3"
            )
        )
    }

    /**
     * 创建指定模型的 LlmApi 实例
     *
     * @param modelKey 模型标识，如 "deepseek-v3"、"qwen-turbo"
     * @param apiKey API Key，内置模式使用默认 Key
     * @param customBaseUrl 自定义 Base URL（仅 MODEL_CUSTOM 时使用）
     * @return 指向对应厂商的 LlmApi 实例
     */
    fun create(
        modelKey: String,
        apiKey: String,
        customBaseUrl: String? = null
    ): LlmApi {
        val config = if (modelKey == MODEL_CUSTOM && customBaseUrl != null) {
            ModelConfig("自定义", customBaseUrl, "default")
        } else {
            MODEL_CONFIGS[modelKey] ?: MODEL_CONFIGS[DEFAULT_MODEL]!!
        }

        // 调用方未传入 Key（如内置模式走空串）时，回退到内置 Key
        val effectiveApiKey = if (apiKey.isBlank()) BuildConfig.BUILTIN_API_KEY else apiKey

        val cacheKey = "$modelKey:${config.baseUrl}"

        return cache.getOrPut(cacheKey) {
            buildRetrofit(config.baseUrl, effectiveApiKey).create(LlmApi::class.java)
        }
    }

    /**
     * 获取模型配置信息
     */
    fun getModelConfig(modelKey: String): ModelConfig? {
        return MODEL_CONFIGS[modelKey]
    }

    /**
     * 获取所有可用模型列表
     */
    fun getAvailableModels(): List<Pair<String, ModelConfig>> {
        return MODEL_CONFIGS.map { (key, config) -> key to config }
    }

    /** 获取内置 Key（供拦截器 / 仓库统一解析使用） */
    fun getBuiltinApiKey(): String = BuildConfig.BUILTIN_API_KEY

    /** 获取默认模型标识 */
    fun getDefaultModel(): String = DEFAULT_MODEL

    /**
     * 构建 Retrofit 实例
     */
    private fun buildRetrofit(baseUrl: String, apiKey: String): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream, application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)  // 流式响应需要更长超时
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * 清除缓存（API Key 变更时调用）
     */
    fun clearCache() {
        cache.clear()
    }
}

/**
 * 模型配置数据类
 */
data class ModelConfig(
    val displayName: String,
    val baseUrl: String,
    val modelName: String
)
