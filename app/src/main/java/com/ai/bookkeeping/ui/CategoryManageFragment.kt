package com.ai.bookkeeping.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ai.bookkeeping.R
import com.ai.bookkeeping.adapter.CategoryAdapter
import com.ai.bookkeeping.adapter.ColorPickerAdapter
import com.ai.bookkeeping.adapter.IconPickerAdapter
import com.ai.bookkeeping.model.Category
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.CategoryViewModel

class CategoryManageFragment : Fragment() {

    private val viewModel: CategoryViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var fabAddCategory: FloatingActionButton

    private var currentType = TransactionType.EXPENSE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_manage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        fabAddCategory = view.findViewById(R.id.fabAddCategory)

        setupToolbar()
        setupViewPager()
        setupFab()
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupViewPager() {
        viewPager.adapter = CategoryPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "支出分类"
                1 -> "收入分类"
                else -> ""
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentType = if (position == 0) TransactionType.EXPENSE else TransactionType.INCOME
            }
        })
    }

    private fun setupFab() {
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog(null)
        }
    }

    fun showAddCategoryDialog(parentCategory: Category?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val recyclerIcons = dialogView.findViewById<RecyclerView>(R.id.recyclerIcons)
        val recyclerColors = dialogView.findViewById<RecyclerView>(R.id.recyclerColors)
        val layoutParentCategory = dialogView.findViewById<View>(R.id.layoutParentCategory)
        val spinnerParentCategory = dialogView.findViewById<Spinner>(R.id.spinnerParentCategory)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        tvTitle.text = if (parentCategory != null) "添加子分类" else "添加分类"

        // Setup icon picker
        val iconAdapter = IconPickerAdapter(IconPickerAdapter.getDefaultIcons()) { }
        recyclerIcons.adapter = iconAdapter

        // Setup color picker
        val colorAdapter = ColorPickerAdapter(ColorPickerAdapter.getDefaultColors()) { }
        recyclerColors.adapter = colorAdapter

        // Show parent category selector if adding root category
        if (parentCategory == null) {
            layoutParentCategory.visibility = View.VISIBLE
            val parentOptions = mutableListOf("无 (作为一级分类)")
            val parentIds = mutableListOf<Long?>(null)

            viewModel.getParentCategories(currentType).observe(viewLifecycleOwner) { categories ->
                categories.forEach {
                    parentOptions.add(it.name)
                    parentIds.add(it.id)
                }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    parentOptions
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerParentCategory.adapter = adapter
            }
        } else {
            layoutParentCategory.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            if (name.isEmpty()) {
                etCategoryName.error = "请输入分类名称"
                return@setOnClickListener
            }

            val selectedIcon = iconAdapter.getSelectedIcon()
            val selectedColor = colorAdapter.getSelectedColor()

            val parentId = if (parentCategory != null) {
                parentCategory.id
            } else {
                val position = spinnerParentCategory.selectedItemPosition
                if (position == 0) null else {
                    // Get parent id from observed data
                    viewModel.getParentCategories(currentType).value?.getOrNull(position - 1)?.id
                }
            }

            viewModel.getNextSortOrder(currentType) { sortOrder ->
                val category = Category(
                    name = name,
                    icon = selectedIcon.name,
                    color = selectedColor,
                    type = currentType,
                    parentId = parentId,
                    sortOrder = sortOrder,
                    isSystem = false,
                    isActive = true
                )
                viewModel.insertCategory(category)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val recyclerIcons = dialogView.findViewById<RecyclerView>(R.id.recyclerIcons)
        val recyclerColors = dialogView.findViewById<RecyclerView>(R.id.recyclerColors)
        val layoutParentCategory = dialogView.findViewById<View>(R.id.layoutParentCategory)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        tvTitle.text = "编辑分类"
        etCategoryName.setText(category.name)
        layoutParentCategory.visibility = View.GONE

        // Setup icon picker
        val iconAdapter = IconPickerAdapter(IconPickerAdapter.getDefaultIcons()) { }
        recyclerIcons.adapter = iconAdapter
        iconAdapter.setSelectedIcon(category.icon)

        // Setup color picker
        val colorAdapter = ColorPickerAdapter(ColorPickerAdapter.getDefaultColors()) { }
        recyclerColors.adapter = colorAdapter
        colorAdapter.setSelectedColor(category.color)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            if (name.isEmpty()) {
                etCategoryName.error = "请输入分类名称"
                return@setOnClickListener
            }

            val selectedIcon = iconAdapter.getSelectedIcon()
            val selectedColor = colorAdapter.getSelectedColor()

            val updatedCategory = category.copy(
                name = name,
                icon = selectedIcon.name,
                color = selectedColor
            )
            viewModel.updateCategory(updatedCategory)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showDeleteCategoryDialog(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除分类")
            .setMessage("确定要删除\"${category.name}\"吗？${if (category.parentId == null) "这将同时删除所有子分类。" else ""}")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteCategory(category)
            }
            .show()
    }

    private inner class CategoryPagerAdapter(fragment: Fragment) :
        FragmentStateAdapter(fragment) {

        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return CategoryListFragment.newInstance(
                if (position == 0) TransactionType.EXPENSE else TransactionType.INCOME
            )
        }
    }
}
