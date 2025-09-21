package com.messagetrans.domain.repository

import com.messagetrans.data.database.entities.EmailConfig
import kotlinx.coroutines.flow.Flow

interface EmailRepository {
    fun getAllEmailConfigs(): Flow<List<EmailConfig>>
    suspend fun getEnabledEmailConfigs(): List<EmailConfig>
    suspend fun getEmailConfigById(id: String): EmailConfig?
    suspend fun insertEmailConfig(emailConfig: EmailConfig)
    suspend fun updateEmailConfig(emailConfig: EmailConfig)
    suspend fun deleteEmailConfig(emailConfig: EmailConfig)
    suspend fun deleteEmailConfigById(id: String)
}