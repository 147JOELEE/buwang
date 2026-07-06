package com.buwang.app

import android.app.Application
import com.buwang.app.core.crypto.CryptoManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BuWangApplication : Application() {

    @Inject
    lateinit var cryptoManager: CryptoManager

    override fun onCreate() {
        super.onCreate()
        // 初始化加密基础设施
        cryptoManager.generateMasterKeyIfNeeded()
    }
}
