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
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val emailRepository = EmailRepositoryImpl(
        (application as MessageTransApplication).database.emailConfigDao()
    )
    
    private val simRepository = SimRepositoryImpl(
        application.database.simConfigDao()
    )
    
    val emailConfigs: LiveData<List<EmailConfig>> = emailRepository.getAllEmailConfigs().asLiveData()
    val simConfigs: LiveData<List<SimConfig>> = simRepository.getAllSimConfigs().asLiveData()
    
    fun addEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                emailRepository.insertEmailConfig(emailConfig)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun updateEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                emailRepository.updateEmailConfig(emailConfig)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun deleteEmailConfig(emailConfig: EmailConfig) {
        viewModelScope.launch {
            try {
                emailRepository.deleteEmailConfig(emailConfig)
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
    
    fun initializeSimConfigs() {
        viewModelScope.launch {
            try {
                // 检查是否已经初始化SIM卡配置
                val existingConfigs = simRepository.getAllSimConfigs()
                
                // 如果没有配置，创建默认配置
                // 这里简化处理，实际应用中需要检测真实的SIM卡信息
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
                
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}