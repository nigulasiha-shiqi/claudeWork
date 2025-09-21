package com.messagetrans.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.repository.SmsRepositoryImpl
import com.messagetrans.service.sms.SmsMonitorService
import com.messagetrans.utils.PermissionManager
import kotlinx.coroutines.launch
import android.util.Log

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val smsRepository = SmsRepositoryImpl(
        (application as MessageTransApplication).database.smsLogDao()
    )
    
    private val _serviceStatus = MutableLiveData<Boolean>()
    val serviceStatus: LiveData<Boolean> = _serviceStatus
    
    private val _totalSmsCount = MutableLiveData<Int>()
    val totalSmsCount: LiveData<Int> = _totalSmsCount
    
    private val _forwardedSmsCount = MutableLiveData<Int>()
    val forwardedSmsCount: LiveData<Int> = _forwardedSmsCount
    
    private val _permissionStatus = MutableLiveData<Boolean>()
    val permissionStatus: LiveData<Boolean> = _permissionStatus
    
    init {
        loadData()
        checkPermissions()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            try {
                val totalCount = smsRepository.getLogCount()
                val forwardedCount = smsRepository.getForwardedCount()
                
                _totalSmsCount.value = totalCount
                _forwardedSmsCount.value = forwardedCount
                
            } catch (e: Exception) {
                _totalSmsCount.value = 0
                _forwardedSmsCount.value = 0
            }
        }
    }
    
    private fun checkPermissions() {
        val status = PermissionManager.checkAllPermissions(getApplication())
        _permissionStatus.value = status.hasBasicPermissions
        
        // 根据权限状态更新服务状态
        _serviceStatus.value = status.hasBasicPermissions && isServiceRunning()
    }
    
    fun toggleService() {
        Log.d(TAG, "toggleService called")
        val context = getApplication<Application>()
        val permissionStatus = PermissionManager.checkAllPermissions(context)
        
        Log.d(TAG, "Permission status: $permissionStatus")
        
        if (!permissionStatus.hasBasicPermissions) {
            Log.w(TAG, "Missing basic permissions: ${permissionStatus.missingPermissions}")
            _permissionStatus.value = false
            return
        }
        
        val isCurrentlyRunning = _serviceStatus.value ?: false
        Log.d(TAG, "Current service status: $isCurrentlyRunning")
        
        try {
            if (isCurrentlyRunning) {
                Log.d(TAG, "Stopping SMS service")
                SmsMonitorService.stop(context)
                _serviceStatus.value = false
            } else {
                Log.d(TAG, "Starting SMS service")
                SmsMonitorService.start(context)
                _serviceStatus.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling service", e)
        }
    }
    
    fun refreshData() {
        loadData()
        checkPermissions()
    }
    
    private fun isServiceRunning(): Boolean {
        // 简化实现，通过静态状态检查
        return SmsMonitorService.isRunning()
    }
}