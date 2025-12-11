package com.ai.bookkeeping.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ai.bookkeeping.databinding.FragmentPhotoRecordBinding
import com.ai.bookkeeping.model.ExpenseCategories
import com.ai.bookkeeping.model.IncomeCategories
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 拍照记账Fragment
 */
class PhotoRecordFragment : Fragment() {

    private var _binding: FragmentPhotoRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()
    private var currentType = TransactionType.EXPENSE
    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    // 拍照启动器
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            binding.ivPhoto.setImageURI(photoUri)
            binding.ivPhoto.visibility = View.VISIBLE
            binding.tvPhotoHint.visibility = View.GONE
        }
    }

    // 从相册选择启动器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 复制到应用目录
            val file = createImageFile()
            currentPhotoPath = file.absolutePath
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            binding.ivPhoto.setImageURI(uri)
            binding.ivPhoto.visibility = View.VISIBLE
            binding.tvPhotoHint.visibility = View.GONE
        }
    }

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeToggle()
        setupCategorySpinner()
        setupClickListeners()
    }

    private fun setupTypeToggle() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentType = when (checkedId) {
                    binding.btnExpense.id -> TransactionType.EXPENSE
                    binding.btnIncome.id -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
                updateCategorySpinner()
            }
        }
        binding.btnExpense.isChecked = true
    }

    private fun setupCategorySpinner() {
        updateCategorySpinner()
    }

    private fun updateCategorySpinner() {
        val categories = if (currentType == TransactionType.EXPENSE) {
            ExpenseCategories.list
        } else {
            IncomeCategories.list
        }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupClickListeners() {
        // 点击拍照区域
        binding.cardPhoto.setOnClickListener {
            showPhotoOptions()
        }

        // 拍照按钮
        binding.btnCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        // 相册按钮
        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun showPhotoOptions() {
        val options = arrayOf("拍照", "从相册选择")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("添加照片")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun saveTransaction() {
        val amountStr = binding.etAmount.text.toString()
        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "请输入有效金额", Toast.LENGTH_SHORT).show()
            return
        }

        val category = binding.spinnerCategory.selectedItem.toString()
        val description = binding.etDescription.text.toString().ifEmpty { category }
        val note = binding.etNote.text.toString()

        val transaction = Transaction(
            amount = amount,
            type = currentType,
            category = category,
            description = description,
            note = note,
            aiParsed = false,
            imagePath = currentPhotoPath
        )

        viewModel.insert(transaction)
        Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
