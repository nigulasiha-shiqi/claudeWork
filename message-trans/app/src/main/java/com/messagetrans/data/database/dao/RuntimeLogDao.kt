package com.messagetrans.data.database.dao

import androidx.room.*
import com.messagetrans.data.database.entities.RuntimeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface RuntimeLogDao {
    @Query("SELECT * FROM runtime_logs ORDER BY timestamp DESC LIMIT 500")
    fun getAllRuntimeLogs(): Flow<List<RuntimeLog>>

    @Query("SELECT * FROM runtime_logs WHERE level = :level ORDER BY timestamp DESC")
    suspend fun getRuntimeLogsByLevel(level: String): List<RuntimeLog>

    @Query("SELECT * FROM runtime_logs WHERE tag = :tag ORDER BY timestamp DESC")
    suspend fun getRuntimeLogsByTag(tag: String): List<RuntimeLog>

    @Query("SELECT * FROM runtime_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getRuntimeLogsByDateRange(startTime: Long, endTime: Long): List<RuntimeLog>

    @Insert
    suspend fun insertRuntimeLog(log: RuntimeLog)

    @Query("DELETE FROM runtime_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOldRuntimeLogs(beforeTime: Long)

    @Query("DELETE FROM runtime_logs")
    suspend fun clearAllRuntimeLogs()

    @Query("SELECT COUNT(*) FROM runtime_logs")
    suspend fun getRuntimeLogCount(): Int
}