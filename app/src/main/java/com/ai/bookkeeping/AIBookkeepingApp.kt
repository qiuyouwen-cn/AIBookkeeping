package com.ai.bookkeeping

import android.app.Application
import com.ai.bookkeeping.data.AppDatabase
import com.ai.bookkeeping.data.TransactionRepository

/**
 * Application类，用于初始化全局组件
 */
class AIBookkeepingApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TransactionRepository(database.transactionDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AIBookkeepingApp
            private set
    }
}
