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
    val useSSL: Boolean,
    val useProxy: Boolean = false, // 是否使用代理
    val proxyType: String = "HTTP", // 代理类型: HTTP, SOCKS
    val proxyHost: String = "", // 代理服务器地址
    val proxyPort: Int = 8080, // 代理端口
    val proxyUsername: String = "", // 代理用户名
    val proxyPassword: String = "" // 代理密码
)