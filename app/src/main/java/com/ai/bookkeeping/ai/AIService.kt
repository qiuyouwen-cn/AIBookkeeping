package com.ai.bookkeeping.ai

import android.content.Context
import com.ai.bookkeeping.util.AIParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

            // 获取当前日期用于参考
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Calendar.getInstance().time)

            val prompt = """请解析以下记账文本，提取金额、分类、描述、类型和日期时间。

文本：$input

今天日期：$today

请以JSON格式返回，格式如下：
{
  "amount": 数字,
  "category": "分类名",
  "description": "简短描述",
  "type": "expense"或"income",
  "date_offset": 天数偏移(今天=0,昨天=-1,前天=-2),
  "hour": 小时(0-23，无法确定则为-1),
  "minute": 分钟(0-59，无法确定则为0),
  "time_desc": "时间描述如'昨天中午'"
}

日期解析规则：
- "今天"/"今日" -> date_offset: 0
- "昨天"/"昨日" -> date_offset: -1
- "前天" -> date_offset: -2
- "大前天" -> date_offset: -3
- "上周" -> date_offset: -7
- 无日期词 -> date_offset: 0

时间解析规则：
- "早上"/"早餐" -> hour: 7
- "上午" -> hour: 10
- "中午"/"午餐"/"午饭" -> hour: 12
- "下午" -> hour: 15
- "傍晚" -> hour: 18
- "晚上"/"晚餐"/"晚饭" -> hour: 19
- "宵夜"/"夜宵" -> hour: 22
- 无时间词 -> hour: -1

分类参考：餐饮、交通、购物、娱乐、医疗、教育、居住、通讯、服饰、工资、奖金、投资、兼职、红包、其他

只返回JSON，不要其他内容。""".trimMargin()

            val result = chat(prompt, "你是一个智能记账助手，专门解析自然语言记账文本。只返回JSON格式数据，不要任何解释。", context)

            result.fold(
                onSuccess = { response ->
                    try {
                        val cleanJson = response.trim()
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        val json = JSONObject(cleanJson)

                        // 计算实际日期时间
                        val calendar = Calendar.getInstance()
                        val dateOffset = json.optInt("date_offset", 0)
                        val hour = json.optInt("hour", -1)
                        val minute = json.optInt("minute", 0)

                        calendar.add(Calendar.DAY_OF_YEAR, dateOffset)
                        if (hour >= 0) {
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                        }

                        Result.success(ParsedTransaction(
                            amount = json.getDouble("amount"),
                            category = json.getString("category"),
                            description = json.optString("description", ""),
                            isExpense = json.getString("type") == "expense",
                            date = calendar.timeInMillis,
                            timeDescription = json.optString("time_desc", "")
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
        // 使用增强的AIParser进行本地解析
        val amountPattern = Regex("(\\d+\\.?\\d*)")
        val amountMatch = amountPattern.find(input)
        val amount = amountMatch?.value?.toDoubleOrNull() ?: return Result.failure(Exception("无法识别金额"))

        // 判断收入/支出
        val incomeKeywords = listOf("收入", "工资", "薪水", "奖金", "红包", "转入", "收到", "赚")
        val isExpense = incomeKeywords.none { input.contains(it) }

        // 使用AIParser进行智能分类
        val category = determineCategory(input, isExpense)

        // 使用AIParser解析日期时间
        val date = AIParser.parseDateTime(input)

        // 生成时间描述
        val timeDesc = buildTimeDescription(input)

        // 生成描述
        val description = generateDescription(input, category)

        return Result.success(ParsedTransaction(
            amount = amount,
            category = category,
            description = description,
            isExpense = isExpense,
            date = date,
            timeDescription = timeDesc
        ))
    }

    /**
     * 智能分类识别 - 使用评分机制
     */
    private fun determineCategory(input: String, isExpense: Boolean): String {
        // 支出分类关键词
        val expenseCategoryKeywords = mapOf(
            "餐饮" to listOf("早餐", "午餐", "晚餐", "早饭", "午饭", "晚饭", "吃饭", "外卖",
                "奶茶", "咖啡", "饮料", "零食", "水果", "买菜", "超市", "食品",
                "餐厅", "火锅", "烧烤", "小吃", "面包", "蛋糕", "宵夜", "夜宵", "聚餐",
                "食堂", "盒饭", "快餐", "汉堡", "炸鸡", "饭", "菜", "吃", "餐"),
            "交通" to listOf("打车", "出租", "滴滴", "公交", "地铁", "高铁", "火车", "飞机",
                "机票", "车票", "加油", "油费", "停车", "过路费", "共享单车", "骑车",
                "出行", "uber", "曹操", "首汽", "嘀嗒", "车", "票"),
            "购物" to listOf("淘宝", "京东", "拼多多", "网购", "购物", "买东西", "天猫",
                "唯品会", "买", "购"),
            "娱乐" to listOf("电影", "游戏", "KTV", "唱歌", "旅游", "门票", "演出", "酒吧",
                "健身", "运动", "球", "游泳", "瑜伽", "景点", "玩"),
            "医疗" to listOf("医院", "药", "看病", "体检", "挂号", "医药", "诊所", "牙科",
                "眼科", "医", "病"),
            "教育" to listOf("课程", "培训", "学费", "书", "教材", "考试", "补习", "网课",
                "学习", "学", "课"),
            "居住" to listOf("房租", "租金", "物业", "水电", "电费", "水费", "燃气", "网费",
                "宽带", "暖气", "维修", "装修", "房", "租"),
            "通讯" to listOf("话费", "手机费", "流量", "充值", "套餐"),
            "服饰" to listOf("衣服", "裤子", "鞋", "帽子", "包", "配饰", "化妆品", "护肤",
                "洗护", "内衣", "袜子", "外套", "衣", "服")
        )

        // 收入分类关键词
        val incomeCategoryKeywords = mapOf(
            "工资" to listOf("工资", "薪水", "薪资", "月薪", "底薪", "发工资", "薪"),
            "奖金" to listOf("奖金", "年终奖", "绩效", "提成", "分红", "奖励", "奖"),
            "投资" to listOf("利息", "股票", "基金", "理财", "分红", "收益", "回报", "投资"),
            "兼职" to listOf("兼职", "副业", "外快", "私活", "零工"),
            "红包" to listOf("红包", "转账", "礼金", "随份子", "压岁钱")
        )

        val categoryKeywords = if (isExpense) expenseCategoryKeywords else incomeCategoryKeywords
        val defaultCategory = if (isExpense) "其他" else "其他"

        // 使用评分机制找到最匹配的分类
        val scores = mutableMapOf<String, Int>()
        for ((category, keywords) in categoryKeywords) {
            var score = 0
            for (keyword in keywords) {
                if (input.contains(keyword)) {
                    // 更长的关键词给更高的分数
                    score += keyword.length
                }
            }
            if (score > 0) {
                scores[category] = score
            }
        }

        return scores.maxByOrNull { it.value }?.key ?: defaultCategory
    }

    /**
     * 生成时间描述
     */
    private fun buildTimeDescription(input: String): String {
        val parts = mutableListOf<String>()

        // 检查日期词
        val dateWords = listOf("今天", "今日", "昨天", "昨日", "前天", "大前天", "上周", "上星期")
        for (word in dateWords) {
            if (input.contains(word)) {
                parts.add(word)
                break
            }
        }

        // 检查时间词
        val timeWords = listOf("凌晨", "早上", "早晨", "上午", "中午", "下午", "傍晚", "晚上",
            "深夜", "早餐", "午餐", "晚餐", "早饭", "午饭", "晚饭", "宵夜", "夜宵")
        for (word in timeWords) {
            if (input.contains(word)) {
                parts.add(word)
                break
            }
        }

        return parts.joinToString("")
    }

    /**
     * 生成描述
     */
    private fun generateDescription(input: String, category: String): String {
        var desc = input
            .replace(Regex("\\d+\\.?\\d*\\s*(元|块|￥|\$)?"), "")
            .replace(Regex("(花了|花费|消费|支出|收入|收到|赚了|得到)"), "")
            .replace(Regex("(今天|今日|昨天|昨日|前天|大前天|上周|上星期)"), "")
            .replace(Regex("(凌晨|早上|早晨|上午|中午|下午|傍晚|晚上|夜里|深夜)"), "")
            .trim()

        if (desc.isEmpty()) {
            desc = category
        }

        return if (desc.length > 30) desc.substring(0, 30) else desc
    }

    data class ParsedTransaction(
        val amount: Double,
        val category: String,
        val description: String,
        val isExpense: Boolean,
        val date: Long = System.currentTimeMillis(),
        val timeDescription: String = ""  // 时间描述，如"昨天中午"
    )
}
