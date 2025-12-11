package com.ai.bookkeeping.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AIService {

    private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
    private const val PREFS_NAME = "ai_settings"
    private const val KEY_API_KEY = "deepseek_api_key"

    private var apiKey: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, null)
    }

    fun setApiKey(context: Context, key: String) {
        apiKey = key
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key)
            .apply()
    }

    fun getApiKey(context: Context): String? {
        if (apiKey == null) {
            apiKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_API_KEY, null)
        }
        return apiKey
    }

    fun hasApiKey(context: Context): Boolean {
        return !getApiKey(context).isNullOrEmpty()
    }

    suspend fun chat(
        message: String,
        systemPrompt: String = "你是一个专业的财务分析助手，擅长分析个人收支数据并提供理财建议。请用简洁、专业的语言回复。",
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey(context)
            if (key.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先设置AI API密钥"))
            }

            val url = URL(DEEPSEEK_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $key")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }

            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", message)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 2000)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                Result.success(content)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Result.failure(Exception("API请求失败: $responseCode - $errorStream"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeFinancialData(
        context: Context,
        totalIncome: Double,
        totalExpense: Double,
        categoryBreakdown: List<Pair<String, Double>>,
        period: String,
        transactionCount: Int
    ): Result<String> {
        val prompt = buildString {
            appendLine("请分析以下财务数据并给出专业建议：")
            appendLine()
            appendLine("【统计周期】$period")
            appendLine("【交易笔数】${transactionCount}笔")
            appendLine("【总收入】￥${String.format("%.2f", totalIncome)}")
            appendLine("【总支出】￥${String.format("%.2f", totalExpense)}")
            appendLine("【结余】￥${String.format("%.2f", totalIncome - totalExpense)}")
            appendLine()
            appendLine("【分类明细】")
            categoryBreakdown.forEach { (category, amount) ->
                val percent = if (totalExpense > 0) (amount / totalExpense * 100) else 0.0
                appendLine("- $category: ￥${String.format("%.2f", amount)} (${String.format("%.1f", percent)}%)")
            }
            appendLine()
            appendLine("请从以下几个方面进行分析：")
            appendLine("1. 整体收支状况评价")
            appendLine("2. 消费结构分析")
            appendLine("3. 潜在的节省空间")
            appendLine("4. 理财建议")
            appendLine("5. 下一步行动建议")
        }

        return chat(
            message = prompt,
            systemPrompt = """你是一个专业的个人财务分析师。请根据用户提供的收支数据进行深入分析，给出具体、可操作的建议。
                |分析要点：
                |1. 关注收支平衡情况
                |2. 识别可能的过度消费领域
                |3. 提供具体的节省建议
                |4. 考虑用户的生活质量
                |5. 建议要实际可行
                |请用友好、专业的语气回复，适当使用emoji让回复更生动。""".trimMargin(),
            context = context
        )
    }

    suspend fun parseTransaction(
        context: Context,
        input: String
    ): Result<ParsedTransaction> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey(context)
            if (key.isNullOrEmpty()) {
                // 如果没有API密钥，使用本地解析
                return@withContext parseTransactionLocally(input)
            }

            val prompt = """请解析以下记账文本，提取金额、分类、描述和类型（收入/支出）。
                |
                |文本：$input
                |
                |请以JSON格式返回，格式如下：
                |{"amount": 数字, "category": "分类名", "description": "描述", "type": "expense"或"income"}
                |
                |分类参考：餐饮、交通、购物、娱乐、医疗、教育、住房、通讯、服饰、工资、奖金、投资、兼职、红包、其他
                |
                |只返回JSON，不要其他内容。""".trimMargin()

            val result = chat(prompt, "你是一个记账文本解析助手，只返回JSON格式数据。", context)

            result.fold(
                onSuccess = { response ->
                    try {
                        val cleanJson = response.trim()
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        val json = JSONObject(cleanJson)
                        Result.success(ParsedTransaction(
                            amount = json.getDouble("amount"),
                            category = json.getString("category"),
                            description = json.optString("description", ""),
                            isExpense = json.getString("type") == "expense"
                        ))
                    } catch (e: Exception) {
                        parseTransactionLocally(input)
                    }
                },
                onFailure = { parseTransactionLocally(input) }
            )
        } catch (e: Exception) {
            parseTransactionLocally(input)
        }
    }

    private fun parseTransactionLocally(input: String): Result<ParsedTransaction> {
        // 本地解析逻辑（原有的AIParser逻辑）
        val amountPattern = Regex("(\\d+\\.?\\d*)")
        val amountMatch = amountPattern.find(input)
        val amount = amountMatch?.value?.toDoubleOrNull() ?: return Result.failure(Exception("无法识别金额"))

        val isExpense = !input.contains("收入") &&
                       !input.contains("工资") &&
                       !input.contains("奖金") &&
                       !input.contains("红包") &&
                       !input.contains("转入")

        val category = when {
            input.contains("餐") || input.contains("饭") || input.contains("吃") || input.contains("外卖") -> "餐饮"
            input.contains("车") || input.contains("油") || input.contains("打车") || input.contains("地铁") || input.contains("公交") -> "交通"
            input.contains("买") || input.contains("购") || input.contains("淘宝") || input.contains("京东") -> "购物"
            input.contains("电影") || input.contains("游戏") || input.contains("娱乐") || input.contains("KTV") -> "娱乐"
            input.contains("医") || input.contains("药") || input.contains("病") -> "医疗"
            input.contains("书") || input.contains("学") || input.contains("课") || input.contains("培训") -> "教育"
            input.contains("房") || input.contains("租") || input.contains("水电") -> "住房"
            input.contains("话费") || input.contains("流量") || input.contains("网费") -> "通讯"
            input.contains("衣") || input.contains("鞋") || input.contains("服") -> "服饰"
            input.contains("工资") || input.contains("薪") -> "工资"
            input.contains("奖") -> "奖金"
            input.contains("理财") || input.contains("投资") || input.contains("股") || input.contains("基金") -> "投资"
            input.contains("兼职") -> "兼职"
            input.contains("红包") -> "红包"
            else -> "其他"
        }

        val description = input.replace(amountMatch?.value ?: "", "")
            .replace("元", "")
            .replace("块", "")
            .replace("￥", "")
            .replace("¥", "")
            .trim()

        return Result.success(ParsedTransaction(
            amount = amount,
            category = category,
            description = description,
            isExpense = isExpense
        ))
    }

    data class ParsedTransaction(
        val amount: Double,
        val category: String,
        val description: String,
        val isExpense: Boolean
    )
}
