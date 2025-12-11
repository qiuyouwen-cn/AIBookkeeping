package com.ai.bookkeeping.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.ai.bookkeeping.data.CategoryTotal
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.StatisticsViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtil {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportToCsv(
        context: Context,
        data: StatisticsViewModel.ExportData
    ): Result<String> {
        return try {
            val fileName = "账单统计_${fileDateFormat.format(Date())}.csv"
            val csvContent = buildCsvContent(data)

            val filePath = saveFile(context, fileName, csvContent, "text/csv")
            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportToTxt(
        context: Context,
        data: StatisticsViewModel.ExportData
    ): Result<String> {
        return try {
            val fileName = "账单统计_${fileDateFormat.format(Date())}.txt"
            val txtContent = buildTxtContent(data)

            val filePath = saveFile(context, fileName, txtContent, "text/plain")
            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildCsvContent(data: StatisticsViewModel.ExportData): String {
        val sb = StringBuilder()

        // 添加BOM头支持中文
        sb.append('\uFEFF')

        // 概览数据
        sb.appendLine("统计周期,${data.periodName}")
        sb.appendLine("统计时间,${dateFormat.format(Date(data.startDate))} - ${dateFormat.format(Date(data.endDate))}")
        sb.appendLine("总收入,${currencyFormat.format(data.totalIncome)}")
        sb.appendLine("总支出,${currencyFormat.format(data.totalExpense)}")
        sb.appendLine("结余,${currencyFormat.format(data.totalIncome - data.totalExpense)}")
        sb.appendLine("交易笔数,${data.transactionCount}")
        sb.appendLine()

        // 支出分类统计
        if (data.expenseCategories.isNotEmpty()) {
            sb.appendLine("支出分类统计")
            sb.appendLine("分类,金额,占比")
            val expenseTotal = data.expenseCategories.sumOf { it.total }
            data.expenseCategories.forEach { cat ->
                val percent = if (expenseTotal > 0) cat.total / expenseTotal * 100 else 0.0
                sb.appendLine("${cat.category},${currencyFormat.format(cat.total)},${String.format("%.1f%%", percent)}")
            }
            sb.appendLine()
        }

        // 收入分类统计
        if (data.incomeCategories.isNotEmpty()) {
            sb.appendLine("收入分类统计")
            sb.appendLine("分类,金额,占比")
            val incomeTotal = data.incomeCategories.sumOf { it.total }
            data.incomeCategories.forEach { cat ->
                val percent = if (incomeTotal > 0) cat.total / incomeTotal * 100 else 0.0
                sb.appendLine("${cat.category},${currencyFormat.format(cat.total)},${String.format("%.1f%%", percent)}")
            }
            sb.appendLine()
        }

        // 交易明细
        if (data.transactions.isNotEmpty()) {
            sb.appendLine("交易明细")
            sb.appendLine("日期,类型,分类,金额,备注")
            data.transactions.forEach { trans ->
                val type = if (trans.type == TransactionType.EXPENSE) "支出" else "收入"
                val desc = trans.description.replace(",", "，")
                sb.appendLine("${dateFormat.format(Date(trans.date))},$type,${trans.category},${currencyFormat.format(trans.amount)},\"$desc\"")
            }
        }

        return sb.toString()
    }

    private fun buildTxtContent(data: StatisticsViewModel.ExportData): String {
        val sb = StringBuilder()

        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("           AI记账 - 账单统计报告")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("【统计周期】${data.periodName}")
        sb.appendLine("【统计时间】${dateFormat.format(Date(data.startDate))} - ${dateFormat.format(Date(data.endDate))}")
        sb.appendLine()
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("                收支概览")
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("总收入：${currencyFormat.format(data.totalIncome)}")
        sb.appendLine("总支出：${currencyFormat.format(data.totalExpense)}")
        sb.appendLine("结  余：${currencyFormat.format(data.totalIncome - data.totalExpense)}")
        sb.appendLine("交易数：${data.transactionCount}笔")
        sb.appendLine()

        // 支出分类
        if (data.expenseCategories.isNotEmpty()) {
            sb.appendLine("───────────────────────────────────────")
            sb.appendLine("              支出分类统计")
            sb.appendLine("───────────────────────────────────────")
            val expenseTotal = data.expenseCategories.sumOf { it.total }
            data.expenseCategories.forEach { cat ->
                val percent = if (expenseTotal > 0) cat.total / expenseTotal * 100 else 0.0
                val bar = "█".repeat((percent / 5).toInt().coerceAtMost(10))
                sb.appendLine("${cat.category.padEnd(8)} ${currencyFormat.format(cat.total).padStart(12)} ${String.format("%5.1f%%", percent)} $bar")
            }
            sb.appendLine()
        }

        // 收入分类
        if (data.incomeCategories.isNotEmpty()) {
            sb.appendLine("───────────────────────────────────────")
            sb.appendLine("              收入分类统计")
            sb.appendLine("───────────────────────────────────────")
            val incomeTotal = data.incomeCategories.sumOf { it.total }
            data.incomeCategories.forEach { cat ->
                val percent = if (incomeTotal > 0) cat.total / incomeTotal * 100 else 0.0
                val bar = "█".repeat((percent / 5).toInt().coerceAtMost(10))
                sb.appendLine("${cat.category.padEnd(8)} ${currencyFormat.format(cat.total).padStart(12)} ${String.format("%5.1f%%", percent)} $bar")
            }
            sb.appendLine()
        }

        // 交易明细（最近20条）
        val recentTrans = data.transactions.take(20)
        if (recentTrans.isNotEmpty()) {
            sb.appendLine("───────────────────────────────────────")
            sb.appendLine("          近期交易明细（前20条）")
            sb.appendLine("───────────────────────────────────────")
            recentTrans.forEach { trans ->
                val type = if (trans.type == TransactionType.EXPENSE) "支出" else "收入"
                val sign = if (trans.type == TransactionType.EXPENSE) "-" else "+"
                sb.appendLine("${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(trans.date))} [$type] ${trans.category}: $sign${currencyFormat.format(trans.amount)}")
                if (trans.description.isNotBlank()) {
                    sb.appendLine("         备注: ${trans.description}")
                }
            }
        }

        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("      报告生成时间: ${dateFormat.format(Date())}")
        sb.appendLine("          由 AI记账 App 生成")
        sb.appendLine("═══════════════════════════════════════")

        return sb.toString()
    }

    private fun saveFile(context: Context, fileName: String, content: String, mimeType: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("无法创建文件")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw Exception("无法写入文件")

            "Downloads/$fileName"
        } else {
            // Android 9及以下
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }
            file.absolutePath
        }
    }

    fun shareFile(context: Context, filePath: String, mimeType: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "分享统计报告"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
