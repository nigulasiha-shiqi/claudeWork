package com.messagetrans.data.repository

import com.messagetrans.data.database.dao.SmsLogDao
import com.messagetrans.data.database.entities.SmsLog
import com.messagetrans.domain.repository.SmsRepository
import kotlinx.coroutines.flow.Flow

class SmsRepositoryImpl(private val smsLogDao: SmsLogDao) : SmsRepository {
    override fun getAllSmsLogs(): Flow<List<SmsLog>> = smsLogDao.getAllSmsLogs()

    override suspend fun getSmsLogsByDateRange(startTime: Long, endTime: Long): List<SmsLog> =
        smsLogDao.getSmsLogsByDateRange(startTime, endTime)

    override suspend fun getSmsLogsByPhoneNumber(phoneNumber: String): List<SmsLog> =
        smsLogDao.getSmsLogsByPhoneNumber(phoneNumber)

    override suspend fun insertSmsLog(smsLog: SmsLog) = smsLogDao.insertSmsLog(smsLog)

    override suspend fun updateSmsLog(smsLog: SmsLog) = smsLogDao.updateSmsLog(smsLog)

    override suspend fun deleteSmsLog(smsLog: SmsLog) = smsLogDao.deleteSmsLog(smsLog)

    override suspend fun deleteOldLogs(timestamp: Long) = smsLogDao.deleteOldLogs(timestamp)

    override suspend fun getLogCount(): Int = smsLogDao.getLogCount()

    override suspend fun getForwardedCount(): Int = smsLogDao.getForwardedCount()
}