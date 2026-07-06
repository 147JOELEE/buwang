package com.buwang.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.buwang.app.data.local.entity.PersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {

    @Query("SELECT * FROM personas ORDER BY updated_at DESC")
    fun getAllPersonas(): Flow<List<PersonaEntity>>

    @Query("SELECT * FROM personas WHERE id = :personaId")
    fun getPersonaById(personaId: String): Flow<PersonaEntity?>

    @Query("SELECT * FROM personas WHERE id = :personaId")
    suspend fun getPersonaByIdOnce(personaId: String): PersonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(persona: PersonaEntity)

    @Update
    suspend fun update(persona: PersonaEntity)

    @Query("DELETE FROM personas WHERE id = :personaId")
    suspend fun deleteById(personaId: String)

    @Query("SELECT COUNT(*) FROM personas")
    suspend fun getCount(): Int
}
