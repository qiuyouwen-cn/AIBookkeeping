package com.ai.bookkeeping.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.DialogTransferBinding
import com.ai.bookkeeping.model.Account
import com.ai.bookkeeping.model.AccountType
import com.ai.bookkeeping.viewmodel.AccountViewModel
import com.ai.bookkeeping.viewmodel.TransferViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 转账对话框
 */
class TransferDialog : BottomSheetDialogFragment() {

    private var _binding: DialogTransferBinding? = null
    private val binding get() = _binding!!

    private val accountViewModel: AccountViewModel by viewModels()
    private val transferViewModel: TransferViewModel by viewModels()

    private var notebookId: Long = 1
    private var accounts = listOf<Account>()
    private var fromAccount: Account? = null
    private var toAccount: Account? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notebookId = arguments?.getLong(ARG_NOTEBOOK_ID) ?: 1

        setupListeners()
        observeData()
    }

    private fun setupListeners() {
        binding.cardFromAccount.setOnClickListener {
            showAccountPicker(true)
        }

        binding.cardToAccount.setOnClickListener {
            showAccountPicker(false)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            executeTransfer()
        }
    }

    private fun observeData() {
        accountViewModel.allAccounts.observe(viewLifecycleOwner) { accountList ->
            accounts = accountList
        }

        transferViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is TransferViewModel.OperationResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                is TransferViewModel.OperationResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAccountPicker(isFrom: Boolean) {
        if (accounts.isEmpty()) {
            Toast.makeText(requireContext(), "暂无可用账户", Toast.LENGTH_SHORT).show()
            return
        }

        val excludeAccount = if (isFrom) toAccount else fromAccount
        val availableAccounts = accounts.filter { it.id != excludeAccount?.id }

        if (availableAccounts.isEmpty()) {
            Toast.makeText(requireContext(), "需要至少两个账户才能转账", Toast.LENGTH_SHORT).show()
            return
        }

        val accountNames = availableAccounts.map { "${it.name} (¥${String.format("%.2f", it.balance)})" }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(if (isFrom) "选择转出账户" else "选择转入账户")
            .setItems(accountNames) { _, which ->
                val selectedAccount = availableAccounts[which]
                if (isFrom) {
                    fromAccount = selectedAccount
                    updateFromAccountUI()
                } else {
                    toAccount = selectedAccount
                    updateToAccountUI()
                }
            }
            .show()
    }

    private fun updateFromAccountUI() {
        fromAccount?.let { account ->
            binding.tvFromAccount.text = account.name
            binding.ivFromAccountIcon.setImageResource(getAccountIconRes(account.type))
            try {
                binding.ivFromAccountIcon.setColorFilter(Color.parseColor(account.color))
            } catch (e: Exception) {
                binding.ivFromAccountIcon.setColorFilter(Color.parseColor("#5B5FE3"))
            }
        }
    }

    private fun updateToAccountUI() {
        toAccount?.let { account ->
            binding.tvToAccount.text = account.name
            binding.ivToAccountIcon.setImageResource(getAccountIconRes(account.type))
            try {
                binding.ivToAccountIcon.setColorFilter(Color.parseColor(account.color))
            } catch (e: Exception) {
                binding.ivToAccountIcon.setColorFilter(Color.parseColor("#5B5FE3"))
            }
        }
    }

    private fun getAccountIconRes(type: AccountType): Int {
        return when (type) {
            AccountType.CASH -> R.drawable.ic_cash
            AccountType.BANK_CARD -> R.drawable.ic_bank
            AccountType.CREDIT_CARD -> R.drawable.ic_credit_card
            AccountType.ALIPAY -> R.drawable.ic_alipay
            AccountType.WECHAT -> R.drawable.ic_wechat
            AccountType.OTHER -> R.drawable.ic_wallet
        }
    }

    private fun executeTransfer() {
        if (fromAccount == null) {
            Toast.makeText(requireContext(), "请选择转出账户", Toast.LENGTH_SHORT).show()
            return
        }

        if (toAccount == null) {
            Toast.makeText(requireContext(), "请选择转入账户", Toast.LENGTH_SHORT).show()
            return
        }

        val amountStr = binding.etAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            binding.tilAmount.error = "请输入转账金额"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = "请输入有效金额"
            return
        }

        if (amount > fromAccount!!.balance) {
            binding.tilAmount.error = "余额不足"
            return
        }

        val fee = binding.etFee.text.toString().toDoubleOrNull() ?: 0.0
        val note = binding.etNote.text.toString().trim()

        transferViewModel.executeTransfer(
            notebookId = notebookId,
            fromAccountId = fromAccount!!.id,
            toAccountId = toAccount!!.id,
            amount = amount,
            fee = fee,
            note = note
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NOTEBOOK_ID = "notebook_id"

        fun newInstance(notebookId: Long = 1): TransferDialog {
            return TransferDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_NOTEBOOK_ID, notebookId)
                }
            }
        }
    }
}
