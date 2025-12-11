package com.ai.bookkeeping.util

import com.ai.bookkeeping.model.ExpenseCategories
import com.ai.bookkeeping.model.IncomeCategories
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import java.util.regex.Pattern

/**
 * AI记账解析器
 * 使用本地规则引擎解析用户输入的自然语言
 * 支持多种格式如："早餐15"、"午饭花了30块"、"收到工资5000"等
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
        "提成", "分红", "利息", "红包", "转账收", "退款", "兼职"
    )

    // 分类关键词映射（支出）
    private val expenseCategoryKeywords = mapOf(
        "餐饮" to listOf("早餐", "午餐", "晚餐", "早饭", "午饭", "晚饭", "吃饭", "外卖",
                        "奶茶", "咖啡", "饮料", "零食", "水果", "买菜", "超市", "食品",
                        "餐厅", "火锅", "烧烤", "小吃", "面包", "蛋糕", "点心"),
        "交通" to listOf("打车", "出租", "滴滴", "公交", "地铁", "高铁", "火车", "飞机",
                        "机票", "车票", "加油", "油费", "停车", "过路费", "共享单车"),
        "购物" to listOf("淘宝", "京东", "拼多多", "网购", "购物", "买"),
        "娱乐" to listOf("电影", "游戏", "KTV", "唱歌", "旅游", "门票", "演出", "酒吧"),
        "医疗" to listOf("医院", "药", "看病", "体检", "挂号", "医药"),
        "教育" to listOf("课程", "培训", "学费", "书", "教材", "考试"),
        "居住" to listOf("房租", "租金", "物业", "水电", "电费", "水费", "燃气", "网费", "宽带"),
        "通讯" to listOf("话费", "手机费", "流量", "充值"),
        "服饰" to listOf("衣服", "裤子", "鞋", "帽子", "包", "配饰", "化妆品", "护肤")
    )

    // 分类关键词映射（收入）
    private val incomeCategoryKeywords = mapOf(
        "工资" to listOf("工资", "薪水", "薪资", "月薪", "底薪"),
        "奖金" to listOf("奖金", "年终奖", "绩效", "提成", "分红"),
        "投资" to listOf("利息", "股票", "基金", "理财", "分红"),
        "兼职" to listOf("兼职", "副业", "外快", "私活"),
        "红包" to listOf("红包", "转账", "礼金")
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

        // 4. 生成描述
        val description = generateDescription(cleanInput, category)

        return Transaction(
            amount = amount,
            type = type,
            category = category,
            description = description,
            aiParsed = true
        )
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

        for ((category, keywords) in categoryKeywords) {
            for (keyword in keywords) {
                if (input.contains(keyword)) {
                    return category
                }
            }
        }

        return defaultCategories.last() // 返回"其他"
    }

    /**
     * 生成描述
     */
    private fun generateDescription(input: String, category: String): String {
        // 移除金额相关字符，保留描述性文字
        var desc = input
            .replace(Regex("\\d+\\.?\\d*\\s*(元|块|￥|\$)?"), "")
            .replace(Regex("(花了|花费|消费|支出|收入|收到|赚了|得到)"), "")
            .trim()

        if (desc.isEmpty()) {
            desc = category
        }

        return if (desc.length > 20) desc.substring(0, 20) else desc
    }
}
