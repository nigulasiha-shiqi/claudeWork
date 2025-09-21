package com.messagetrans.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey val id: String,
    val phoneNumber: String,
    val content: String,
    val timestamp: Long,
    val simSlot: Int, // 0=SIM1, 1=SIM2
    val simType: String, // "physical" or "esim"
    val isForwarded: Boolean,
    val emailsSent: String, // JSON格式存储发送的邮箱列表
    val errorMessage: String?
)