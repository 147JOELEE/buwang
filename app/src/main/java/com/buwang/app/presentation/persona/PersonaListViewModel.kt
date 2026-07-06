package com.buwang.app.presentation.persona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buwang.app.domain.model.Persona
import com.buwang.app.domain.model.PersonaSourceType
import com.buwang.app.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 角色列表 ViewModel（侧边栏）
 */
@HiltViewModel
class PersonaListViewModel @Inject constructor(
    private val personaRepository: PersonaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonaListUiState())
    val uiState: StateFlow<PersonaListUiState> = _uiState.asStateFlow()

    init {
        loadPersonas()
    }

    fun loadPersonas() {
        viewModelScope.launch {
            personaRepository.seedDefaultPersonaIfNeeded()
            personaRepository.getAllPersonas().collect { personas ->
                _uiState.value = _uiState.value.copy(
                    personas = personas,
                    isLoading = false
                )
            }
        }
    }

    fun deletePersona(id: String) {
        viewModelScope.launch {
            personaRepository.deletePersona(id)
            // 列表会自动通过 Flow 更新
        }
    }

    /**
     * 创建预设角色（从 UX 架构师设计的预设库）
     */
    fun createPresetPersona(preset: PresetPersonaData) {
        viewModelScope.launch {
            personaRepository.createPersona(
                Persona(
                    name = preset.name,
                    description = preset.description,
                    sourceType = PersonaSourceType.PRESET,
                    personalityParams = preset.personalityParams,
                    systemPromptTemplate = preset.systemPrompt
                )
            )
        }
    }
}

/**
 * 预设角色数据（从 UX 设计库传入）
 */
data class PresetPersonaData(
    val name: String,
    val description: String,
    val personalityParams: String,
    val systemPrompt: String
)

/**
 * 角色列表 UI 状态
 */
data class PersonaListUiState(
    val personas: List<Persona> = emptyList(),
    val isLoading: Boolean = true,
    val selectedPersonaId: String? = null
)
