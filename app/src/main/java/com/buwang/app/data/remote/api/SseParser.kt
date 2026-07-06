package com.buwang.app.data.remote.api

import com.buwang.app.data.remote.dto.ChatStreamChunk
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import okio.IOException
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * SSE 流式响应解析器
 *
 * 将 OkHttp 的 ResponseBody（SSE 事件流）解析为 Kotlin Flow<ChatStreamChunk>，
 * 方便在 ViewModel 中逐条消费流式数据。
 *
 * SSE 格式说明：
 * ```
 * data: {"id":"chatcmpl-xxx","choices":[{"delta":{"content":"你好"}}]}
 *
 * data: {"id":"chatcmpl-xxx","choices":[{"delta":{"content":"！"}}]}
 *
 * data: [DONE]
 * ```
 *
 * 使用方式：
 * ```kotlin
 * val response = api.chatCompletionStream(request)
 * val stream = sseParser.parse(response.body()!!)
 * stream.collect { chunk ->
 *     // 更新 UI
 * }
 * ```
 */
class SseParser(private val moshi: Moshi) {

    companion object {
        private const val SSE_DATA_PREFIX = "data: "
        private const val SSE_DONE = "[DONE]"
    }

    /**
     * 解析 SSE 响应体为 Flow
     *
     * @param body ResponseBody from @Streaming call
     * @return Flow emitting ChatStreamChunk, completing when [DONE] received
     * @throws IOException on parsing or stream errors
     */
    fun parse(body: ResponseBody): Flow<SseResult> = flow {
        val reader = BufferedReader(InputStreamReader(body.byteStream()))
        val jsonAdapter = moshi.adapter(ChatStreamChunk::class.java)

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue

                // 跳过空行和注释
                if (currentLine.isBlank() || currentLine.startsWith(":")) {
                    continue
                }

                // 解析 "data: " 前缀
                if (!currentLine.startsWith(SSE_DATA_PREFIX)) {
                    continue
                }

                val data = currentLine.removePrefix(SSE_DATA_PREFIX).trim()

                // [DONE] 标记流结束
                if (data == SSE_DONE) {
                    emit(SseResult.Done)
                    break
                }

                // 解析 JSON 数据
                try {
                    val chunk = jsonAdapter.fromJson(data)
                    if (chunk != null) {
                        emit(SseResult.Data(chunk))
                    }
                } catch (e: Exception) {
                    // 跳过无法解析的行（某些厂商可能在 data 行中发送非 JSON 内容）
                    emit(SseResult.ParseError("解析失败: ${e.message}"))
                }
            }
        } catch (e: IOException) {
            emit(SseResult.StreamError("流中断: ${e.message}"))
            throw e
        } finally {
            reader.close()
            body.close()
        }
    }
}

/**
 * SSE 解析结果密封类
 */
sealed class SseResult {
    /** 成功解析的数据块 */
    data class Data(val chunk: ChatStreamChunk) : SseResult()

    /** 流结束标记 */
    data object Done : SseResult()

    /** 解析错误（非致命，可继续） */
    data class ParseError(val message: String) : SseResult()

    /** 流中断错误（致命） */
    data class StreamError(val message: String) : SseResult()
}

/**
 * 将 SseResult Flow 转换为纯文本内容 Flow
 *
 * 只提取 delta.content，过滤其他事件
 */
fun Flow<SseResult>.toContentFlow(): Flow<String> = flow {
    collect { result ->
        when (result) {
            is SseResult.Data -> {
                result.chunk.choices.forEach { choice ->
                    choice.delta.content?.let { content ->
                        emit(content)
                    }
                }
            }
            is SseResult.Done -> { /* 流结束 */ }
            is SseResult.ParseError -> { /* 跳过 */ }
            is SseResult.StreamError -> throw IOException(result.message)
        }
    }
}
