package com.buwang.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buwang.app.domain.model.Message
import com.buwang.app.domain.repository.ChatRepository
import com.buwang.app.domain.repository.PersonaRepository
import com.buwang.app.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 聊天主界面 ViewModel
 *
 * 职责：
 * - 加载会话消息列表
 * - 管理输入框状态
 * - 调用 ChatRepository 发送消息（流式）
 * - 局部更新正在生成中的助手消息
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val personaRepository: PersonaRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentConversationId: String? = null
    private var currentPersonaId: String? = null

    /**
     * 初始化指定角色与会话的聊天界面
     */
    fun initChat(personaId: String, conversationId: String?) {
        currentPersonaId = personaId
        viewModelScope.launch {
            // 加载角色信息
            personaRepository.getPersonaById(personaId).collect { persona ->
                persona ?: return@collect
                _uiState.value = _uiState.value.copy(personaName = persona.name)

                // 确定会话
                val convId = conversationId ?: chatRepository.createConversation(
                    personaId, "与${persona.name}的对话"
                ).also { currentConversationId = it }

                if (currentConversationId == null) currentConversationId = convId

                // 加载消息
                chatRepository.getMessagesForConversation(convId).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 更新输入框文本
     */
    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    /**
     * 发送消息（流式）
     */
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        val convId = currentConversationId ?: return
        val personaId = currentPersonaId ?: return

        _uiState.value = _uiState.value.copy(inputText = "", isSending = true)

        viewModelScope.launch {
            try {
                // 获取角色 System Prompt
                val persona = personaRepository.getPersonaById(personaId).first()
                val systemPrompt = persona?.systemPromptTemplate
                    ?: "你是一个友善的AI助手。"

                // 获取用户设置（模型 + Key）
                val settings = userSettingsRepository.getSettingsOnce()
                val modelKey = settings?.selectedModel ?: "tokens-box"
                val apiKey = userSettingsRepository.getEffectiveApiKey()

                // 历史消息（取最近20条）
                val history = _uiState.value.messages.takeLast(20)

                // 流式发送
                val builder = StringBuilder()
                chatRepository.sendMessage(
                    modelKey = modelKey,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    conversationId = convId,
                    userMessage = text,
                    historyMessages = history
                ).collect { token ->
                    builder.append(token)
                    // 局部更新UI状态中的"正在生成"消息
                    appendStreamingMessage(builder.toString())
                }

                _uiState.value = _uiState.value.copy(isSending = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "发送失败"
                )
            }
        }
    }

    /**
     * 将流式内容追加到消息列表末尾（临时助手消息）
     */
    private fun appendStreamingMessage(content: String) {
        val current = _uiState.value.messages.toMutableList()
        val last = current.lastOrNull()
        if (last != null && last.role == "assistant" && last.id.isEmpty()) {
            current[current.lastIndex] = last.copy(content = content)
        } else {
            current.add(
                Message(
                    id = "",  // 空ID表示流式临时消息
                    conversationId = currentConversationId ?: "",
                    role = "assistant",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        _uiState.value = _uiState.value.copy(messages = current)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 聊天界面 UI 状态
 */
data class ChatUiState(
    val personaName: String = "",
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)
