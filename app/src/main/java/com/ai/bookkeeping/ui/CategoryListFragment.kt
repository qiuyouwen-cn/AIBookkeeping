package com.ai.bookkeeping.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.adapter.CategoryAdapter
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.CategoryViewModel

class CategoryListFragment : Fragment() {

    companion object {
        private const val ARG_TYPE = "type"

        fun newInstance(type: TransactionType): CategoryListFragment {
            return CategoryListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type.name)
                }
            }
        }
    }

    private val viewModel: CategoryViewModel by viewModels({ requireParentFragment() })
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: CategoryAdapter
    private lateinit var type: TransactionType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = TransactionType.valueOf(
            arguments?.getString(ARG_TYPE) ?: TransactionType.EXPENSE.name
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerCategories = view.findViewById(R.id.recyclerCategories)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        setupRecyclerView()
        observeCategories()
    }

    private fun setupRecyclerView() {
        val parentFragment = requireParentFragment() as? CategoryManageFragment

        adapter = CategoryAdapter(
            onEditClick = { category ->
                parentFragment?.showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                parentFragment?.showDeleteCategoryDialog(category)
            },
            onItemClick = { category ->
                // Toggle expand if it's a parent category
                if (category.parentId == null) {
                    adapter.toggleExpand(category.id)
                }
            },
            onAddSubClick = { parentCategory ->
                parentFragment?.showAddCategoryDialog(parentCategory)
            }
        )

        recyclerCategories.adapter = adapter
    }

    private fun observeCategories() {
        viewModel.getCategoriesByType(type).observe(viewLifecycleOwner) { categories ->
            adapter.setCategories(categories)
            tvEmpty.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
            recyclerCategories.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
