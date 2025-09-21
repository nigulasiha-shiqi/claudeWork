package com.messagetrans.domain.repository

import com.messagetrans.data.database.entities.SimConfig
import kotlinx.coroutines.flow.Flow

interface SimRepository {
    fun getAllSimConfigs(): Flow<List<SimConfig>>
    suspend fun getEnabledSimConfigs(): List<SimConfig>
    suspend fun getSimConfigBySlot(slotIndex: Int): SimConfig?
    suspend fun insertSimConfig(simConfig: SimConfig)
    suspend fun updateSimConfig(simConfig: SimConfig)
    suspend fun deleteSimConfig(simConfig: SimConfig)
}