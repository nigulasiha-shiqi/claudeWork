package com.messagetrans.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sim_configs")
data class SimConfig(
    @PrimaryKey val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String?,
    val isEnabled: Boolean, // 是否启用拦截
    val simType: String // "physical" or "esim"
)