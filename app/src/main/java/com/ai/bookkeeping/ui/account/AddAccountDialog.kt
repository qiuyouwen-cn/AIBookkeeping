package com.ai.bookkeeping.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.DialogAddAccountBinding
import com.ai.bookkeeping.model.Account
import com.ai.bookkeeping.model.AccountType
import com.ai.bookkeeping.viewmodel.AccountViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 添加/编辑账户对话框
 */
class AddAccountDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels({ requireParentFragment() })

    private var editingAccount: Account? = null
    private var selectedType: AccountType = AccountType.CASH

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editingAccount = arguments?.getParcelable(ARG_ACCOUNT)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        editingAccount?.let { account ->
            binding.tvDialogTitle.text = "编辑账户"
            binding.etAccountName.setText(account.name)
            binding.etBalance.setText(account.balance.toString())
            binding.switchDefault.isChecked = account.isDefault

            // 设置账户类型
            selectedType = account.type
            when (account.type) {
                AccountType.CASH -> binding.chipCash.isChecked = true
                AccountType.BANK_CARD -> binding.chipBank.isChecked = true
                AccountType.CREDIT_CARD -> binding.chipCredit.isChecked = true
                AccountType.ALIPAY -> binding.chipAlipay.isChecked = true
                AccountType.WECHAT -> binding.chipWechat.isChecked = true
                AccountType.OTHER -> binding.chipOther.isChecked = true
            }
        }
    }

    private fun setupListeners() {
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedType = when (checkedIds[0]) {
                    R.id.chip_cash -> AccountType.CASH
                    R.id.chip_bank -> AccountType.BANK_CARD
                    R.id.chip_credit -> AccountType.CREDIT_CARD
                    R.id.chip_alipay -> AccountType.ALIPAY
                    R.id.chip_wechat -> AccountType.WECHAT
                    R.id.chip_other -> AccountType.OTHER
                    else -> AccountType.CASH
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveAccount()
        }
    }

    private fun saveAccount() {
        val name = binding.etAccountName.text.toString().trim()
        if (name.isEmpty()) {
            binding.tilAccountName.error = "请输入账户名称"
            return
        }

        val balance = binding.etBalance.text.toString().toDoubleOrNull() ?: 0.0
        val isDefault = binding.switchDefault.isChecked

        val color = getColorForType(selectedType)
        val icon = getIconForType(selectedType)

        val account = editingAccount?.copy(
            name = name,
            type = selectedType,
            balance = balance,
            color = color,
            icon = icon,
            isDefault = isDefault
        ) ?: Account(
            name = name,
            type = selectedType,
            balance = balance,
            color = color,
            icon = icon,
            isDefault = isDefault
        )

        if (editingAccount != null) {
            viewModel.update(account)
        } else {
            viewModel.insert(account)
        }

        dismiss()
    }

    private fun getColorForType(type: AccountType): String {
        return when (type) {
            AccountType.CASH -> "#26DE81"
            AccountType.BANK_CARD -> "#45AAF2"
            AccountType.CREDIT_CARD -> "#FC5C65"
            AccountType.ALIPAY -> "#00AAEE"
            AccountType.WECHAT -> "#07C160"
            AccountType.OTHER -> "#A55EEA"
        }
    }

    private fun getIconForType(type: AccountType): String {
        return when (type) {
            AccountType.CASH -> "ic_cash"
            AccountType.BANK_CARD -> "ic_bank"
            AccountType.CREDIT_CARD -> "ic_credit_card"
            AccountType.ALIPAY -> "ic_alipay"
            AccountType.WECHAT -> "ic_wechat"
            AccountType.OTHER -> "ic_wallet"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACCOUNT = "account"

        fun newInstance(account: Account? = null): AddAccountDialog {
            return AddAccountDialog().apply {
                arguments = Bundle().apply {
                    account?.let { putParcelable(ARG_ACCOUNT, it) }
                }
            }
        }
    }
}
