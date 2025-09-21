package com.messagetrans.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.messagetrans.data.database.dao.EmailConfigDao
import com.messagetrans.data.database.dao.SimConfigDao
import com.messagetrans.data.database.dao.SmsLogDao
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.data.database.entities.SimConfig
import com.messagetrans.data.database.entities.SmsLog

@Database(
    entities = [SmsLog::class, EmailConfig::class, SimConfig::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsLogDao(): SmsLogDao
    abstract fun emailConfigDao(): EmailConfigDao
    abstract fun simConfigDao(): SimConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "message_trans_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}