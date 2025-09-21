package com.messagetrans.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.messagetrans.service.sms.SmsMonitorService
import com.messagetrans.utils.PermissionManager

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                startServiceIfPermissionsGranted(context)
            }
        }
    }
    
    private fun startServiceIfPermissionsGranted(context: Context) {
        val permissionStatus = PermissionManager.checkAllPermissions(context)
        
        if (permissionStatus.hasBasicPermissions) {
            try {
                SmsMonitorService.start(context)
                Log.d(TAG, "SMS Monitor Service started on boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SMS Monitor Service on boot", e)
            }
        } else {
            Log.w(TAG, "Cannot start service on boot - missing permissions")
        }
    }
}