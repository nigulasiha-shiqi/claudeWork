package com.messagetrans.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val hasBasicPermissions: Boolean,
    val hasBatteryOptimization: Boolean,
    val hasNotificationPermission: Boolean,
    val missingPermissions: List<String>
)

object PermissionManager {
    
    private const val TAG = "PermissionManager"
    
    // 基础权限
    private val BASIC_PERMISSIONS = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.RECEIVE_BOOT_COMPLETED
    )
    
    // Android 10+ 电话号码权限
    private val PHONE_NUMBER_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.READ_PHONE_NUMBERS)
    } else {
        emptyArray()
    }
    
    // Android 13+ 通知权限
    private val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
    
    fun checkAllPermissions(context: Context): PermissionStatus {
        val missingPermissions = mutableListOf<String>()
        
        // 检查基础权限
        for (permission in BASIC_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        
        // 检查通知权限 (Android 13+)
        var hasNotificationPermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (permission in NOTIFICATION_PERMISSION) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission)
                    hasNotificationPermission = false
                }
            }
        }
        
        val hasBasicPermissions = missingPermissions.none { it in BASIC_PERMISSIONS }
        val hasBatteryOptimization = isBatteryOptimizationDisabled(context)
        
        Log.d(TAG, "Permission check - Basic: $hasBasicPermissions, Battery: $hasBatteryOptimization, Notification: $hasNotificationPermission")
        
        return PermissionStatus(
            hasBasicPermissions = hasBasicPermissions,
            hasBatteryOptimization = hasBatteryOptimization,
            hasNotificationPermission = hasNotificationPermission,
            missingPermissions = missingPermissions
        )
    }
    
    fun requestBasicPermissions(activity: Activity, requestCode: Int) {
        val allPermissions = BASIC_PERMISSIONS + NOTIFICATION_PERMISSION + PHONE_NUMBER_PERMISSION
        val missingPermissions = allPermissions.filter { 
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${missingPermissions.joinToString()}")
            ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), requestCode)
        }
    }
    
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // 低版本默认不受电池优化影响
        }
    }
    
    fun requestDisableBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Battery optimization settings opened")
                } else {
                    // 如果无法打开特定应用设置，打开通用设置
                    openBatteryOptimizationSettings(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open battery optimization settings", e)
                openBatteryOptimizationSettings(context)
            }
        }
    }
    
    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
            Log.d(TAG, "General battery optimization settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open general battery optimization settings", e)
        }
    }
    
    fun openAutoStartSettings(context: Context) {
        // 针对不同厂商的自启动设置
        val autoStartIntents = listOf(
            // 华为
            Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            
            // 小米
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Intent().setClassName("com.xiaomi.mipicks", "com.xiaomi.mipicks.ui.AppPicksTabActivity"),
            
            // OPPO
            Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.FakeActivity"),
            Intent().setClassName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            
            // VIVO
            Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            
            // 魅族
            Intent().setClassName("com.meizu.safe", "com.meizu.safe.security.SHOW_APPSEC"),
            
            // 一加
            Intent().setClassName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            
            // 三星
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
        )
        
        for (intent in autoStartIntents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Auto-start settings opened: ${intent.component}")
                    return
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to open auto-start settings: ${intent.component}", e)
            }
        }
        
        // 如果都失败了，打开应用设置页面
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
            Log.d(TAG, "App details settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open any settings", e)
        }
    }
    
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "App settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
        }
    }
}