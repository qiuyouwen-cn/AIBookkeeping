package com.ai.bookkeeping.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ai.bookkeeping.data.AppDatabase
import com.ai.bookkeeping.data.CategoryRepository
import com.ai.bookkeeping.model.Category
import com.ai.bookkeeping.model.TransactionType
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository

    val allCategories: LiveData<List<Category>>

    init {
        val categoryDao = AppDatabase.getDatabase(application).categoryDao()
        repository = CategoryRepository.getInstance(categoryDao)
        allCategories = repository.getAllCategories().asLiveData()
    }

    fun getParentCategories(type: TransactionType): LiveData<List<Category>> {
        return repository.getParentCategories(type).asLiveData()
    }

    fun getSubCategories(parentId: Long): LiveData<List<Category>> {
        return repository.getSubCategories(parentId).asLiveData()
    }

    fun getCategoriesByType(type: TransactionType): LiveData<List<Category>> {
        return repository.getCategoriesByType(type).asLiveData()
    }

    fun insertCategory(category: Category, onResult: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insert(category)
            onResult(id)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.update(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.delete(category)
        }
    }

    fun deleteCategoryById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun getNextSortOrder(type: TransactionType, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxSortOrder(type) ?: 0
            onResult(maxOrder + 1)
        }
    }

    fun getExpenseCategories(): LiveData<List<Category>> {
        return repository.getCategoriesByType(TransactionType.EXPENSE).asLiveData()
    }

    fun getIncomeCategories(): LiveData<List<Category>> {
        return repository.getCategoriesByType(TransactionType.INCOME).asLiveData()
    }
}
