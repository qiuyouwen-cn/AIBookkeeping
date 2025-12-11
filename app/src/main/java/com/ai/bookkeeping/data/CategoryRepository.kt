package com.ai.bookkeeping.data

import com.ai.bookkeeping.model.Category
import com.ai.bookkeeping.model.TransactionType
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    fun getParentCategories(type: TransactionType): Flow<List<Category>> =
        categoryDao.getParentCategories(type)

    fun getSubCategories(parentId: Long): Flow<List<Category>> =
        categoryDao.getSubCategories(parentId)

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    suspend fun insert(category: Category): Long = categoryDao.insert(category)

    suspend fun update(category: Category) = categoryDao.update(category)

    suspend fun delete(category: Category) = categoryDao.delete(category)

    suspend fun deleteById(id: Long) = categoryDao.deleteById(id)

    suspend fun getMaxSortOrder(type: TransactionType): Int? = categoryDao.getMaxSortOrder(type)

    companion object {
        @Volatile
        private var INSTANCE: CategoryRepository? = null

        fun getInstance(categoryDao: CategoryDao): CategoryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CategoryRepository(categoryDao)
                INSTANCE = instance
                instance
            }
        }
    }
}
