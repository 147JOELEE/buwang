package com.buwang.app.data.remote.api

import com.buwang.app.data.remote.dto.ChatRequest
import com.buwang.app.data.remote.dto.ChatResponse
import com.buwang.app.data.remote.dto.ModelListResponse
import com.buwang.app.data.remote.dto.TestApiRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * 统一 LLM API 接口 — 适配 OpenAI 兼容格式
 *
 * 支持的模型提供方（均兼容 OpenAI 格式）：
 * - DeepSeek V3: https://api.deepseek.com/v1
 * - 通义千问: https://dashscope.aliyuncs.com/compatible-mode/v1
 * - 文心一言: 通过兼容层接入
 * - MiniMax: 通过兼容层接入
 * - 硅基流动: https://api.siliconflow.cn/v1
 * - 任意 OpenAI 兼容接口
 */
interface LlmApi {

    /**
     * 非流式对话 — 发送消息并等待完整回复
     */
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): Response<ChatResponse>

    /**
     * 流式对话 — SSE 事件流，逐字返回
     * 使用 @Streaming 避免 OkHttp 一次性缓冲整个响应体
     */
    @Streaming
    @POST("chat/completions")
    suspend fun chatCompletionStream(@Body request: ChatRequest): Response<ResponseBody>

    /**
     * 获取可用模型列表（部分厂商支持）
     */
    @GET("models")
    suspend fun listModels(): Response<ModelListResponse>

    /**
     * 发送最小化测试请求，用于验证 API Key 有效性
     */
    @POST("chat/completions")
    suspend fun testApiKey(@Body request: TestApiRequest): Response<ChatResponse>
}
