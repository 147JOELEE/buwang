package com.buwang.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.buwang.app.data.local.dao.ConversationDao
import com.buwang.app.data.local.dao.MessageDao
import com.buwang.app.data.local.dao.PersonaDao
import com.buwang.app.data.local.dao.UserSettingsDao
import com.buwang.app.data.local.entity.ConversationEntity
import com.buwang.app.data.local.entity.MessageEntity
import com.buwang.app.data.local.entity.PersonaEntity
import com.buwang.app.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        PersonaEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BuWangDatabase : RoomDatabase() {

    abstract fun personaDao(): PersonaDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        const val DATABASE_NAME = "buwang_database.db"
    }
}
