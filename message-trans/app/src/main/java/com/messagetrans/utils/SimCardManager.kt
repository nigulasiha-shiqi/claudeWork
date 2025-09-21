package com.messagetrans.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.messagetrans.data.database.entities.SimConfig

data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String?,
    val countryIso: String?,
    val simType: String, // "physical" or "esim"
    val isActive: Boolean
)

object SimCardManager {
    private const val TAG = "SimCardManager"
    
    /**
     * 获取所有可用的SIM卡信息（包括eSIM）
     */
    fun getAllSimCards(context: Context): List<SimCardInfo> {
        val simCards = mutableListOf<SimCardInfo>()
        
        // 检查权限
        if (!hasRequiredPermissions(context)) {
            Log.w(TAG, "Missing required permissions for SIM card access")
            return simCards
        }
        
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            
            if (subscriptionManager == null || telephonyManager == null) {
                Log.e(TAG, "Failed to get system services")
                return simCards
            }
            
            // 获取所有订阅信息（包括eSIM）
            val subscriptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            } else {
                emptyList()
            }
            
            Log.d(TAG, "Found ${subscriptions.size} active subscriptions")
            
            for (subscription in subscriptions) {
                try {
                    val simInfo = parseSubscriptionInfo(subscription, telephonyManager)
                    simCards.add(simInfo)
                    Log.d(TAG, "Added SIM card: $simInfo")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing subscription info", e)
                }
            }
            
            // 如果没有找到活跃的订阅，尝试检查SIM卡槽
            if (simCards.isEmpty()) {
                simCards.addAll(checkSimSlots(telephonyManager))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SIM card information", e)
        }
        
        return simCards.sortedBy { it.slotIndex }
    }
    
    /**
     * 解析订阅信息
     */
    private fun parseSubscriptionInfo(subscription: SubscriptionInfo, telephonyManager: TelephonyManager): SimCardInfo {
        val subscriptionId = subscription.subscriptionId
        val slotIndex = subscription.simSlotIndex
        
        // 获取显示名称
        val displayName = when {
            !subscription.displayName.isNullOrEmpty() -> subscription.displayName.toString()
            !subscription.carrierName.isNullOrEmpty() -> subscription.carrierName.toString()
            else -> "SIM卡 ${slotIndex + 1}"
        }
        
        // 获取运营商名称
        val carrierName = subscription.carrierName?.toString() ?: "未知运营商"
        
        // 获取电话号码
        val phoneNumber = try {
            subscription.number?.takeIf { it.isNotEmpty() && it != "Unknown" }
        } catch (e: Exception) {
            null
        }
        
        // 获取国家代码
        val countryIso = subscription.countryIso
        
        // 判断SIM卡类型
        val simType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (subscription.isEmbedded) "esim" else "physical"
        } else {
            "physical" // Android 10以下无法准确判断，默认为实体卡
        }
        
        return SimCardInfo(
            subscriptionId = subscriptionId,
            slotIndex = slotIndex,
            displayName = displayName,
            carrierName = carrierName,
            phoneNumber = phoneNumber,
            countryIso = countryIso,
            simType = simType,
            isActive = true
        )
    }
    
    /**
     * 检查SIM卡槽（备用方法）
     */
    private fun checkSimSlots(telephonyManager: TelephonyManager): List<SimCardInfo> {
        val simCards = mutableListOf<SimCardInfo>()
        
        try {
            val phoneCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telephonyManager.phoneCount
            } else {
                2 // 默认双卡
            }
            
            Log.d(TAG, "Phone count: $phoneCount")
            
            for (slotIndex in 0 until phoneCount) {
                try {
                    val simState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        telephonyManager.getSimState(slotIndex)
                    } else {
                        telephonyManager.simState
                    }
                    
                    if (simState == TelephonyManager.SIM_STATE_READY) {
                        val simInfo = SimCardInfo(
                            subscriptionId = -1,
                            slotIndex = slotIndex,
                            displayName = "SIM卡 ${slotIndex + 1}",
                            carrierName = "未知运营商",
                            phoneNumber = null,
                            countryIso = null,
                            simType = "physical",
                            isActive = true
                        )
                        simCards.add(simInfo)
                        Log.d(TAG, "Added SIM slot $slotIndex")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking SIM slot $slotIndex", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking SIM slots", e)
        }
        
        return simCards
    }
    
    /**
     * 将SimCardInfo转换为SimConfig实体
     */
    fun convertToSimConfig(simCardInfo: SimCardInfo): SimConfig {
        return SimConfig(
            slotIndex = simCardInfo.slotIndex,
            displayName = simCardInfo.displayName,
            carrierName = simCardInfo.carrierName,
            phoneNumber = simCardInfo.phoneNumber,
            isEnabled = true, // 默认启用
            simType = simCardInfo.simType
        )
    }
    
    /**
     * 检查所需权限
     */
    private fun hasRequiredPermissions(context: Context): Boolean {
        val requiredPermissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS
        )
        
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 获取指定槽位的SIM卡信息
     */
    fun getSimCardBySlot(context: Context, slotIndex: Int): SimCardInfo? {
        return getAllSimCards(context).find { it.slotIndex == slotIndex }
    }
    
    /**
     * 检查SIM卡是否发生变化
     */
    fun hasSimCardsChanged(context: Context, existingConfigs: List<SimConfig>): Boolean {
        val currentSimCards = getAllSimCards(context)
        
        if (currentSimCards.size != existingConfigs.size) {
            return true
        }
        
        // 检查每个槽位的SIM卡是否匹配
        for (simCard in currentSimCards) {
            val existingConfig = existingConfigs.find { it.slotIndex == simCard.slotIndex }
            if (existingConfig == null || 
                existingConfig.carrierName != simCard.carrierName ||
                existingConfig.simType != simCard.simType) {
                return true
            }
        }
        
        return false
    }
}