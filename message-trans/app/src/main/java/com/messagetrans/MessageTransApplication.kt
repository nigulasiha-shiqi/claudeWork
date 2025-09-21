package com.messagetrans

import android.app.Application
import com.messagetrans.data.database.AppDatabase

class MessageTransApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: MessageTransApplication? = null

        fun getInstance(): MessageTransApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}