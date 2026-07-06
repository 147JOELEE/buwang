package com.buwang.app.di

import com.buwang.app.data.repository.ChatRepositoryImpl
import com.buwang.app.data.repository.PersonaRepositoryImpl
import com.buwang.app.data.repository.UserSettingsRepositoryImpl
import com.buwang.app.domain.repository.ChatRepository
import com.buwang.app.domain.repository.PersonaRepository
import com.buwang.app.domain.repository.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPersonaRepository(impl: PersonaRepositoryImpl): PersonaRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(impl: UserSettingsRepositoryImpl): UserSettingsRepository
}
