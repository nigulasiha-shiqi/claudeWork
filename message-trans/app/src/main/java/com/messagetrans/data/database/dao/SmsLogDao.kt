package com.messagetrans.data.database.dao

import androidx.room.*
import com.messagetrans.data.database.entities.SmsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSmsLogs(): Flow<List<SmsLog>>

    @Query("SELECT * FROM sms_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getSmsLogsByDateRange(startTime: Long, endTime: Long): List<SmsLog>

    @Query("SELECT * FROM sms_logs WHERE phoneNumber LIKE :phoneNumber ORDER BY timestamp DESC")
    suspend fun getSmsLogsByPhoneNumber(phoneNumber: String): List<SmsLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(smsLog: SmsLog)

    @Update
    suspend fun updateSmsLog(smsLog: SmsLog)

    @Delete
    suspend fun deleteSmsLog(smsLog: SmsLog)

    @Query("DELETE FROM sms_logs WHERE timestamp < :timestamp")
    suspend fun deleteOldLogs(timestamp: Long)

    @Query("SELECT COUNT(*) FROM sms_logs")
    suspend fun getLogCount(): Int

    @Query("SELECT COUNT(*) FROM sms_logs WHERE isForwarded = 1")
    suspend fun getForwardedCount(): Int

    @Query("UPDATE sms_logs SET isForwarded = :isForwarded, emailsSent = :emailsSent, errorMessage = :errorMessage WHERE id = :smsLogId")
    suspend fun updateEmailStatus(smsLogId: String, isForwarded: Boolean, emailsSent: String, errorMessage: String?)
}