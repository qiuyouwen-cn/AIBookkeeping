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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.FragmentPhotoRecordBinding
import com.ai.bookkeeping.model.ExpenseCategories
import com.ai.bookkeeping.model.IncomeCategories
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 拍照记账Fragment - 支持多图片上传(最多4张)
 */
class PhotoRecordFragment : Fragment() {

    private var _binding: FragmentPhotoRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()
    private var currentType = TransactionType.EXPENSE

    // 图片路径列表
    private val photoPaths = mutableListOf<String?>(null, null, null, null)
    private var currentPhotoIndex = 0
    private var photoUri: Uri? = null

    // 图片视图数组
    private lateinit var photoViews: Array<ImageView>
    private lateinit var addIcons: Array<ImageView>
    private lateinit var removeButtons: Array<ImageButton>
    private lateinit var photoCards: Array<View>

    // 拍照启动器
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            updatePhotoUI(currentPhotoIndex, photoUri!!)
        }
    }

    // 从相册选择启动器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 复制到应用目录
            val file = createImageFile()
            photoPaths[currentPhotoIndex] = file.absolutePath
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            updatePhotoUI(currentPhotoIndex, uri)
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

        initPhotoViews()
        setupTypeToggle()
        setupCategorySpinner()
        setupClickListeners()
    }

    private fun initPhotoViews() {
        photoViews = arrayOf(
            binding.ivPhoto1,
            binding.ivPhoto2,
            binding.ivPhoto3,
            binding.ivPhoto4
        )

        addIcons = arrayOf(
            binding.ivAdd1,
            binding.ivAdd2,
            binding.ivAdd3,
            binding.ivAdd4
        )

        removeButtons = arrayOf(
            binding.btnRemove1,
            binding.btnRemove2,
            binding.btnRemove3,
            binding.btnRemove4
        )

        photoCards = arrayOf(
            binding.cardPhoto1,
            binding.cardPhoto2,
            binding.cardPhoto3,
            binding.cardPhoto4
        )

        // 设置点击事件
        for (i in 0..3) {
            photoCards[i].setOnClickListener {
                if (photoPaths[i] == null) {
                    currentPhotoIndex = i
                    showPhotoOptions()
                }
            }

            removeButtons[i].setOnClickListener {
                removePhoto(i)
            }
        }
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
        // 拍照按钮
        binding.btnCamera.setOnClickListener {
            currentPhotoIndex = findNextEmptySlot()
            if (currentPhotoIndex >= 0) {
                checkCameraPermissionAndOpen()
            } else {
                Toast.makeText(requireContext(), "已达到最大图片数量(4张)", Toast.LENGTH_SHORT).show()
            }
        }

        // 相册按钮
        binding.btnGallery.setOnClickListener {
            currentPhotoIndex = findNextEmptySlot()
            if (currentPhotoIndex >= 0) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(requireContext(), "已达到最大图片数量(4张)", Toast.LENGTH_SHORT).show()
            }
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun findNextEmptySlot(): Int {
        for (i in 0..3) {
            if (photoPaths[i] == null) return i
        }
        return -1
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
        photoPaths[currentPhotoIndex] = photoFile.absolutePath
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

    private fun updatePhotoUI(index: Int, uri: Uri) {
        photoViews[index].setImageURI(uri)
        photoViews[index].visibility = View.VISIBLE
        addIcons[index].visibility = View.GONE
        removeButtons[index].visibility = View.VISIBLE
    }

    private fun removePhoto(index: Int) {
        // 删除文件
        photoPaths[index]?.let { path ->
            File(path).delete()
        }
        photoPaths[index] = null

        // 更新UI
        photoViews[index].setImageDrawable(null)
        photoViews[index].visibility = View.GONE
        addIcons[index].visibility = View.VISIBLE
        removeButtons[index].visibility = View.GONE
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

        // 转换为JSON数组
        val validPaths = photoPaths.filterNotNull()
        val imagePathsJson = if (validPaths.isNotEmpty()) {
            JSONArray(validPaths).toString()
        } else {
            null
        }

        val transaction = Transaction(
            amount = amount,
            type = currentType,
            category = category,
            description = description,
            note = note,
            aiParsed = false,
            imagePath = validPaths.firstOrNull(),  // 兼容旧字段
            imagePaths = imagePathsJson
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
