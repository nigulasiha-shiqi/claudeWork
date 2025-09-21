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

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
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
        val context = getApplication<Application>()
        val hasPermissions = PermissionManager.checkAllPermissions(context).hasBasicPermissions
        
        if (!hasPermissions) {
            _permissionStatus.value = false
            return
        }
        
        val isCurrentlyRunning = _serviceStatus.value ?: false
        
        if (isCurrentlyRunning) {
            SmsMonitorService.stop(context)
            _serviceStatus.value = false
        } else {
            SmsMonitorService.start(context)
            _serviceStatus.value = true
        }
    }
    
    fun refreshData() {
        loadData()
        checkPermissions()
    }
    
    private fun isServiceRunning(): Boolean {
        // 这里可以实现检查服务是否正在运行的逻辑
        // 简化实现，实际应用中可以通过ActivityManager检查
        return false
    }
}