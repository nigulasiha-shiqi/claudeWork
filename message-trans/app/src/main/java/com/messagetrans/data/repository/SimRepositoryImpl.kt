package com.messagetrans.data.repository

import com.messagetrans.data.database.dao.SimConfigDao
import com.messagetrans.data.database.entities.SimConfig
import com.messagetrans.domain.repository.SimRepository
import kotlinx.coroutines.flow.Flow

class SimRepositoryImpl(private val simConfigDao: SimConfigDao) : SimRepository {
    override fun getAllSimConfigs(): Flow<List<SimConfig>> = simConfigDao.getAllSimConfigs()

    override suspend fun getEnabledSimConfigs(): List<SimConfig> =
        simConfigDao.getEnabledSimConfigs()

    override suspend fun getSimConfigBySlot(slotIndex: Int): SimConfig? =
        simConfigDao.getSimConfigBySlot(slotIndex)

    override suspend fun getSimConfigCount(): Int =
        simConfigDao.getSimConfigCount()

    override suspend fun insertSimConfig(simConfig: SimConfig) =
        simConfigDao.insertSimConfig(simConfig)

    override suspend fun updateSimConfig(simConfig: SimConfig) =
        simConfigDao.updateSimConfig(simConfig)

    override suspend fun deleteSimConfig(simConfig: SimConfig) =
        simConfigDao.deleteSimConfig(simConfig)
}