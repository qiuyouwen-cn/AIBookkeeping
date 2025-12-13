package com.ai.bookkeeping.ui.import

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.FragmentImportBillBinding
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.util.BillParser
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 账单导入Fragment
 */
class ImportBillFragment : Fragment() {

    private var _binding: FragmentImportBillBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()

    private var selectedUri: Uri? = null
    private var parsedTransactions: List<Transaction> = emptyList()
    private var currentSource = BillParser.BillSource.WECHAT

    // 文件选择启动器
    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            parseFile(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // 账单来源选择
        binding.radioGroupSource.setOnCheckedChangeListener { _, checkedId ->
            currentSource = when (checkedId) {
                R.id.radio_wechat -> BillParser.BillSource.WECHAT
                R.id.radio_alipay -> BillParser.BillSource.ALIPAY
                else -> BillParser.BillSource.WECHAT
            }
            // 切换来源后重新解析
            selectedUri?.let { parseFile(it) }
        }

        // 选择文件
        binding.btnSelectFile.setOnClickListener {
            selectFileLauncher.launch("text/*")
        }

        // 开始导入
        binding.btnImport.setOnClickListener {
            importTransactions()
        }
    }

    private fun parseFile(uri: Uri) {
        binding.layoutProgress.visibility = View.VISIBLE
        binding.tvProgress.text = "解析中..."
        binding.progressBar.isIndeterminate = true
        binding.btnImport.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        showError("无法读取文件")
                    }
                    return@launch
                }

                val result = BillParser.parseCSV(inputStream, currentSource)
                parsedTransactions = result.transactions

                withContext(Dispatchers.Main) {
                    binding.layoutProgress.visibility = View.GONE
                    binding.cardFileInfo.visibility = View.VISIBLE

                    // 获取文件名
                    val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                    val fileName = cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) it.getString(nameIndex) else "未知文件"
                        } else "未知文件"
                    } ?: "未知文件"

                    binding.tvFileName.text = fileName
                    binding.tvRecordCount.text = "解析到 ${result.successCount} 条记录" +
                            if (result.failCount > 0) "，${result.failCount} 条失败" else ""

                    binding.btnImport.isEnabled = result.successCount > 0

                    if (result.errors.isNotEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "部分记录解析失败，请检查文件格式",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("解析失败: ${e.message}")
                }
            }
        }
    }

    private fun importTransactions() {
        if (parsedTransactions.isEmpty()) {
            Toast.makeText(requireContext(), "没有可导入的记录", Toast.LENGTH_SHORT).show()
            return
        }

        binding.layoutProgress.visibility = View.VISIBLE
        binding.progressBar.isIndeterminate = false
        binding.progressBar.max = parsedTransactions.size
        binding.progressBar.progress = 0
        binding.btnImport.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            var importedCount = 0
            var failedCount = 0

            for ((index, transaction) in parsedTransactions.withIndex()) {
                try {
                    viewModel.insert(transaction)
                    importedCount++
                } catch (e: Exception) {
                    failedCount++
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.progress = index + 1
                    binding.tvProgress.text = "导入中... ${index + 1}/${parsedTransactions.size}"
                }
            }

            withContext(Dispatchers.Main) {
                binding.layoutProgress.visibility = View.GONE

                val message = if (failedCount == 0) {
                    "成功导入 $importedCount 条记录"
                } else {
                    "导入完成：成功 $importedCount 条，失败 $failedCount 条"
                }

                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                // 导入完成后返回
                findNavController().popBackStack()
            }
        }
    }

    private fun showError(message: String) {
        binding.layoutProgress.visibility = View.GONE
        binding.cardFileInfo.visibility = View.GONE
        binding.btnImport.isEnabled = false
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
