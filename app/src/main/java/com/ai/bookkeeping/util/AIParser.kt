package com.ai.bookkeeping.util

import com.ai.bookkeeping.model.ExpenseCategories
import com.ai.bookkeeping.model.IncomeCategories
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import java.util.Calendar
import java.util.regex.Pattern

/**
 * AI记账解析器
 * 使用本地规则引擎解析用户输入的自然语言
 * 支持多种格式如："早餐15"、"昨天中午吃饭30块"、"收到工资5000"等
 *
 * 增强功能：
 * - 相对日期解析（昨天、前天、今天、上周等）
 * - 时间段识别（早上、中午、下午、晚上等）
 * - 智能分类识别
 */
object AIParser {

    // 金额匹配正则
    private val amountPatterns = listOf(
        Pattern.compile("(\\d+\\.?\\d*)\\s*(元|块|￥|\$)?"),
        Pattern.compile("(花了|花费|消费|支出|收入|收到|赚了|得到)\\s*(\\d+\\.?\\d*)"),
        Pattern.compile("(\\d+\\.?\\d*)\\s*(花了|花费|消费|支出|收入|收到|赚了|得到)")
    )

    // 收入关键词
    private val incomeKeywords = listOf(
        "工资", "薪水", "薪资", "收入", "收到", "赚", "得到", "奖金",
        "提成", "分红", "利息", "红包", "转账收", "退款", "兼职", "转入"
    )

    // 相对日期关键词映射（天数偏移）
    private val relativeDateKeywords = mapOf(
        "今天" to 0,
        "今日" to 0,
        "昨天" to -1,
        "昨日" to -1,
        "前天" to -2,
        "大前天" to -3,
        "前几天" to -3,
        "上周" to -7,
        "上星期" to -7
    )

    // 时间段关键词映射（小时, 分钟）
    private val timeOfDayKeywords = mapOf(
        "凌晨" to Pair(3, 0),      // 3:00
        "早上" to Pair(7, 30),     // 7:30
        "早晨" to Pair(7, 30),     // 7:30
        "上午" to Pair(10, 0),     // 10:00
        "中午" to Pair(12, 0),     // 12:00
        "午饭" to Pair(12, 0),     // 12:00 (午饭时间)
        "午餐" to Pair(12, 0),     // 12:00
        "下午" to Pair(15, 0),     // 15:00
        "傍晚" to Pair(18, 0),     // 18:00
        "晚上" to Pair(19, 30),    // 19:30
        "晚饭" to Pair(18, 30),    // 18:30 (晚饭时间)
        "晚餐" to Pair(18, 30),    // 18:30
        "夜里" to Pair(22, 0),     // 22:00
        "深夜" to Pair(23, 30),    // 23:30
        "早餐" to Pair(7, 30),     // 7:30 (早餐时间)
        "早饭" to Pair(7, 30),     // 7:30
        "宵夜" to Pair(22, 30),    // 22:30
        "夜宵" to Pair(22, 30)     // 22:30
    )

    // 分类关键词映射（支出）
    private val expenseCategoryKeywords = mapOf(
        "餐饮" to listOf("早餐", "午餐", "晚餐", "早饭", "午饭", "晚饭", "吃饭", "外卖",
                        "奶茶", "咖啡", "饮料", "零食", "水果", "买菜", "超市", "食品",
                        "餐厅", "火锅", "烧烤", "小吃", "面包", "蛋糕", "点心", "饭", "菜",
                        "宵夜", "夜宵", "聚餐", "食堂", "盒饭", "快餐", "汉堡", "炸鸡"),
        "交通" to listOf("打车", "出租", "滴滴", "公交", "地铁", "高铁", "火车", "飞机",
                        "机票", "车票", "加油", "油费", "停车", "过路费", "共享单车", "骑车",
                        "出行", "uber", "曹操", "首汽", "神州", "嘀嗒"),
        "购物" to listOf("淘宝", "京东", "拼多多", "网购", "购物", "买东西", "天猫", "唯品会"),
        "娱乐" to listOf("电影", "游戏", "KTV", "唱歌", "旅游", "门票", "演出", "酒吧",
                        "健身", "运动", "球", "游泳", "瑜伽", "景点"),
        "医疗" to listOf("医院", "药", "看病", "体检", "挂号", "医药", "诊所", "牙科", "眼科"),
        "教育" to listOf("课程", "培训", "学费", "书", "教材", "考试", "补习", "网课", "学习"),
        "居住" to listOf("房租", "租金", "物业", "水电", "电费", "水费", "燃气", "网费", "宽带",
                        "暖气", "空调", "维修", "装修"),
        "通讯" to listOf("话费", "手机费", "流量", "充值", "套餐"),
        "服饰" to listOf("衣服", "裤子", "鞋", "帽子", "包", "配饰", "化妆品", "护肤", "洗护",
                        "内衣", "袜子", "外套", "T恤")
    )

