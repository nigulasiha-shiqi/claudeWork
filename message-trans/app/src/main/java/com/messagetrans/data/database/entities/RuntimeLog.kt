package com.messagetrans.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runtime_logs")
data class RuntimeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: String, // INFO, WARN, ERROR, DEBUG
    val tag: String, // 日志标签，如 EmailService, SmsService 等
    val message: String, // 日志消息
    val details: String? = null // 详细信息，如错误堆栈等
)

enum class LogLevel(val displayName: String) {
    DEBUG("调试"),
    INFO("信息"),
    WARN("警告"),
    ERROR("错误")
}