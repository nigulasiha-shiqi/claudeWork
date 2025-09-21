package com.messagetrans.data.database.dao

import androidx.room.*
import com.messagetrans.data.database.entities.EmailConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailConfigDao {
    @Query("SELECT * FROM email_configs ORDER BY displayName ASC")
    fun getAllEmailConfigs(): Flow<List<EmailConfig>>

    @Query("SELECT * FROM email_configs WHERE isEnabled = 1 ORDER BY displayName ASC")
    suspend fun getEnabledEmailConfigs(): List<EmailConfig>

    @Query("SELECT * FROM email_configs WHERE id = :id")
    suspend fun getEmailConfigById(id: String): EmailConfig?

    @Query("SELECT COUNT(*) FROM email_configs")
    suspend fun getEmailConfigCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmailConfig(emailConfig: EmailConfig)

    @Update
    suspend fun updateEmailConfig(emailConfig: EmailConfig)

    @Delete
    suspend fun deleteEmailConfig(emailConfig: EmailConfig)

    @Query("DELETE FROM email_configs WHERE id = :id")
    suspend fun deleteEmailConfigById(id: String)
}