package com.messagetrans.utils

import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.messagetrans.MessageTransApplication
import com.messagetrans.data.database.entities.SmsLog
import com.messagetrans.data.repository.SimRepositoryImpl
import com.messagetrans.data.repository.SmsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SimInfo(
    val slotIndex: Int,
    val simType: String,
    val carrierName: String,
    val phoneNumber: String?
)

object SmsUtils {
    
    private const val TAG = "SmsUtils"
    
    fun getSimInfoFromIntent(context: Context, intent: Intent): SimInfo {
        return try {
            val subscriptionId = intent.getIntExtra("subscription", -1)
            val slotIndex = intent.getIntExtra("slot", 0)
            
            if (subscriptionId != -1) {
                getSimInfoBySubscriptionId(context, subscriptionId)
            } else {
                getSimInfoBySlotIndex(context, slotIndex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SIM info from intent", e)
            SimInfo(0, "unknown", "Unknown", null)
        }
    }
    
    private fun getSimInfoBySubscriptionId(context: Context, subscriptionId: Int): SimInfo {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        
        try {
            val subscriptionInfo: SubscriptionInfo? = subscriptionManager.getActiveSubscriptionInfo(subscriptionId)
            
            return if (subscriptionInfo != null) {
                SimInfo(
                    slotIndex = subscriptionInfo.simSlotIndex,
                    simType = if (subscriptionInfo.isEmbedded) "esim" else "physical",
                    carrierName = subscriptionInfo.carrierName?.toString() ?: "Unknown",
                    phoneNumber = subscriptionInfo.number
                )
            } else {
                SimInfo(0, "unknown", "Unknown", null)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when accessing subscription info", e)
            return SimInfo(0, "unknown", "Unknown", null)
        }
    }
    
    private fun getSimInfoBySlotIndex(context: Context, slotIndex: Int): SimInfo {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        return try {
            val carrierName = telephonyManager.simOperatorName ?: "Unknown"
            val phoneNumber = telephonyManager.line1Number
            
            SimInfo(
                slotIndex = slotIndex,
                simType = "unknown", // 无法从slot确定类型
                carrierName = carrierName,
                phoneNumber = phoneNumber
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when accessing telephony info", e)
            SimInfo(slotIndex, "unknown", "Unknown", null)
        }
    }
    
    suspend fun shouldInterceptSms(context: Context, slotIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val database = MessageTransApplication.getInstance().database
                val simRepository = SimRepositoryImpl(database.simConfigDao())
                
                val simConfig = simRepository.getSimConfigBySlot(slotIndex)
                simConfig?.isEnabled ?: false
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if SMS should be intercepted", e)
                false
            }
        }
    }
    
    suspend fun saveSmsLog(context: Context, smsLog: SmsLog) {
        withContext(Dispatchers.IO) {
            try {
                val database = MessageTransApplication.getInstance().database
                val smsRepository = SmsRepositoryImpl(database.smsLogDao())
                
                smsRepository.insertSmsLog(smsLog)
                Log.d(TAG, "SMS log saved: ${smsLog.id}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving SMS log", e)
            }
        }
    }
    
    suspend fun updateSmsLogEmailStatus(context: Context, smsLogId: String, emailsSent: List<String>, errorMessage: String?) {
        withContext(Dispatchers.IO) {
            try {
                val database = MessageTransApplication.getInstance().database
                val smsLogDao = database.smsLogDao()
                
                val emailsSentJson = com.google.gson.Gson().toJson(emailsSent)
                val isForwarded = emailsSent.isNotEmpty()
                
                smsLogDao.updateEmailStatus(smsLogId, isForwarded, emailsSentJson, errorMessage)
                
                RuntimeLogger.logInfo(TAG, "短信日志邮件状态已更新", 
                    "ID: $smsLogId, 转发成功: $isForwarded, 邮箱数量: ${emailsSent.size}")
                Log.d(TAG, "SMS log email status updated: $smsLogId")
                
            } catch (e: Exception) {
                RuntimeLogger.logError(TAG, "更新短信日志邮件状态失败", e, "ID: $smsLogId")
                Log.e(TAG, "Error updating SMS log email status", e)
            }
        }
    }
}