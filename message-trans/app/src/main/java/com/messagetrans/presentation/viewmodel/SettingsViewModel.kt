package com.messagetrans.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.data.database.entities.SimConfig
import com.messagetrans.data.repository.EmailRepositoryImpl
import com.messagetrans.data.repository.SimRepositoryImpl
import com.messagetrans.service.email.EmailService
import com.messagetrans.utils.EmailConfigCache
import com.messagetrans.utils.RuntimeLogger
import com.messagetrans.utils.SimCardManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    
    // 当前正在编辑的邮件配置（用于代理设置）
    private val _currentEmailConfig = MutableLiveData<EmailConfig?>()
    val currentEmailConfig: LiveData<EmailConfig?> = _currentEmailConfig
    
    private val emailService = EmailService
    
    init {
        initializeSimConfigs()
        initializeEmailConfigs()
        loadCurrentEmailConfig()
    }
    
    fun addEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                emailRepository.insertEmailConfig(emailConfig)
                syncEmailConfigsToCache()
                // 如果是第一个配置，设置为当前配置
                if (_currentEmailConfig.value == null) {
                    _currentEmailConfig.value = emailConfig
                }
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
                // 如果更新的是当前配置，更新当前配置
                if (_currentEmailConfig.value?.id == emailConfig.id) {
                    _currentEmailConfig.value = emailConfig
                }
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
    
    /**
     * 加载当前的邮件配置（用于代理设置）
     */
    private fun loadCurrentEmailConfig() {
        viewModelScope.launch {
            try {
                val configs = emailRepository.getAllEmailConfigs().first()
                // 获取第一个启用的配置，如果没有则获取第一个配置
                val config = configs.firstOrNull { it.isEnabled } ?: configs.firstOrNull()
                _currentEmailConfig.value = config
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current email config", e)
            }
        }
    }
    
    /**
     * 更新代理启用状态
     */
    fun updateProxyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _currentEmailConfig.value?.let { config ->
                    val updatedConfig = config.copy(useProxy = enabled)
                    emailRepository.updateEmailConfig(updatedConfig)
                    _currentEmailConfig.value = updatedConfig
                    syncEmailConfigsToCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating proxy enabled", e)
            }
        }
    }
    
    /**
     * 更新代理类型
     */
    fun updateProxyType(proxyType: String) {
        viewModelScope.launch {
            try {
                _currentEmailConfig.value?.let { config ->
                    val updatedConfig = config.copy(proxyType = proxyType)
                    emailRepository.updateEmailConfig(updatedConfig)
                    _currentEmailConfig.value = updatedConfig
                    syncEmailConfigsToCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating proxy type", e)
            }
        }
    }
    
    /**
     * 更新代理主机
     */
    fun updateProxyHost(host: String) {
        viewModelScope.launch {
            try {
                _currentEmailConfig.value?.let { config ->
                    val updatedConfig = config.copy(proxyHost = host)
                    emailRepository.updateEmailConfig(updatedConfig)
                    _currentEmailConfig.value = updatedConfig
                    syncEmailConfigsToCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating proxy host", e)
            }
        }
    }
    
    /**
     * 更新代理端口
     */
    fun updateProxyPort(port: Int) {
        viewModelScope.launch {
            try {
                _currentEmailConfig.value?.let { config ->
                    val updatedConfig = config.copy(proxyPort = port)
                    emailRepository.updateEmailConfig(updatedConfig)
                    _currentEmailConfig.value = updatedConfig
                    syncEmailConfigsToCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating proxy port", e)
            }
        }
    }
    
    /**
     * 更新代理用户名
     */
    fun updateProxyUsername(username: String) {
        viewModelScope.launch {
            try {
                _currentEmailConfig.value?.let { config ->
                    val updatedConfig = config.copy(proxyUsername = username)
                    emailRepository.updateEmailConfig(updatedConfig)
                    _currentEmailConfig.value = updatedConfig
                    syncEmailConfigsToCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating proxy username", e)
            }
        }
    }
    
    /**
     * 更新代理密码
     */
    fun updateProxyPassword(password: String) {
        viewModelScope.launch {
            try {
                _currentEmailConfig.value?.let { config ->
                    val updatedConfig = config.copy(proxyPassword = password)
                    emailRepository.updateEmailConfig(updatedConfig)
                    _currentEmailConfig.value = updatedConfig
                    syncEmailConfigsToCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating proxy password", e)
            }
        }
    }
    
    /**
     * 保存代理配置
     */
    fun saveProxyConfig(enabled: Boolean, proxyType: String, host: String, port: Int, username: String, password: String) {
        viewModelScope.launch {
            try {
                _currentEmailConfig.value?.let { config ->
                    val updatedConfig = config.copy(
                        useProxy = enabled,
                        proxyType = proxyType,
                        proxyHost = host,
                        proxyPort = port,
                        proxyUsername = username,
                        proxyPassword = password
                    )
                    emailRepository.updateEmailConfig(updatedConfig)
                    _currentEmailConfig.value = updatedConfig
                    syncEmailConfigsToCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving proxy config", e)
            }
        }
    }
    
    /**
     * 测试代理连接
     */
    suspend fun testProxyConnection(): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                _currentEmailConfig.value?.let { config ->
                    if (!config.useProxy || config.proxyHost.isEmpty()) {
                        return@withContext Pair(false, "代理未启用或代理服务器地址为空")
                    }
                    
                    val result = emailService.testProxyConnection(config)
                    if (result.success) {
                        RuntimeLogger.logInfo(TAG, "代理连接测试成功", "代理类型: ${config.proxyType}, 地址: ${config.proxyHost}:${config.proxyPort}")
                        Pair(true, "代理连接测试成功！\n\n代理类型: ${config.proxyType}\n地址: ${config.proxyHost}:${config.proxyPort}")
                    } else {
                        RuntimeLogger.logWarn(TAG, "代理连接测试失败", "代理类型: ${config.proxyType}, 地址: ${config.proxyHost}:${config.proxyPort}")
                        Pair(false, result.errorMessage ?: "代理连接测试失败，请检查代理配置")
                    }
                } ?: Pair(false, "没有找到邮件配置")
            } catch (e: Exception) {
                RuntimeLogger.logError(TAG, "代理连接测试异常", e)
                Pair(false, "代理连接测试异常: ${e.message}")
            }
        }
    }
}