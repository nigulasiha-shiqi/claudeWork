package com.messagetrans.service.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.messagetrans.R
import com.messagetrans.presentation.ui.main.MainActivity

class SmsMonitorService : Service() {
    
    companion object {
        private const val TAG = "SmsMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "SMS_MONITOR_CHANNEL"
        
        fun start(context: Context) {
            val intent = Intent(context, SmsMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, SmsMonitorService::class.java)
            context.stopService(intent)
        }
    }
    
    private lateinit var smsReceiver: SmsInterceptorReceiver
    private var isReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SMS Monitor Service created")
        
        createNotificationChannel()
        smsReceiver = SmsInterceptorReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SMS Monitor Service started")
        
        startForeground(NOTIFICATION_ID, createNotification())
        registerSmsReceiver()
        
        return START_STICKY // 服务被杀死后自动重启
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SMS Monitor Service destroyed")
        
        unregisterSmsReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "短信监控服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "短信转发服务正在后台运行"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("短信转发服务")
            .setContentText("正在监控短信并转发到邮箱")
            .setSmallIcon(R.drawable.ic_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun registerSmsReceiver() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
                    priority = 1000 // 高优先级，确保能接收到短信
                }
                
                registerReceiver(smsReceiver, filter)
                isReceiverRegistered = true
                Log.d(TAG, "SMS receiver registered")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register SMS receiver", e)
            }
        }
    }

    private fun unregisterSmsReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(smsReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "SMS receiver unregistered")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister SMS receiver", e)
            }
        }
    }
}