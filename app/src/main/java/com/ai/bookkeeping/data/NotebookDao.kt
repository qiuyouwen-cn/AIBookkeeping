package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ai.bookkeeping.model.Notebook
import kotlinx.coroutines.flow.Flow

/**
 * 账本数据访问对象
 */
@Dao
interface NotebookDao {

    @Insert
    suspend fun insert(notebook: Notebook): Long

    @Insert
    suspend fun insertAll(notebooks: List<Notebook>)

    @Update
    suspend fun update(notebook: Notebook)

    @Delete
    suspend fun delete(notebook: Notebook)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notebooks WHERE isActive = 1 ORDER BY sortOrder")
    fun getAllNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE isActive = 1 ORDER BY sortOrder")
    fun getAllNotebooksLiveData(): LiveData<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE isActive = 1 ORDER BY sortOrder")
    suspend fun getAllNotebooksSync(): List<Notebook>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebookById(id: Long): Notebook?

    @Query("SELECT * FROM notebooks WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefaultNotebook(): Notebook?

    @Query("SELECT * FROM notebooks WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    fun getDefaultNotebookFlow(): Flow<Notebook?>

    @Query("UPDATE notebooks SET isDefault = 0")
    suspend fun clearDefaultNotebooks()

    @Query("UPDATE notebooks SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultNotebook(id: Long)

    @Query("SELECT COUNT(*) FROM notebooks WHERE isActive = 1")
    suspend fun getNotebookCount(): Int

    @Query("SELECT MAX(sortOrder) FROM notebooks")
    suspend fun getMaxSortOrder(): Int?

    @Query("UPDATE notebooks SET isActive = 0 WHERE id = :id")
    suspend fun deactivateNotebook(id: Long)

    @Transaction
    suspend fun setAsDefault(id: Long) {
        clearDefaultNotebooks()
        setDefaultNotebook(id)
    }
}
