package com.ai.bookkeeping

import android.app.Application
import com.ai.bookkeeping.data.*

/**
 * Application类，用于初始化全局组件
 */
class AIBookkeepingApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    // Repositories
    val repository by lazy { TransactionRepository(database.transactionDao()) }
    val categoryRepository by lazy { CategoryRepository.getInstance(database.categoryDao()) }
    val accountRepository by lazy { AccountRepository.getInstance(database.accountDao()) }
    val budgetRepository by lazy { BudgetRepository.getInstance(database.budgetDao()) }
    val notebookRepository by lazy { NotebookRepository.getInstance(database.notebookDao()) }
    val transferRepository by lazy { TransferRepository.getInstance(database.transferDao(), database.accountDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AIBookkeepingApp
            private set
    }
}
