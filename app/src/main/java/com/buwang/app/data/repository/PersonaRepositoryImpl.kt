package com.buwang.app.data.repository

import com.buwang.app.core.personality.BigFiveParams
import com.buwang.app.core.personality.PresetCharacters
import com.buwang.app.data.local.dao.PersonaDao
import com.buwang.app.data.local.entity.PersonaEntity
import com.buwang.app.domain.model.Persona
import com.buwang.app.domain.model.PersonaSourceType
import com.buwang.app.domain.repository.PersonaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonaRepositoryImpl @Inject constructor(
    private val personaDao: PersonaDao
) : PersonaRepository {

    override fun getAllPersonas(): Flow<List<Persona>> {
        return personaDao.getAllPersonas().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getPersonaById(id: String): Flow<Persona?> {
        return personaDao.getPersonaById(id).map { it?.toDomainModel() }
    }

    override suspend fun createPersona(persona: Persona): String {
        val now = System.currentTimeMillis()
        val id = persona.id.ifEmpty { UUID.randomUUID().toString() }
        val entity = PersonaEntity(
            id = id,
            name = persona.name,
            avatarPath = persona.avatarPath,
            description = persona.description,
            sourceType = persona.sourceType,
            sourceMbtiType = persona.sourceMbtiType,
            personalityParams = persona.personalityParams,
            systemPromptTemplate = persona.systemPromptTemplate,
            createdAt = now,
            updatedAt = now
        )
        personaDao.insert(entity)
        return id
    }

    override suspend fun updatePersona(persona: Persona) {
        personaDao.getPersonaByIdOnce(persona.id)?.let { existing ->
            val entity = PersonaEntity(
                id = persona.id,
                name = persona.name,
                avatarPath = persona.avatarPath,
                description = persona.description,
                sourceType = persona.sourceType,
                sourceMbtiType = persona.sourceMbtiType,
                personalityParams = persona.personalityParams,
                systemPromptTemplate = persona.systemPromptTemplate,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            personaDao.update(entity)
        }
    }

    override suspend fun deletePersona(id: String) {
        personaDao.deleteById(id)
    }

    override suspend fun getPersonaCount(): Int {
        return personaDao.getCount()
    }

    override suspend fun seedDefaultPersonaIfNeeded() {
        if (personaDao.getCount() > 0) return
        val preset = PresetCharacters.getDefault()
        val now = System.currentTimeMillis()
        personaDao.insert(
            PersonaEntity(
                id = "preset_default",
                name = preset.name,
                avatarPath = null,
                description = preset.description,
                sourceType = PersonaSourceType.PRESET,
                sourceMbtiType = null,
                personalityParams = preset.params.toJson(),
                systemPromptTemplate = preset.systemPromptTemplate,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}

/**
 * Entity → Domain Model 映射
 */
private fun PersonaEntity.toDomainModel(): Persona {
    return Persona(
        id = id,
        name = name,
        avatarPath = avatarPath,
        description = description,
        sourceType = sourceType,
        sourceMbtiType = sourceMbtiType,
        personalityParams = personalityParams,
        systemPromptTemplate = systemPromptTemplate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
