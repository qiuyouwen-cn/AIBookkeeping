package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import com.ai.bookkeeping.model.Notebook
import kotlinx.coroutines.flow.Flow

/**
 * 账本仓库类
 */
class NotebookRepository(private val notebookDao: NotebookDao) {

    val allNotebooks: Flow<List<Notebook>> = notebookDao.getAllNotebooks()
    val allNotebooksLiveData: LiveData<List<Notebook>> = notebookDao.getAllNotebooksLiveData()
    val defaultNotebook: Flow<Notebook?> = notebookDao.getDefaultNotebookFlow()

    suspend fun insert(notebook: Notebook): Long {
        return notebookDao.insert(notebook)
    }

    suspend fun insertAll(notebooks: List<Notebook>) {
        notebookDao.insertAll(notebooks)
    }

    suspend fun update(notebook: Notebook) {
        notebookDao.update(notebook)
    }

    suspend fun delete(notebook: Notebook) {
        notebookDao.delete(notebook)
    }

    suspend fun deleteById(id: Long) {
        notebookDao.deleteById(id)
    }

    suspend fun getAllNotebooksSync(): List<Notebook> {
        return notebookDao.getAllNotebooksSync()
    }

    suspend fun getNotebookById(id: Long): Notebook? {
        return notebookDao.getNotebookById(id)
    }

    suspend fun getDefaultNotebookSync(): Notebook? {
        return notebookDao.getDefaultNotebook()
    }

    suspend fun setAsDefault(id: Long) {
        notebookDao.setAsDefault(id)
    }

    suspend fun getNotebookCount(): Int {
        return notebookDao.getNotebookCount()
    }

    suspend fun getMaxSortOrder(): Int {
        return notebookDao.getMaxSortOrder() ?: 0
    }

    suspend fun deactivateNotebook(id: Long) {
        notebookDao.deactivateNotebook(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: NotebookRepository? = null

        fun getInstance(notebookDao: NotebookDao): NotebookRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotebookRepository(notebookDao)
                INSTANCE = instance
                instance
            }
        }
    }
}
