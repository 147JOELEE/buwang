package com.buwang.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buwang.app.data.remote.api.LlmApiFactory
import com.buwang.app.data.remote.dto.ChatMessage
import com.buwang.app.data.remote.dto.TestApiRequest
import com.buwang.app.domain.model.UserSettings
import com.buwang.app.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * API Key 管理 ViewModel
 *
 * 管理内置/自定义模式切换、Base URL + API Key 配置。
 */
@HiltViewModel
class ApiKeyViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val llmApiFactory: LlmApiFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeyUiState())
    val uiState: StateFlow<ApiKeyUiState> = _uiState.asStateFlow()

    /** 可用模型列表（标识, 显示名），从 LlmApiFactory 动态读取，保持单一数据源 */
    val availableModels: List<Pair<String, String>> =
        llmApiFactory.getAvailableModels().map { (key, config) -> key to config.displayName }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userSettingsRepository.getSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    apiKeyMode = settings?.apiKeyMode ?: "builtin",
                    selectedModel = settings?.selectedModel ?: LlmApiFactory.DEFAULT_MODEL,
                    hasCustomKey = settings?.hasCustomApiKey ?: false,
                    isLoading = false
                )
            }
        }
    }

    fun switchMode(mode: String) {
        viewModelScope.launch {
            userSettingsRepository.switchApiKeyMode(mode)
            _uiState.value = _uiState.value.copy(apiKeyMode = mode)
        }
    }

    /**
     * 保存自定义 API 配置（Base URL + Key）
     */
    fun saveCustomConfig(baseUrl: String, apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "API Key 不能为空")
            return
        }
        if (!baseUrl.endsWith("/")) {
            _uiState.value = _uiState.value.copy(error = "Base URL 需以 / 结尾")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                // 有效性验证（发送最小化测试请求）
                val valid = validateApiKey(baseUrl, apiKey)
                if (!valid) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "API Key 验证失败，请检查 Base URL 和 Key"
                    )
                    return@launch
                }
                userSettingsRepository.setCustomApiKey(apiKey)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasCustomKey = true,
                    customBaseUrl = baseUrl,
                    customApiKey = apiKey,
                    success = "配置已保存并生效"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    fun updateModel(modelKey: String) {
        viewModelScope.launch {
            userSettingsRepository.updateModel(modelKey)
            _uiState.value = _uiState.value.copy(selectedModel = modelKey)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(success = null)
    }

    /**
     * 验证 API Key 有效性（发送最小化测试请求）
     *
     * 通过 LlmApiFactory 构造指向自定义 Base URL 的实例，
     * 发送一次 max_tokens=5 的非流式请求，以真实联网方式校验 Key 与端点。
     */
    private suspend fun validateApiKey(baseUrl: String, apiKey: String): Boolean {
        return try {
            val api = llmApiFactory.create(
                modelKey = LlmApiFactory.MODEL_CUSTOM,
                apiKey = apiKey,
                customBaseUrl = baseUrl
            )
            // 自定义端点模型名未知：tokens-box 用 MiniMax-M2.7，其余用常见默认名兜底
            val testModel = if (baseUrl.contains("tokens-box", ignoreCase = true)) {
                "MiniMax-M2.7"
            } else {
                "default"
            }
            val response = api.testApiKey(
                TestApiRequest(
                    model = testModel,
                    messages = listOf(ChatMessage("user", "hi")),
                    maxTokens = 5,
                    stream = false
                )
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * API Key 管理 UI 状态
 */
data class ApiKeyUiState(
    val isLoading: Boolean = true,
    val apiKeyMode: String = "builtin",  // builtin | custom
    val selectedModel: String = "tokens-box",
    val hasCustomKey: Boolean = false,
    val customBaseUrl: String = "",
    val customApiKey: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null
)
