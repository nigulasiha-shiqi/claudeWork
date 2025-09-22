package com.messagetrans.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.database.entities.RuntimeLog
import com.messagetrans.data.database.entities.SmsLog
import com.messagetrans.data.repository.SmsRepositoryImpl
import com.messagetrans.service.sms.EmailSendingWorker
import com.messagetrans.utils.RuntimeLogger
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val smsRepository = SmsRepositoryImpl(
        (application as MessageTransApplication).database.smsLogDao()
    )
    
    private val runtimeLogDao = (application as MessageTransApplication).database.runtimeLogDao()
    
    val smsLogs: LiveData<List<SmsLog>> = smsRepository.getAllSmsLogs().asLiveData()
    
    private val allRuntimeLogs: LiveData<List<RuntimeLog>> = runtimeLogDao.getAllRuntimeLogs().asLiveData()
    private val _filteredRuntimeLogs = MutableLiveData<List<RuntimeLog>>()
    val runtimeLogs: LiveData<List<RuntimeLog>> = _filteredRuntimeLogs
    
    private var currentLogFilter = "ALL" // ALL, INFO, WARN, ERROR
    
    init {
        // 监听所有运行日志的变化，并应用当前筛选器
        allRuntimeLogs.observeForever { logs ->
            applyLogFilter(logs)
        }
    }
    
    fun clearAllLogs() {
        viewModelScope.launch {
            try {
                // 删除7天前的SMS日志
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                smsRepository.deleteOldLogs(sevenDaysAgo)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun clearAllRuntimeLogs() {
        viewModelScope.launch {
            try {
                runtimeLogDao.clearAllRuntimeLogs()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun deleteSmsLog(smsLog: SmsLog) {
        viewModelScope.launch {
            try {
                smsRepository.deleteSmsLog(smsLog)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun searchLogsByPhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            try {
                smsRepository.getSmsLogsByPhoneNumber("%$phoneNumber%")
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun getLogsByDateRange(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            try {
                smsRepository.getSmsLogsByDateRange(startTime, endTime)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun updateSmsLog(smsLog: SmsLog) {
        viewModelScope.launch {
            try {
                smsRepository.updateSmsLog(smsLog)
                RuntimeLogger.logInfo("LogsViewModel", "短信日志已更新", "ID: ${smsLog.id}")
            } catch (e: Exception) {
                RuntimeLogger.logError("LogsViewModel", "更新短信日志失败", e)
            }
        }
    }
    
    fun resendSms(smsLog: SmsLog) {
        viewModelScope.launch {
            try {
                // 重置转发状态
                val updatedLog = smsLog.copy(
                    isForwarded = false,
                    errorMessage = null
                )
                smsRepository.updateSmsLog(updatedLog)
                
                // 创建邮件发送任务
                val inputData = Data.Builder()
                    .putString("sms_log_id", smsLog.id)
                    .putString("phone_number", smsLog.phoneNumber)
                    .putString("content", smsLog.content)
                    .putLong("timestamp", smsLog.timestamp)
                    .putInt("sim_slot", smsLog.simSlot)
                    .putString("sim_type", smsLog.simType)
                    .build()

                val emailWorkRequest = OneTimeWorkRequestBuilder<EmailSendingWorker>()
                    .setInputData(inputData)
                    .addTag("email_resend_${smsLog.id}")
                    .build()

                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "email_resend_${smsLog.id}",
                    ExistingWorkPolicy.REPLACE, // 替换现有的重发任务
                    emailWorkRequest
                )
                
                RuntimeLogger.logInfo("LogsViewModel", "短信重新转发已启动", "手机号: ${smsLog.phoneNumber}")
            } catch (e: Exception) {
                RuntimeLogger.logError("LogsViewModel", "重新转发短信失败", e)
            }
        }
    }
    
    /**
     * 设置日志筛选器
     */
    fun setLogFilter(filter: String) {
        currentLogFilter = filter
        allRuntimeLogs.value?.let { logs ->
            applyLogFilter(logs)
        }
    }
    
    /**
     * 应用日志筛选
     */
    private fun applyLogFilter(logs: List<RuntimeLog>) {
        val filteredLogs = when (currentLogFilter) {
            "INFO" -> logs.filter { it.level == "INFO" }
            "WARN" -> logs.filter { it.level == "WARN" }
            "ERROR" -> logs.filter { it.level == "ERROR" }
            else -> logs // ALL
        }
        _filteredRuntimeLogs.value = filteredLogs
    }
    
    /**
     * 获取当前筛选器
     */
    fun getCurrentFilter(): String = currentLogFilter
}