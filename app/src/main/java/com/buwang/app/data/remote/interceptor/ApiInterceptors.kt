package com.buwang.app.data.remote.interceptor

import com.buwang.app.BuildConfig
import com.buwang.app.core.crypto.CryptoManager
import com.buwang.app.data.local.dao.UserSettingsDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * API Key 注入拦截器
 *
 * 在每次请求前，根据用户设置（内置/自定义模式）自动注入对应的 API Key。
 * 自定义 Key 从加密存储中解密后使用。
 *
 * 注意：此拦截器在 OkHttp 层工作，因此在 Retrofit 的 Authorization header 之前执行。
 * 使用时需要在 NetworkModule 中添加到 OkHttpClient 的拦截器链首位。
 */
class ApiKeyInterceptor(
    private val userSettingsDao: UserSettingsDao,
    private val cryptoManager: CryptoManager
) : Interceptor {

    companion object {
        /** 内置 Key 统一引用 BuildConfig（构建期从 local.properties 注入），避免多处硬编码 */
        private val BUILTIN_KEY = BuildConfig.BUILTIN_API_KEY
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 如果已有 Authorization header，跳过注入
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

                val apiKey = runBlocking {
            try {
                val settings = userSettingsDao.getSettings().first()
                when (settings?.apiKeyMode) {
                    "custom" -> {
                        val encrypted = settings.customApiKeyEncrypted
                        if (encrypted != null) {
                            cryptoManager.decrypt(encrypted)
                        } else {
                            BUILTIN_KEY
                        }
                    }
                    else -> BUILTIN_KEY
                }
            } catch (e: Exception) {
                BUILTIN_KEY
            }
        }

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return chain.proceed(newRequest)
    }
}

/**
 * 错误处理拦截器
 *
 * 统一处理 API 错误响应：
 * - 401 Unauthorized: API Key 无效或过期
 * - 429 Too Many Requests: 速率限制
 * - 5xx: 服务端错误
 * - 网络超时/连接失败
 */
class ErrorHandlingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            val response = chain.proceed(request)

            when (response.code) {
                401 -> throw ApiException.Unauthorized("API Key 无效或已过期，请检查设置")
                429 -> throw ApiException.RateLimited("请求过于频繁，请稍后再试")
                500 -> throw ApiException.ServerError("服务暂时不可用，请稍后重试")
                502, 503, 504 -> throw ApiException.ServerError("模型服务繁忙，正在为您自动重试...")
                else -> response
            }
        } catch (e: java.net.SocketTimeoutException) {
            throw ApiException.Timeout("请求超时，请检查网络连接")
        } catch (e: java.net.UnknownHostException) {
            throw ApiException.NetworkError("无法连接到服务器，请检查网络")
        } catch (e: java.io.IOException) {
            if (e.message?.contains("timeout") == true) {
                throw ApiException.Timeout("请求超时，请检查网络连接")
            }
            throw ApiException.NetworkError("网络异常: ${e.message}")
        }
    }
}

/**
 * API 异常类型
 */
sealed class ApiException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause) {

    /** 用户可读的错误提示 */
    abstract val userMessage: String

    class Unauthorized(override val userMessage: String) : ApiException(userMessage)
    class RateLimited(override val userMessage: String) : ApiException(userMessage)
    class ServerError(override val userMessage: String) : ApiException(userMessage)
    class Timeout(override val userMessage: String) : ApiException(userMessage)
    class NetworkError(override val userMessage: String) : ApiException(userMessage)
    class Unknown(override val userMessage: String) : ApiException(userMessage)
}
