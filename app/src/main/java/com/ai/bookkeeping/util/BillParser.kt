package com.ai.bookkeeping.util

import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 账单解析工具
 * 支持微信和支付宝CSV账单导入
 */
object BillParser {

    enum class BillSource {
        WECHAT,
        ALIPAY
    }

    data class ParseResult(
        val transactions: List<Transaction>,
        val successCount: Int,
        val failCount: Int,
        val errors: List<String>
    )

    /**
     * 解析CSV文件
     */
    fun parseCSV(inputStream: InputStream, source: BillSource): ParseResult {
        return when (source) {
            BillSource.WECHAT -> parseWeChatBill(inputStream)
            BillSource.ALIPAY -> parseAlipayBill(inputStream)
        }
    }

    /**
     * 解析微信账单
     * 微信账单CSV格式:
     * 交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注
     */
    private fun parseWeChatBill(inputStream: InputStream): ParseResult {
        val transactions = mutableListOf<Transaction>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failCount = 0

        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var line: String?
        var lineNumber = 0
        var headerFound = false

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        while (reader.readLine().also { line = it } != null) {
            lineNumber++
            val currentLine = line ?: continue

            // 跳过微信账单头部信息
            if (currentLine.startsWith("微信支付账单明细") ||
                currentLine.startsWith("----------------------") ||
                currentLine.isBlank()) {
                continue
            }

            // 检测到表头行
            if (currentLine.contains("交易时间") && currentLine.contains("金额")) {
                headerFound = true
                continue
            }

            if (!headerFound) continue

            try {
                val columns = parseCSVLine(currentLine)
                if (columns.size < 6) continue

                val dateStr = columns[0].trim()
                val transactionType = columns[1].trim()
                val counterparty = columns[2].trim()
                val product = columns[3].trim()
                val incomeOrExpense = columns[4].trim()
                val amountStr = columns[5].trim().replace("¥", "").replace(",", "")

                // 跳过非收支记录
                if (incomeOrExpense != "支出" && incomeOrExpense != "收入") continue

                val amount = amountStr.toDoubleOrNull() ?: continue
                val date = try {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                val type = if (incomeOrExpense == "支出") TransactionType.EXPENSE else TransactionType.INCOME
                val category = guessCategory(product, counterparty, type)
                val description = product.ifEmpty { counterparty }
                val note = "来自微信账单: $transactionType - $counterparty"

                transactions.add(
                    Transaction(
                        amount = amount,
                        type = type,
                        category = category,
                        description = description,
                        note = note,
                        date = date,
                        aiParsed = false
                    )
                )
                successCount++
            } catch (e: Exception) {
                errors.add("第${lineNumber}行解析失败: ${e.message}")
                failCount++
            }
        }

        reader.close()
        return ParseResult(transactions, successCount, failCount, errors)
    }

    /**
     * 解析支付宝账单
     * 支付宝账单CSV格式:
     * 交易时间,交易分类,交易对方,对方账号,商品说明,收/支,金额,收/付款方式,交易状态,交易订单号,商家订单号,备注
     */
    private fun parseAlipayBill(inputStream: InputStream): ParseResult {
        val transactions = mutableListOf<Transaction>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failCount = 0

        val reader = BufferedReader(InputStreamReader(inputStream, "GBK")) // 支付宝通常是GBK编码
        var line: String?
        var lineNumber = 0
        var headerFound = false

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        while (reader.readLine().also { line = it } != null) {
            lineNumber++
            val currentLine = line ?: continue

            // 跳过支付宝账单头部信息
            if (currentLine.startsWith("支付宝") ||
                currentLine.startsWith("-") ||
                currentLine.startsWith("#") ||
                currentLine.isBlank()) {
                continue
            }

            // 检测到表头行
            if (currentLine.contains("交易时间") && currentLine.contains("金额")) {
                headerFound = true
                continue
            }

            if (!headerFound) continue

            try {
                val columns = parseCSVLine(currentLine)
                if (columns.size < 7) continue

                val dateStr = columns[0].trim()
                val transactionCategory = columns[1].trim()
                val counterparty = columns[2].trim()
                val product = columns[4].trim()
                val incomeOrExpense = columns[5].trim()
                val amountStr = columns[6].trim().replace("¥", "").replace(",", "")

                // 跳过非收支记录
                if (incomeOrExpense != "支出" && incomeOrExpense != "收入") continue

                val amount = amountStr.toDoubleOrNull() ?: continue
                val date = try {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                val type = if (incomeOrExpense == "支出") TransactionType.EXPENSE else TransactionType.INCOME
                val category = guessCategory(product, transactionCategory, type)
                val description = product.ifEmpty { counterparty }
                val note = "来自支付宝账单: $transactionCategory - $counterparty"

                transactions.add(
                    Transaction(
                        amount = amount,
                        type = type,
                        category = category,
                        description = description,
                        note = note,
                        date = date,
                        aiParsed = false
                    )
                )
                successCount++
            } catch (e: Exception) {
                errors.add("第${lineNumber}行解析失败: ${e.message}")
                failCount++
            }
        }

        reader.close()
        return ParseResult(transactions, successCount, failCount, errors)
    }

    /**
     * 解析CSV行，处理引号内的逗号
     */
    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    /**
     * 根据描述猜测分类
     */
    private fun guessCategory(product: String, extra: String, type: TransactionType): String {
        val text = "$product $extra".lowercase()

        if (type == TransactionType.INCOME) {
            return when {
                text.contains("工资") || text.contains("薪") -> "工资"
                text.contains("奖") -> "奖金"
                text.contains("红包") -> "红包"
                text.contains("退款") || text.contains("退") -> "其他"
                text.contains("转账") -> "其他"
                else -> "其他"
            }
        }

        return when {
            text.contains("餐") || text.contains("饭") || text.contains("食") ||
            text.contains("美团") || text.contains("饿了么") || text.contains("外卖") ||
            text.contains("咖啡") || text.contains("奶茶") -> "餐饮"

            text.contains("滴滴") || text.contains("打车") || text.contains("出行") ||
            text.contains("地铁") || text.contains("公交") || text.contains("加油") ||
            text.contains("停车") || text.contains("高速") -> "交通"

            text.contains("淘宝") || text.contains("京东") || text.contains("拼多多") ||
            text.contains("购物") || text.contains("超市") || text.contains("商城") -> "购物"

            text.contains("电影") || text.contains("游戏") || text.contains("娱乐") ||
            text.contains("KTV") || text.contains("音乐") -> "娱乐"

            text.contains("医") || text.contains("药") || text.contains("医院") -> "医疗"

            text.contains("书") || text.contains("课程") || text.contains("学习") ||
            text.contains("培训") || text.contains("教育") -> "教育"

            text.contains("房租") || text.contains("物业") || text.contains("水电") ||
            text.contains("燃气") -> "居住"

            text.contains("话费") || text.contains("流量") || text.contains("宽带") -> "通讯"

            text.contains("衣") || text.contains("服装") || text.contains("鞋") -> "服饰"

            else -> "其他"
        }
    }
}
