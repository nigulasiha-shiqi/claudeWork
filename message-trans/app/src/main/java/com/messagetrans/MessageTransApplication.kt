package com.messagetrans

import android.app.Application
import com.messagetrans.data.database.AppDatabase
import com.messagetrans.utils.EmailConfigCache
import com.messagetrans.utils.RuntimeLogger

class MessageTransApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        RuntimeLogger.init(this)
        EmailConfigCache.init(this)
    }

    companion object {
        @Volatile
        private var instance: MessageTransApplication? = null

        fun getInstance(): MessageTransApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}