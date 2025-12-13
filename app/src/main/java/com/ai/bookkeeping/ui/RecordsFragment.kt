package com.ai.bookkeeping.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.ui.dialog.EditTransactionDialog
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.util.Locale

class RecordsFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()
    private lateinit var adapter: TransactionAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    private lateinit var tvMonthExpense: TextView
    private lateinit var tvMonthIncome: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var chipAll: Chip
    private lateinit var chipExpense: Chip
    private lateinit var chipIncome: Chip

    private var allTransactions: List<Transaction> = emptyList()
    private var currentFilter: FilterType = FilterType.ALL

    enum class FilterType { ALL, EXPENSE, INCOME }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupChips()
        setupObservers()
    }

    private fun initViews(view: View) {
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense)
        tvMonthIncome = view.findViewById(R.id.tvMonthIncome)
        recyclerView = view.findViewById(R.id.recyclerView)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        chipAll = view.findViewById(R.id.chipAll)
        chipExpense = view.findViewById(R.id.chipExpense)
        chipIncome = view.findViewById(R.id.chipIncome)
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                showEditDialog(transaction)
            },
            onDeleteClick = { transaction ->
                viewModel.delete(transaction)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialog = EditTransactionDialog.newInstance(transaction)
        dialog.setOnSaveListener { updatedTransaction ->
            viewModel.update(updatedTransaction)
        }
        dialog.show(parentFragmentManager, "edit_transaction")
    }

    private fun setupChips() {
        chipAll.setOnClickListener {
            currentFilter = FilterType.ALL
            applyFilter()
        }

        chipExpense.setOnClickListener {
            currentFilter = FilterType.EXPENSE
            applyFilter()
        }

        chipIncome.setOnClickListener {
            currentFilter = FilterType.INCOME
            applyFilter()
        }
    }

    private fun setupObservers() {
        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            allTransactions = transactions
            applyFilter()
        }

        viewModel.currentMonthExpense.observe(viewLifecycleOwner) { expense ->
            tvMonthExpense.text = currencyFormat.format(expense ?: 0.0)
        }

        viewModel.currentMonthIncome.observe(viewLifecycleOwner) { income ->
            tvMonthIncome.text = currencyFormat.format(income ?: 0.0)
        }
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            FilterType.ALL -> allTransactions
            FilterType.EXPENSE -> allTransactions.filter { it.type == TransactionType.EXPENSE }
            FilterType.INCOME -> allTransactions.filter { it.type == TransactionType.INCOME }
        }

        adapter.submitList(filteredList)
        layoutEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
    }
}
