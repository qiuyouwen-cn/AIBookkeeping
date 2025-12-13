package com.ai.bookkeeping.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.bookkeeping.databinding.FragmentAccountManageBinding
import com.ai.bookkeeping.model.Account
import com.ai.bookkeeping.viewmodel.AccountViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * 账户管理Fragment
 */
class AccountManageFragment : Fragment() {

    private var _binding: FragmentAccountManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()
    private lateinit var adapter: AccountAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = AccountAdapter(
            onItemClick = { account ->
                showEditAccountDialog(account)
            },
            onItemLongClick = { account ->
                showAccountOptionsDialog(account)
            }
        )
        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAccounts.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
    }

    private fun observeData() {
        viewModel.allAccounts.observe(viewLifecycleOwner) { accounts ->
            adapter.submitList(accounts)
        }

        viewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)
            binding.tvTotalBalance.text = formatter.format(balance)
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AccountViewModel.OperationResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is AccountViewModel.OperationResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddAccountDialog() {
        AddAccountDialog.newInstance().show(childFragmentManager, "add_account")
    }

    private fun showEditAccountDialog(account: Account) {
        AddAccountDialog.newInstance(account).show(childFragmentManager, "edit_account")
    }

    private fun showAccountOptionsDialog(account: Account) {
        val options = mutableListOf("编辑", "调整余额")
        if (!account.isDefault) {
            options.add("设为默认")
            options.add("删除")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(account.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "编辑" -> showEditAccountDialog(account)
                    "调整余额" -> showAdjustBalanceDialog(account)
                    "设为默认" -> viewModel.setAsDefault(account)
                    "删除" -> confirmDeleteAccount(account)
                }
            }
            .show()
    }

    private fun showAdjustBalanceDialog(account: Account) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.ai.bookkeeping.R.layout.dialog_adjust_balance, null)

        val etBalance = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.ai.bookkeeping.R.id.et_balance
        )
        etBalance.setText(account.balance.toString())

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("调整余额")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val newBalance = etBalance.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.setBalance(account.id, newBalance)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteAccount(account: Account) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除账户")
            .setMessage("确定要删除账户 \"${account.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.delete(account)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
