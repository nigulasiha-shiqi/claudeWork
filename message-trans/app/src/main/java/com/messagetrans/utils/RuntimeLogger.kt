package com.messagetrans.utils

import android.content.Context
import android.util.Log
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.database.entities.RuntimeLog
import com.messagetrans.data.database.entities.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object RuntimeLogger {
    
    private var applicationContext: Context? = null
    
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
    
    fun logDebug(tag: String, message: String, details: String? = null) {
        Log.d(tag, message)
        saveLog(LogLevel.DEBUG, tag, message, details)
    }
    
    fun logInfo(tag: String, message: String, details: String? = null) {
        Log.i(tag, message)
        saveLog(LogLevel.INFO, tag, message, details)
    }
    
    fun logWarn(tag: String, message: String, details: String? = null) {
        Log.w(tag, message)
        saveLog(LogLevel.WARN, tag, message, details)
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null, details: String? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        
        val errorDetails = buildString {
            if (details != null) {
                append(details)
                append("\n")
            }
            if (throwable != null) {
                append("异常: ${throwable.message}\n")
                append("堆栈: ${throwable.stackTraceToString()}")
            }
        }.takeIf { it.isNotEmpty() }
        
        saveLog(LogLevel.ERROR, tag, message, errorDetails)
    }
    
    private fun saveLog(level: LogLevel, tag: String, message: String, details: String?) {
        val context = applicationContext ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context as? MessageTransApplication ?: return@launch
                val dao = app.database.runtimeLogDao()
                
                val log = RuntimeLog(
                    timestamp = System.currentTimeMillis(),
                    level = level.name,
                    tag = tag,
                    message = message,
                    details = details
                )
                
                dao.insertRuntimeLog(log)
                
                // 清理7天前的日志
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                dao.deleteOldRuntimeLogs(sevenDaysAgo)
                
            } catch (e: Exception) {
                Log.e("RuntimeLogger", "Failed to save runtime log", e)
            }
        }
    }
    
    // 邮件发送相关日志
    fun logEmailSending(emailAddress: String, smsInfo: String) {
        logInfo("EmailService", "开始发送邮件到 $emailAddress", "短信内容: $smsInfo")
    }
    
    fun logEmailSuccess(emailAddress: String, smsInfo: String) {
        logInfo("EmailService", "邮件发送成功到 $emailAddress", "短信内容: $smsInfo")
    }
    
    fun logEmailFailure(emailAddress: String, error: String, throwable: Throwable? = null) {
        logError("EmailService", "邮件发送失败到 $emailAddress: $error", throwable)
    }
    
    // SMS接收相关日志
    fun logSmsReceived(phoneNumber: String, content: String, simSlot: Int) {
        logInfo("SmsReceiver", "接收到短信", "发送方: $phoneNumber, SIM卡槽: $simSlot, 内容: ${content.take(50)}...")
    }
    
    fun logSmsProcessing(phoneNumber: String, enabledEmailCount: Int) {
        logInfo("SmsProcessor", "处理短信转发", "发送方: $phoneNumber, 启用的邮箱数量: $enabledEmailCount")
    }
    
    // 服务相关日志
    fun logServiceStart() {
        logInfo("SmsMonitorService", "短信监控服务已启动")
    }
    
    fun logServiceStop() {
        logInfo("SmsMonitorService", "短信监控服务已停止")
    }
    
    // 权限相关日志
    fun logPermissionGranted(permission: String) {
        logInfo("PermissionManager", "权限已授予: $permission")
    }
    
    fun logPermissionDenied(permission: String) {
        logWarn("PermissionManager", "权限被拒绝: $permission")
    }
}