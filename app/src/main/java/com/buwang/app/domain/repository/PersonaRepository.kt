package com.buwang.app.domain.repository

import com.buwang.app.domain.model.Persona
import kotlinx.coroutines.flow.Flow

interface PersonaRepository {
    fun getAllPersonas(): Flow<List<Persona>>
    fun getPersonaById(id: String): Flow<Persona?>
    suspend fun createPersona(persona: Persona): String
    suspend fun updatePersona(persona: Persona)
    suspend fun deletePersona(id: String)
    suspend fun getPersonaCount(): Int

    /**
     * 首次启动时播种默认角色（小暖）。
     * 仅当角色表为空时插入，使用固定 ID "preset_default" 以便主页默认会话直接命中。
     */
    suspend fun seedDefaultPersonaIfNeeded()
}
