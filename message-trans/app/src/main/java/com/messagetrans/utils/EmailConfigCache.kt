package com.messagetrans.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.messagetrans.data.database.entities.EmailConfig
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * 邮件配置缓存管理器
 * 使用SharedPreferences和简单加密来持久化邮件配置
 * 即使重新安装应用也能保持配置
 */
object EmailConfigCache {
    
    private const val TAG = "EmailConfigCache"
    private const val PREFS_NAME = "email_config_cache"
    private const val KEY_EMAIL_CONFIGS = "cached_email_configs"
    private const val KEY_ENCRYPTION_KEY = "encryption_key"
    private const val CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    private var sharedPrefs: SharedPreferences? = null
    private var encryptionKey: SecretKey? = null
    
    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        initEncryptionKey()
    }
    
    /**
     * 初始化加密密钥
     */
    private fun initEncryptionKey() {
        val prefs = sharedPrefs ?: return
        
        val savedKey = prefs.getString(KEY_ENCRYPTION_KEY, null)
        
        encryptionKey = if (savedKey != null) {
            try {
                val keyBytes = Base64.decode(savedKey, Base64.DEFAULT)
                SecretKeySpec(keyBytes, "AES")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load encryption key, generating new one", e)
                generateAndSaveNewKey()
            }
        } else {
            generateAndSaveNewKey()
        }
    }
    
    /**
     * 生成并保存新的加密密钥
     */
    private fun generateAndSaveNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(128)
        val key = keyGenerator.generateKey()
        
        val keyBase64 = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        sharedPrefs?.edit()?.putString(KEY_ENCRYPTION_KEY, keyBase64)?.apply()
        
        return key
    }
    
    /**
     * 加密字符串
     */
    private fun encrypt(data: String): String? {
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
            val encryptedBytes = cipher.doFinal(data.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            null
        }
    }
    
    /**
     * 解密字符串
     */
    private fun decrypt(encryptedData: String): String? {
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            null
        }
    }
    
    /**
     * 保存邮件配置到缓存
     */
    fun saveEmailConfigs(emailConfigs: List<EmailConfig>) {
        try {
            val gson = Gson()
            val configsJson = gson.toJson(emailConfigs)
            
            val encryptedJson = encrypt(configsJson)
            if (encryptedJson != null) {
                sharedPrefs?.edit()?.putString(KEY_EMAIL_CONFIGS, encryptedJson)?.apply()
                RuntimeLogger.logInfo(TAG, "邮件配置已保存到缓存", "配置数量: ${emailConfigs.size}")
                Log.d(TAG, "Email configs saved to cache: ${emailConfigs.size} configs")
            } else {
                RuntimeLogger.logError(TAG, "邮件配置加密失败")
                Log.e(TAG, "Failed to encrypt email configs")
            }
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "保存邮件配置到缓存失败", e)
            Log.e(TAG, "Failed to save email configs to cache", e)
        }
    }
    
    /**
     * 从缓存加载邮件配置
     */
    fun loadEmailConfigs(): List<EmailConfig> {
        return try {
            val encryptedJson = sharedPrefs?.getString(KEY_EMAIL_CONFIGS, null)
            if (encryptedJson != null) {
                val configsJson = decrypt(encryptedJson)
                if (configsJson != null) {
                    val gson = Gson()
                    val type = object : TypeToken<List<EmailConfig>>() {}.type
                    val configs: List<EmailConfig> = gson.fromJson(configsJson, type) ?: emptyList()
                    
                    RuntimeLogger.logInfo(TAG, "从缓存加载邮件配置", "配置数量: ${configs.size}")
                    Log.d(TAG, "Email configs loaded from cache: ${configs.size} configs")
                    configs
                } else {
                    RuntimeLogger.logWarn(TAG, "邮件配置解密失败")
                    Log.w(TAG, "Failed to decrypt email configs")
                    emptyList()
                }
            } else {
                Log.d(TAG, "No cached email configs found")
                emptyList()
            }
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "从缓存加载邮件配置失败", e)
            Log.e(TAG, "Failed to load email configs from cache", e)
            emptyList()
        }
    }
    
    /**
     * 检查是否有缓存的配置
     */
    fun hasCachedConfigs(): Boolean {
        return sharedPrefs?.contains(KEY_EMAIL_CONFIGS) == true
    }
    
    /**
     * 清除缓存的配置
     */
    fun clearCache() {
        sharedPrefs?.edit()?.remove(KEY_EMAIL_CONFIGS)?.apply()
        RuntimeLogger.logInfo(TAG, "邮件配置缓存已清除")
        Log.d(TAG, "Email config cache cleared")
    }
    
    /**
     * 导出配置到字符串（用于备份）
     */
    fun exportConfigsToString(): String? {
        return try {
            val configs = loadEmailConfigs()
            if (configs.isNotEmpty()) {
                val gson = Gson()
                val exportData = mapOf(
                    "version" to 1,
                    "timestamp" to System.currentTimeMillis(),
                    "configs" to configs
                )
                val exportJson = gson.toJson(exportData)
                Base64.encodeToString(exportJson.toByteArray(), Base64.DEFAULT)
            } else {
                null
            }
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "导出邮件配置失败", e)
            Log.e(TAG, "Failed to export email configs", e)
            null
        }
    }
    
    /**
     * 从字符串导入配置（用于恢复备份）
     */
    fun importConfigsFromString(importString: String): Boolean {
        return try {
            val importJson = String(Base64.decode(importString, Base64.DEFAULT))
            val gson = Gson()
            val importData = gson.fromJson(importJson, Map::class.java) as? Map<String, Any> ?: return false
            
            val version = (importData["version"] as? Double)?.toInt() ?: 0
            if (version != 1) {
                RuntimeLogger.logError(TAG, "不支持的导入版本: $version")
                return false
            }
            
            val configsData = importData["configs"]
            val configsJson = gson.toJson(configsData)
            val type = object : TypeToken<List<EmailConfig>>() {}.type
            val configs: List<EmailConfig> = gson.fromJson(configsJson, type)
            
            saveEmailConfigs(configs)
            RuntimeLogger.logInfo(TAG, "邮件配置导入成功", "配置数量: ${configs.size}")
            true
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "导入邮件配置失败", e)
            Log.e(TAG, "Failed to import email configs", e)
            false
        }
    }
}