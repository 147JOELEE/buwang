package com.buwang.app.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.proto.KeyTemplate
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导出导入密码异常
 */
class ExportException(message: String) : Exception(message)

/**
 * 加密管理器 — 封装 Android Keystore + Tink AEAD 加密/解密
 *
 * 职责：
 * - API Key 的加密存储与解密读取
 * - 导出文件的加密与解密
 * - 敏感数据保护
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEY_STORE_NAME = "buwang_keystore"
        private const val MASTER_KEY_URI = "android-keystore://buwang_master_key"
        private const val KEYSET_NAME = "buwang_aead_keyset"
        private const val PREF_FILE_NAME = "buwang_crypto_prefs"
    }

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(getApplicationContext(), KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(com.google.crypto.tink.aead.AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    /**
     * 加密字符串
     * @param plaintext 明文
     * @param associatedData 关联数据（可选），用于绑定加密上下文
     * @return Base64 编码的密文
     */
    fun encrypt(plaintext: String, associatedData: ByteArray = ByteArray(0)): String {
        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), associatedData)
        return android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
    }

    /**
     * 解密字符串
     * @param encryptedBase64 Base64 编码的密文
     * @param associatedData 关联数据，必须与加密时一致
     * @return 解密后的明文
     */
    fun decrypt(encryptedBase64: String, associatedData: ByteArray = ByteArray(0)): String {
        val ciphertext = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
        val plaintext = aead.decrypt(ciphertext, associatedData)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * 加密字节数组（用于文件加密）
     */
    fun encryptBytes(data: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead.encrypt(data, associatedData)
    }

    /**
     * 解密字节数组（用于文件解密）
     */
    fun decryptBytes(data: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead.decrypt(data, associatedData)
    }

    /**
     * 基于密码的加密（用于导出文件）
     *
     * 使用 PBKDF2 从密码派生密钥，然后用 AES-256-GCM 加密数据。
     * 输出格式：[salt(16B)] | [iv(12B)] | [ciphertext] | [tag(16B)]
     *
     * @param password 用户设置的密码
     * @param plaintext 明文数据
     * @return 加密后的字节数组（包含 salt 和 iv）
     */
    fun encryptWithPassword(password: String, plaintext: ByteArray): ByteArray {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)

        // 组合: salt + iv + ciphertext
        return salt + iv + ciphertext
    }

    /**
     * 基于密码的解密（用于导入文件）
     *
     * @param password 用户设置的密码
     * @param encrypted 加密数据（包含 salt + iv + ciphertext）
     * @return 解密后的明文数据
     * @throws ExportException 密码错误时抛出
     */
    fun decryptWithPassword(password: String, encrypted: ByteArray): ByteArray {
        return try {
            val salt = encrypted.copyOfRange(0, 16)
            val iv = encrypted.copyOfRange(16, 28)
            val ciphertext = encrypted.copyOfRange(28, encrypted.size)

            val key = deriveKey(password, salt)
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(ciphertext)
        } catch (e: javax.crypto.AEADBadTagException) {
            throw ExportException("密码错误，无法解密备份文件")
        } catch (e: Exception) {
            throw ExportException("解密失败: ${e.message}")
        }
    }

    /**
     * 从密码派生 AES 密钥（PBKDF2）
     */
    private fun deriveKey(password: String, salt: ByteArray): javax.crypto.SecretKeySpec {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            100000,  // 迭代次数
            256      // 密钥长度
        )
        val secretKey = factory.generateSecret(spec)
        return javax.crypto.spec.SecretKeySpec(secretKey.encoded, "AES")
    }

    /**
     * 生成 Android Keystore 主密钥（需要在 Application 初始化时调用）
     */
    fun generateMasterKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME)
        keyStore.load(null)
        if (!keyStore.containsAlias(MASTER_KEY_URI)) {
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_URI,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEY_STORE_NAME
            )
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getApplicationContext(): android.content.Context {
        // 使用反射避免循环依赖，实际项目中由 Hilt 注入 Application Context
        return Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as android.content.Context
    }
}