    // 分类关键词映射（收入）
    private val incomeCategoryKeywords = mapOf(
        "工资" to listOf("工资", "薪水", "薪资", "月薪", "底薪", "发工资"),
        "奖金" to listOf("奖金", "年终奖", "绩效", "提成", "分红", "奖励"),
        "投资" to listOf("利息", "股票", "基金", "理财", "分红", "收益", "回报"),
        "兼职" to listOf("兼职", "副业", "外快", "私活", "零工"),
        "红包" to listOf("红包", "转账", "礼金", "随份子", "压岁钱")
    )

    /**
     * 解析用户输入，返回Transaction对象
     * 这是一个本地解析方法，不需要网络请求
     */
    suspend fun parse(input: String): Transaction? {
        val cleanInput = input.trim()
        if (cleanInput.isEmpty()) return null

        // 1. 判断是收入还是支出
        val type = determineType(cleanInput)

        // 2. 提取金额
        val amount = extractAmount(cleanInput) ?: return null

        // 3. 确定分类
        val category = determineCategory(cleanInput, type)

        // 4. 解析日期和时间
        val dateTime = parseDateTime(cleanInput)

        // 5. 生成描述
        val description = generateDescription(cleanInput, category)

        return Transaction(
            amount = amount,
            type = type,
            category = category,
            description = description,
            date = dateTime,
            aiParsed = true
        )
    }

    /**
     * 解析日期和时间
     * 支持：昨天、前天、今天、早上、中午、下午、晚上等
     */
    fun parseDateTime(input: String): Long {
        val calendar = Calendar.getInstance()

        // 1. 解析相对日期
        var dateOffset = 0
        for ((keyword, offset) in relativeDateKeywords) {
            if (input.contains(keyword)) {
                dateOffset = offset
                break
            }
        }

        // 应用日期偏移
        if (dateOffset != 0) {
            calendar.add(Calendar.DAY_OF_YEAR, dateOffset)
        }

        // 2. 解析时间段
        var foundTime = false
        for ((keyword, timePair) in timeOfDayKeywords) {
            if (input.contains(keyword)) {
                calendar.set(Calendar.HOUR_OF_DAY, timePair.first)
                calendar.set(Calendar.MINUTE, timePair.second)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                foundTime = true
                break
            }
        }

        // 3. 尝试解析具体时间（如 "14点30"、"下午3点"）
        if (!foundTime) {
            val timePattern = Pattern.compile("(\\d{1,2})[:点时](\\d{0,2})分?")
            val matcher = timePattern.matcher(input)
            if (matcher.find()) {
                val hour = matcher.group(1)?.toIntOrNull() ?: 0
                val minute = matcher.group(2)?.toIntOrNull() ?: 0

                // 处理 "下午3点" 这种情况
                val adjustedHour = if (input.contains("下午") || input.contains("晚上")) {
                    if (hour < 12) hour + 12 else hour
                } else {
                    hour
                }

                calendar.set(Calendar.HOUR_OF_DAY, adjustedHour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * 判断交易类型（收入/支出）
     */
    private fun determineType(input: String): TransactionType {
        for (keyword in incomeKeywords) {
            if (input.contains(keyword)) {
                return TransactionType.INCOME
            }
        }
        return TransactionType.EXPENSE
    }

    /**
     * 提取金额
     */
    private fun extractAmount(input: String): Double? {
        // 先尝试匹配带关键词的金额
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(input)
            while (matcher.find()) {
                for (i in 1..matcher.groupCount()) {
                    val group = matcher.group(i)
                    if (group != null) {
                        val amount = group.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            return amount
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * 确定分类
     */
    private fun determineCategory(input: String, type: TransactionType): String {
        val categoryKeywords = if (type == TransactionType.EXPENSE) {
            expenseCategoryKeywords
        } else {
            incomeCategoryKeywords
        }

        val defaultCategories = if (type == TransactionType.EXPENSE) {
            ExpenseCategories.list
        } else {
            IncomeCategories.list
        }

        // 计算每个分类的匹配分数
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

        // 返回得分最高的分类
        return scores.maxByOrNull { it.value }?.key ?: defaultCategories.last()
    }

    /**
     * 生成描述
     */
    private fun generateDescription(input: String, category: String): String {
        // 移除金额相关字符和日期时间词，保留描述性文字
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

    /**
     * 获取时间段的友好名称
     */
    fun getTimeOfDayName(hour: Int): String {
        return when (hour) {
            in 0..5 -> "凌晨"
            in 6..8 -> "早上"
            in 9..11 -> "上午"
            12 -> "中午"
            in 13..17 -> "下午"
            in 18..19 -> "傍晚"
            in 20..22 -> "晚上"
            else -> "深夜"
        }
    }
}
