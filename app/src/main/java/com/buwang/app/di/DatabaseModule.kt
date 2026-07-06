package com.buwang.app.di

import android.content.Context
import androidx.room.Room
import com.buwang.app.data.local.dao.ConversationDao
import com.buwang.app.data.local.dao.MessageDao
import com.buwang.app.data.local.dao.PersonaDao
import com.buwang.app.data.local.dao.UserSettingsDao
import com.buwang.app.data.local.database.BuWangDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BuWangDatabase {
        return Room.databaseBuilder(
            context,
            BuWangDatabase::class.java,
            BuWangDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePersonaDao(database: BuWangDatabase): PersonaDao {
        return database.personaDao()
    }

    @Provides
    fun provideConversationDao(database: BuWangDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: BuWangDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideUserSettingsDao(database: BuWangDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }
}
