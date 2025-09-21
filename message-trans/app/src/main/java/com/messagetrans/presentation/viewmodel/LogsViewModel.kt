package com.messagetrans.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.database.entities.RuntimeLog
import com.messagetrans.data.database.entities.SmsLog
import com.messagetrans.data.repository.SmsRepositoryImpl
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val smsRepository = SmsRepositoryImpl(
        (application as MessageTransApplication).database.smsLogDao()
    )
    
    private val runtimeLogDao = (application as MessageTransApplication).database.runtimeLogDao()
    
    val smsLogs: LiveData<List<SmsLog>> = smsRepository.getAllSmsLogs().asLiveData()
    val runtimeLogs: LiveData<List<RuntimeLog>> = runtimeLogDao.getAllRuntimeLogs().asLiveData()
    
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
}