package com.ai.bookkeeping.ui.notebook

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.bookkeeping.databinding.FragmentNotebookManageBinding
import com.ai.bookkeeping.model.Notebook
import com.ai.bookkeeping.model.NotebookWithStats
import com.ai.bookkeeping.viewmodel.NotebookViewModel

/**
 * 账本管理Fragment
 */
class NotebookManageFragment : Fragment() {

    private var _binding: FragmentNotebookManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotebookViewModel by viewModels()
    private lateinit var adapter: NotebookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotebookManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeData()
        viewModel.loadNotebooksWithStats()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotebookAdapter(
            onItemClick = { notebookWithStats ->
                viewModel.switchNotebook(notebookWithStats.notebook)
                Toast.makeText(requireContext(), "已切换到「${notebookWithStats.notebook.name}」", Toast.LENGTH_SHORT).show()
            },
            onMoreClick = { notebookWithStats ->
                showNotebookOptionsDialog(notebookWithStats)
            },
            currentNotebookId = viewModel.currentNotebook.value?.id ?: 1
        )
        binding.rvNotebooks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotebooks.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddNotebook.setOnClickListener {
            showAddNotebookDialog()
        }
    }

    private fun observeData() {
        viewModel.notebooksWithStats.observe(viewLifecycleOwner) { notebooks ->
            adapter.submitList(notebooks)
        }

        viewModel.currentNotebook.observe(viewLifecycleOwner) { notebook ->
            notebook?.let {
                adapter.updateCurrentNotebook(it.id)
            }
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NotebookViewModel.OperationResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    viewModel.loadNotebooksWithStats()
                }
                is NotebookViewModel.OperationResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddNotebookDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.ai.bookkeeping.R.layout.dialog_add_notebook, null)

        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.ai.bookkeeping.R.id.et_notebook_name
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("新建账本")
            .setView(dialogView)
            .setPositiveButton("创建") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.insert(Notebook(name = name))
                } else {
                    Toast.makeText(requireContext(), "请输入账本名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNotebookOptionsDialog(notebookWithStats: NotebookWithStats) {
        val notebook = notebookWithStats.notebook
        val options = mutableListOf("切换到此账本", "编辑")
        if (!notebook.isDefault) {
            options.add("设为默认")
            options.add("删除")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(notebook.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "切换到此账本" -> {
                        viewModel.switchNotebook(notebook)
                        Toast.makeText(requireContext(), "已切换到「${notebook.name}」", Toast.LENGTH_SHORT).show()
                    }
                    "编辑" -> showEditNotebookDialog(notebook)
                    "设为默认" -> viewModel.setAsDefault(notebook)
                    "删除" -> confirmDeleteNotebook(notebook)
                }
            }
            .show()
    }

    private fun showEditNotebookDialog(notebook: Notebook) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.ai.bookkeeping.R.layout.dialog_add_notebook, null)

        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.ai.bookkeeping.R.id.et_notebook_name
        )
        etName.setText(notebook.name)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("编辑账本")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.update(notebook.copy(name = name))
                } else {
                    Toast.makeText(requireContext(), "请输入账本名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteNotebook(notebook: Notebook) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除账本")
            .setMessage("确定要删除账本「${notebook.name}」吗？\n该账本下的所有记录将被删除。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.delete(notebook)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
