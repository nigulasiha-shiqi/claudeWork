package com.messagetrans.domain.repository

import com.messagetrans.data.database.entities.SmsLog
import kotlinx.coroutines.flow.Flow

interface SmsRepository {
    fun getAllSmsLogs(): Flow<List<SmsLog>>
    suspend fun getSmsLogsByDateRange(startTime: Long, endTime: Long): List<SmsLog>
    suspend fun getSmsLogsByPhoneNumber(phoneNumber: String): List<SmsLog>
    suspend fun insertSmsLog(smsLog: SmsLog)
    suspend fun deleteSmsLog(smsLog: SmsLog)
    suspend fun deleteOldLogs(timestamp: Long)
    suspend fun getLogCount(): Int
    suspend fun getForwardedCount(): Int
}