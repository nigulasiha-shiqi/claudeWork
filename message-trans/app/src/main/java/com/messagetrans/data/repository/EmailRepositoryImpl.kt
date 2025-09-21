package com.messagetrans.data.repository

import com.messagetrans.data.database.dao.EmailConfigDao
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.domain.repository.EmailRepository
import kotlinx.coroutines.flow.Flow

class EmailRepositoryImpl(private val emailConfigDao: EmailConfigDao) : EmailRepository {
    override fun getAllEmailConfigs(): Flow<List<EmailConfig>> = emailConfigDao.getAllEmailConfigs()

    override suspend fun getEnabledEmailConfigs(): List<EmailConfig> =
        emailConfigDao.getEnabledEmailConfigs()

    override suspend fun getEmailConfigById(id: String): EmailConfig? =
        emailConfigDao.getEmailConfigById(id)

    override suspend fun insertEmailConfig(emailConfig: EmailConfig) =
        emailConfigDao.insertEmailConfig(emailConfig)

    override suspend fun updateEmailConfig(emailConfig: EmailConfig) =
        emailConfigDao.updateEmailConfig(emailConfig)

    override suspend fun deleteEmailConfig(emailConfig: EmailConfig) =
        emailConfigDao.deleteEmailConfig(emailConfig)

    override suspend fun deleteEmailConfigById(id: String) =
        emailConfigDao.deleteEmailConfigById(id)
}