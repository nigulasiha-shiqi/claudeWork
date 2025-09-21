package com.messagetrans.data.database.dao

import androidx.room.*
import com.messagetrans.data.database.entities.SimConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface SimConfigDao {
    @Query("SELECT * FROM sim_configs ORDER BY slotIndex ASC")
    fun getAllSimConfigs(): Flow<List<SimConfig>>

    @Query("SELECT * FROM sim_configs WHERE isEnabled = 1 ORDER BY slotIndex ASC")
    suspend fun getEnabledSimConfigs(): List<SimConfig>

    @Query("SELECT * FROM sim_configs WHERE slotIndex = :slotIndex")
    suspend fun getSimConfigBySlot(slotIndex: Int): SimConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimConfig(simConfig: SimConfig)

    @Update
    suspend fun updateSimConfig(simConfig: SimConfig)

    @Delete
    suspend fun deleteSimConfig(simConfig: SimConfig)
}