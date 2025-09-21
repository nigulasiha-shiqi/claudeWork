package com.messagetrans.service.sms

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.repository.EmailRepositoryImpl
import com.messagetrans.service.email.EmailService
import com.messagetrans.utils.SmsUtils

class EmailSendingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "EmailSendingWorker"
    }
    
    override suspend fun doWork(): Result {
        return try {
            val smsLogId = inputData.getString("sms_log_id") ?: return Result.failure()
            val phoneNumber = inputData.getString("phone_number") ?: return Result.failure()
            val content = inputData.getString("content") ?: return Result.failure()
            val timestamp = inputData.getLong("timestamp", 0L)
            val simSlot = inputData.getInt("sim_slot", 0)
            val simType = inputData.getString("sim_type") ?: "unknown"
            
            Log.d(TAG, "Processing email sending for SMS: $smsLogId")
            
            // 获取启用的邮箱配置
            val database = MessageTransApplication.getInstance().database
            val emailRepository = EmailRepositoryImpl(database.emailConfigDao())
            val enabledEmailConfigs = emailRepository.getEnabledEmailConfigs()
            
            if (enabledEmailConfigs.isEmpty()) {
                Log.w(TAG, "No enabled email configs found")
                return Result.success()
            }
            
            // 格式化邮件内容
            val (subject, emailContent) = EmailService.formatSmsEmailContent(
                phoneNumber, content, timestamp, simSlot, simType
            )
            
            val successfulEmails = mutableListOf<String>()
            var lastError: String? = null
            
            // 向所有启用的邮箱发送
            for (emailConfig in enabledEmailConfigs) {
                val result = EmailService.sendEmail(emailConfig, subject, emailContent)
                
                if (result.success) {
                    successfulEmails.add(emailConfig.emailAddress)
                    Log.d(TAG, "Email sent successfully to ${emailConfig.emailAddress}")
                } else {
                    lastError = result.errorMessage
                    Log.e(TAG, "Failed to send email to ${emailConfig.emailAddress}: ${result.errorMessage}")
                }
            }
            
            // 更新短信日志的邮件发送状态
            val emailsSentJson = Gson().toJson(successfulEmails)
            SmsUtils.updateSmsLogEmailStatus(
                applicationContext,
                smsLogId,
                successfulEmails,
                if (successfulEmails.isEmpty()) lastError else null
            )
            
            if (successfulEmails.isNotEmpty()) {
                Log.d(TAG, "Email sending completed successfully for SMS: $smsLogId")
                Result.success()
            } else {
                Log.e(TAG, "Failed to send email to any configured address for SMS: $smsLogId")
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in EmailSendingWorker", e)
            Result.retry()
        }
    }
}