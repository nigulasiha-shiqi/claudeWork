package com.messagetrans.service.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.messagetrans.data.database.entities.SmsLog
import com.messagetrans.utils.RuntimeLogger
import com.messagetrans.utils.SmsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SmsInterceptorReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsInterceptorReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            Log.d(TAG, "SMS received")
            
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No SMS messages found in intent")
                return
            }

            for (message in messages) {
                processSmsMessage(context, message, intent)
            }
        }
    }

    private fun processSmsMessage(context: Context, message: SmsMessage, intent: Intent) {
        try {
            val phoneNumber = message.originatingAddress ?: "Unknown"
            val messageBody = message.messageBody ?: ""
            val timestamp = message.timestampMillis
            
            // 获取SIM卡信息
            val simInfo = SmsUtils.getSimInfoFromIntent(context, intent)
            
            RuntimeLogger.logSmsReceived(phoneNumber, messageBody, simInfo.slotIndex)
            Log.d(TAG, "Processing SMS: from=$phoneNumber, body=$messageBody, sim=${simInfo.slotIndex}")
            
            // 检查是否需要拦截这个SIM卡的短信
            CoroutineScope(Dispatchers.IO).launch {
                if (SmsUtils.shouldInterceptSms(context, simInfo.slotIndex)) {
                    val smsLog = SmsLog(
                        id = UUID.randomUUID().toString(),
                        phoneNumber = phoneNumber,
                        content = messageBody,
                        timestamp = timestamp,
                        simSlot = simInfo.slotIndex,
                        simType = simInfo.simType,
                        isForwarded = false,
                        emailsSent = "[]",
                        errorMessage = null
                    )
                    
                    // 保存短信日志
                    SmsUtils.saveSmsLog(context, smsLog)
                    
                    // 触发邮件发送
                    triggerEmailSending(context, smsLog)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS message", e)
        }
    }

    private fun triggerEmailSending(context: Context, smsLog: SmsLog) {
        val workData = workDataOf(
            "sms_log_id" to smsLog.id,
            "phone_number" to smsLog.phoneNumber,
            "content" to smsLog.content,
            "timestamp" to smsLog.timestamp,
            "sim_slot" to smsLog.simSlot,
            "sim_type" to smsLog.simType
        )
        
        val emailWorkRequest = OneTimeWorkRequestBuilder<EmailSendingWorker>()
            .setInputData(workData)
            .build()
        
        WorkManager.getInstance(context).enqueue(emailWorkRequest)
        
        Log.d(TAG, "Email sending work enqueued for SMS: ${smsLog.id}")
    }
}