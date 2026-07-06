package com.buwang.app.data.repository

import com.buwang.app.BuildConfig
import com.buwang.app.core.crypto.CryptoManager
import com.buwang.app.data.local.dao.UserSettingsDao
import com.buwang.app.data.local.entity.UserSettingsEntity
import com.buwang.app.domain.model.UserSettings
import com.buwang.app.domain.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepositoryImpl @Inject constructor(
    private val userSettingsDao: UserSettingsDao,
    private val cryptoManager: CryptoManager
) : UserSettingsRepository {

    override fun getSettings(): Flow<UserSettings?> {
        return userSettingsDao.getSettings().map { it?.toDomainModel() }
    }

    override suspend fun getSettingsOnce(): UserSettings? {
        return userSettingsDao.getSettings().first()?.toDomainModel()
    }

    override suspend fun initializeDefaultSettings() {
        val existing = userSettingsDao.getSettings().first()
        if (existing == null) {
            userSettingsDao.insert(
                UserSettingsEntity(
                    id = 1,
                    userAvatarPath = null,
                    userPersonaDesc = null,
                    chatBackground = null,
                    selectedModel = "tokens-box",
                    apiKeyMode = "builtin",
                    customApiKeyEncrypted = null
                )
            )
        }
    }

    override suspend fun updateModel(modelKey: String) {
        val settings = userSettingsDao.getSettings().first()
        settings?.let {
            userSettingsDao.update(it.copy(selectedModel = modelKey))
        }
    }

    override suspend fun switchApiKeyMode(mode: String) {
        val settings = userSettingsDao.getSettings().first()
        settings?.let {
            userSettingsDao.update(it.copy(apiKeyMode = mode))
        }
    }

    override suspend fun setCustomApiKey(apiKey: String) {
        val settings = userSettingsDao.getSettings().first()
        settings?.let {
            val encrypted = cryptoManager.encrypt(apiKey)
            userSettingsDao.update(
                it.copy(
                    apiKeyMode = "custom",
                    customApiKeyEncrypted = encrypted
                )
            )
        }
    }

    override suspend fun clearCustomApiKey() {
        val settings = userSettingsDao.getSettings().first()
        settings?.let {
            userSettingsDao.update(
                it.copy(
                    apiKeyMode = "builtin",
                    customApiKeyEncrypted = null
                )
            )
        }
    }

    override suspend fun updateUserPersona(userPersonaDesc: String?) {
        val settings = userSettingsDao.getSettings().first()
        settings?.let {
            userSettingsDao.update(it.copy(userPersonaDesc = userPersonaDesc))
        }
    }

    override suspend fun getEffectiveApiKey(): String {
        val settings = userSettingsDao.getSettings().first()
        return when {
            settings?.apiKeyMode == "custom" &&
                !settings.customApiKeyEncrypted.isNullOrEmpty() -> {
                try {
                    cryptoManager.decrypt(settings.customApiKeyEncrypted)
                } catch (e: Exception) {
                    BuildConfig.BUILTIN_API_KEY
                }
            }
            else -> BuildConfig.BUILTIN_API_KEY
        }
    }
}

private fun UserSettingsEntity.toDomainModel(): UserSettings {
    return UserSettings(
        userAvatarPath = userAvatarPath,
        userPersonaDesc = userPersonaDesc,
        chatBackground = chatBackground,
        selectedModel = selectedModel,
        apiKeyMode = apiKeyMode,
        hasCustomApiKey = !customApiKeyEncrypted.isNullOrEmpty()
    )
}
