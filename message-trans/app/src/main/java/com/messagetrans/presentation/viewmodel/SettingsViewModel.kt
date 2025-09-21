package com.messagetrans.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.data.database.entities.SimConfig
import com.messagetrans.data.repository.EmailRepositoryImpl
import com.messagetrans.data.repository.SimRepositoryImpl
import com.messagetrans.utils.EmailConfigCache
import com.messagetrans.utils.RuntimeLogger
import com.messagetrans.utils.SimCardManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.util.Log

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val emailRepository = EmailRepositoryImpl(
        (application as MessageTransApplication).database.emailConfigDao()
    )
    
    private val simRepository = SimRepositoryImpl(
        (application as MessageTransApplication).database.simConfigDao()
    )
    
    val emailConfigs: LiveData<List<EmailConfig>> = emailRepository.getAllEmailConfigs().asLiveData()
    val simConfigs: LiveData<List<SimConfig>> = simRepository.getAllSimConfigs().asLiveData()
    
    init {
        initializeSimConfigs()
        initializeEmailConfigs()
    }
    
    fun addEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                emailRepository.insertEmailConfig(emailConfig)
                syncEmailConfigsToCache()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun updateEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                emailRepository.updateEmailConfig(emailConfig)
                syncEmailConfigsToCache()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun deleteEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                emailRepository.deleteEmailConfig(emailConfig)
                syncEmailConfigsToCache()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun toggleEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                val updatedConfig = emailConfig.copy(isEnabled = !emailConfig.isEnabled)
                emailRepository.updateEmailConfig(updatedConfig)
                syncEmailConfigsToCache()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun updateSimConfig(simConfig: SimConfig) {
        viewModelScope.launch {
            try {
                simRepository.updateSimConfig(simConfig)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun toggleSimConfig(simConfig: SimConfig) {
        viewModelScope.launch {
            try {
                val updatedConfig = simConfig.copy(isEnabled = !simConfig.isEnabled)
                simRepository.updateSimConfig(updatedConfig)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    private fun initializeSimConfigs() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing SIM configs")
                val context = getApplication<Application>()
                
                // 获取真实的SIM卡信息
                val realSimCards = SimCardManager.getAllSimCards(context)
                Log.d(TAG, "Found ${realSimCards.size} SIM cards: $realSimCards")
                
                // 获取现有配置
                val existingConfigs = mutableListOf<SimConfig>()
                simConfigs.value?.let { existingConfigs.addAll(it) }
                
                // 检查是否需要更新SIM卡配置
                if (realSimCards.isNotEmpty()) {
                    // 清除现有配置（如果有的话）
                    for (config in existingConfigs) {
                        simRepository.deleteSimConfig(config)
                    }
                    
                    // 添加真实的SIM卡配置
                    for (simCard in realSimCards) {
                        val simConfig = SimCardManager.convertToSimConfig(simCard)
                        simRepository.insertSimConfig(simConfig)
                        Log.d(TAG, "Added SIM config: $simConfig")
                    }
                } else {
                    // 如果没有检测到SIM卡，创建默认配置（为了测试）
                    if (existingConfigs.isEmpty()) {
                        Log.d(TAG, "No SIM cards detected, creating default configs")
                        val defaultSim1 = SimConfig(
                            slotIndex = 0,
                            displayName = "SIM卡 1",
                            carrierName = "未知运营商",
                            phoneNumber = null,
                            isEnabled = true,
                            simType = "physical"
                        )
                        
                        val defaultSim2 = SimConfig(
                            slotIndex = 1,
                            displayName = "SIM卡 2",
                            carrierName = "未知运营商",
                            phoneNumber = null,
                            isEnabled = false,
                            simType = "physical"
                        )
                        
                        simRepository.insertSimConfig(defaultSim1)
                        simRepository.insertSimConfig(defaultSim2)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SIM configs", e)
            }
        }
    }
    
    /**
     * 刷新SIM卡配置（检测SIM卡变化）
     */
    fun refreshSimConfigs() {
        initializeSimConfigs()
    }
    
    /**
     * 初始化邮件配置（从缓存恢复）
     */
    private fun initializeEmailConfigs() {
        viewModelScope.launch {
            try {
                // 检查数据库中是否已有邮件配置
                val existingCount = emailRepository.getEmailConfigCount()
                
                // 如果数据库为空且有缓存，尝试从缓存恢复
                if (existingCount == 0 && EmailConfigCache.hasCachedConfigs()) {
                    val cachedConfigs = EmailConfigCache.loadEmailConfigs()
                    if (cachedConfigs.isNotEmpty()) {
                        Log.d(TAG, "从缓存恢复邮件配置: ${cachedConfigs.size} 个")
                        RuntimeLogger.logInfo(TAG, "从缓存恢复邮件配置", "配置数量: ${cachedConfigs.size}")
                        
                        // 将缓存的配置添加到数据库
                        for (config in cachedConfigs) {
                            emailRepository.insertEmailConfig(config)
                        }
                    }
                } else {
                    Log.d(TAG, "数据库中已有 $existingCount 个邮件配置，跳过缓存恢复")
                }
            } catch (e: Exception) {
                RuntimeLogger.logError(TAG, "初始化邮件配置失败", e)
                Log.e(TAG, "Error initializing email configs", e)
            }
        }
    }
    
    /**
     * 同步邮件配置到缓存
     */
    private suspend fun syncEmailConfigsToCache() {
        try {
            val currentConfigs = emailRepository.getAllEmailConfigs().first()
            EmailConfigCache.saveEmailConfigs(currentConfigs)
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "同步邮件配置到缓存失败", e)
            Log.e(TAG, "Error syncing email configs to cache", e)
        }
    }
    
    /**
     * 导出邮件配置
     */
    fun exportEmailConfigs(): String? {
        return EmailConfigCache.exportConfigsToString()
    }
    
    /**
     * 导入邮件配置
     */
    fun importEmailConfigs(importString: String): Boolean {
        viewModelScope.launch {
            if (EmailConfigCache.importConfigsFromString(importString)) {
                // 重新加载到数据库
                initializeEmailConfigs()
            }
        }
        return EmailConfigCache.importConfigsFromString(importString)
    }
}