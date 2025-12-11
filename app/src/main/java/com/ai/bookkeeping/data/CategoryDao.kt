package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ai.bookkeeping.model.Category
import com.ai.bookkeeping.model.TransactionType

/**
 * 分类数据访问对象
 */
@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE type = :type AND parentId IS NULL AND isActive = 1 ORDER BY sortOrder")
    fun getParentCategories(type: TransactionType): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type AND parentId IS NULL AND isActive = 1 ORDER BY sortOrder")
    suspend fun getParentCategoriesSync(type: TransactionType): List<Category>

    @Query("SELECT * FROM categories WHERE parentId = :parentId AND isActive = 1 ORDER BY sortOrder")
    fun getSubCategories(parentId: Long): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId AND isActive = 1 ORDER BY sortOrder")
    suspend fun getSubCategoriesSync(parentId: Long): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type AND parentId IS NULL LIMIT 1")
    suspend fun getCategoryByName(name: String, type: TransactionType): Category?

    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY sortOrder")
    fun getAllCategories(type: TransactionType): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY sortOrder")
    suspend fun getAllCategoriesSync(type: TransactionType): List<Category>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("UPDATE categories SET isActive = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: Long)
}
