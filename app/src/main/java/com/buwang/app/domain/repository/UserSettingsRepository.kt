package com.buwang.app.domain.repository

import com.buwang.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun getSettings(): Flow<UserSettings?>
    suspend fun getSettingsOnce(): UserSettings?
    suspend fun initializeDefaultSettings()
    suspend fun updateModel(modelKey: String)
    suspend fun switchApiKeyMode(mode: String)
    suspend fun setCustomApiKey(apiKey: String)
    suspend fun clearCustomApiKey()
    suspend fun updateUserPersona(userPersonaDesc: String?)
    /**
     * 解析当前生效的 API Key
     *
     * 自定义模式且已配置时解密返回；否则回退到内置 Key（Tokens-Box 联调 Key）。
     * 用于对话链路真正发起请求时注入鉴权头。
     */
    suspend fun getEffectiveApiKey(): String
}
