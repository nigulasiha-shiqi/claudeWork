package com.messagetrans.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_configs")
data class EmailConfig(
    @PrimaryKey val id: String,
    val displayName: String,
    val emailAddress: String,
    val smtpServer: String,
    val smtpPort: Int,
    val username: String,
    val password: String, // 加密存储
    val isEnabled: Boolean,
    val useSSL: Boolean
)