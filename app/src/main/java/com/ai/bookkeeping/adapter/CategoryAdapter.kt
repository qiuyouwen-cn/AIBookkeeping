package com.ai.bookkeeping.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.model.Category

class CategoryAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit,
    private val onItemClick: (Category) -> Unit,
    private val onAddSubClick: ((Category) -> Unit)? = null
) : ListAdapter<CategoryAdapter.CategoryItem, RecyclerView.ViewHolder>(CategoryDiffCallback()) {

    companion object {
        private const val TYPE_PARENT = 0
        private const val TYPE_SUB = 1
    }

    private val expandedParentIds = mutableSetOf<Long>()
    private var allCategories: List<Category> = emptyList()

    sealed class CategoryItem {
        data class Parent(val category: Category, var isExpanded: Boolean = false) : CategoryItem()
        data class Sub(val category: Category, val parentId: Long) : CategoryItem()
    }

    fun setCategories(categories: List<Category>) {
        allCategories = categories
        updateDisplayList()
    }

    private fun updateDisplayList() {
        val displayList = mutableListOf<CategoryItem>()
        val parentCategories = allCategories.filter { it.parentId == null }

        for (parent in parentCategories) {
            val isExpanded = expandedParentIds.contains(parent.id)
            displayList.add(CategoryItem.Parent(parent, isExpanded))

            if (isExpanded) {
                val subCategories = allCategories.filter { it.parentId == parent.id }
                for (sub in subCategories) {
                    displayList.add(CategoryItem.Sub(sub, parent.id))
                }
            }
        }

        submitList(displayList)
    }

    fun toggleExpand(parentId: Long) {
        if (expandedParentIds.contains(parentId)) {
            expandedParentIds.remove(parentId)
        } else {
            expandedParentIds.add(parentId)
        }
        updateDisplayList()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CategoryItem.Parent -> TYPE_PARENT
            is CategoryItem.Sub -> TYPE_SUB
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PARENT -> {
                val view = inflater.inflate(R.layout.item_category_parent, parent, false)
                ParentViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_category_sub, parent, false)
                SubViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CategoryItem.Parent -> (holder as ParentViewHolder).bind(item)
            is CategoryItem.Sub -> (holder as SubViewHolder).bind(item)
        }
    }

    inner class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewIconBg: View = itemView.findViewById(R.id.viewIconBg)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvSubCount: TextView = itemView.findViewById(R.id.tvSubCount)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        private val layoutParent: View = itemView.findViewById(R.id.layoutParent)

        fun bind(item: CategoryItem.Parent) {
            val category = item.category

            tvCategoryName.text = category.name

            // Set icon background color
            val bgDrawable = viewIconBg.background as? GradientDrawable
                ?: GradientDrawable().also { viewIconBg.background = it }
            bgDrawable.shape = GradientDrawable.OVAL
            try {
                bgDrawable.setColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                bgDrawable.setColor(Color.parseColor("#6C63FF"))
            }

            // Set icon
            val iconResId = getIconResource(category.icon)
            ivIcon.setImageResource(iconResId)

            // Count sub categories
            val subCount = allCategories.count { it.parentId == category.id }
            tvSubCount.text = if (subCount > 0) "${subCount}个子分类" else "无子分类"

            // Show/hide expand icon based on sub categories
            ivExpand.visibility = if (subCount > 0) View.VISIBLE else View.INVISIBLE
            ivExpand.rotation = if (item.isExpanded) 180f else 0f

            // Click listeners
            layoutParent.setOnClickListener {
                if (subCount > 0) {
                    toggleExpand(category.id)
                }
                onItemClick(category)
            }

            ivEdit.setOnClickListener { onEditClick(category) }
            ivDelete.setOnClickListener {
                if (!category.isSystem) {
                    onDeleteClick(category)
                }
            }

            // Disable delete for system categories
            ivDelete.alpha = if (category.isSystem) 0.3f else 1f
        }
    }

    inner class SubViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewIconBg: View = itemView.findViewById(R.id.viewIconBg)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvSubCategoryName: TextView = itemView.findViewById(R.id.tvSubCategoryName)
        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

        fun bind(item: CategoryItem.Sub) {
            val category = item.category

            tvSubCategoryName.text = category.name

            // Set icon background color
            val bgDrawable = viewIconBg.background as? GradientDrawable
                ?: GradientDrawable().also { viewIconBg.background = it }
            bgDrawable.shape = GradientDrawable.OVAL
            try {
                bgDrawable.setColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                bgDrawable.setColor(Color.parseColor("#6C63FF"))
            }

            // Set icon
            val iconResId = getIconResource(category.icon)
            ivIcon.setImageResource(iconResId)

            // Click listeners
            itemView.setOnClickListener { onItemClick(category) }
            ivEdit.setOnClickListener { onEditClick(category) }
            ivDelete.setOnClickListener {
                if (!category.isSystem) {
                    onDeleteClick(category)
                }
            }

            // Disable delete for system categories
            ivDelete.alpha = if (category.isSystem) 0.3f else 1f
        }
    }

    private fun getIconResource(iconName: String): Int {
        return when (iconName) {
            "ic_food" -> R.drawable.ic_food
            "ic_transport" -> R.drawable.ic_transport
            "ic_shopping" -> R.drawable.ic_shopping
            "ic_entertainment" -> R.drawable.ic_entertainment
            "ic_medical" -> R.drawable.ic_medical
            "ic_education" -> R.drawable.ic_education
            "ic_housing" -> R.drawable.ic_housing
            "ic_communication" -> R.drawable.ic_communication
            "ic_clothing" -> R.drawable.ic_clothing
            "ic_salary" -> R.drawable.ic_salary
            "ic_bonus" -> R.drawable.ic_bonus
            "ic_investment" -> R.drawable.ic_investment
            "ic_parttime" -> R.drawable.ic_parttime
            "ic_redpacket" -> R.drawable.ic_redpacket
            "ic_other" -> R.drawable.ic_other
            else -> R.drawable.ic_other
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return when {
                oldItem is CategoryItem.Parent && newItem is CategoryItem.Parent ->
                    oldItem.category.id == newItem.category.id
                oldItem is CategoryItem.Sub && newItem is CategoryItem.Sub ->
                    oldItem.category.id == newItem.category.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return when {
                oldItem is CategoryItem.Parent && newItem is CategoryItem.Parent ->
                    oldItem.category == newItem.category && oldItem.isExpanded == newItem.isExpanded
                oldItem is CategoryItem.Sub && newItem is CategoryItem.Sub ->
                    oldItem.category == newItem.category
                else -> false
            }
        }
    }
}
