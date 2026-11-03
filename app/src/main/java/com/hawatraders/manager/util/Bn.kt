package com.hawatraders.manager.util

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bengali digit and price formatting helpers. */
object Bn {
    private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    /** Replaces ASCII digits 0-9 with Bengali digits. */
    fun digits(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) sb.append(if (c in '0'..'9') BN_DIGITS[c - '0'] else c)
        return sb.toString()
    }

    fun number(n: Int): String = digits(n.toString())

    fun number(n: Long): String = digits(n.toString())

    /** Replaces Bengali digits with ASCII digits so user input can be parsed. */
    fun toAscii(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            val idx = BN_DIGITS.indexOf(c)
            sb.append(if (idx >= 0) ('0' + idx) else c)
        }
        return sb.toString()
    }

    /** Formats a price like "৳ ১২,৫০০" (South Asian digit grouping, decimals only if present). */
    fun price(value: Double): String {
        val rounded = Math.round(value * 100) / 100.0
        val whole = rounded.toLong()
        val frac = Math.round((rounded - whole) * 100).toInt()
        val grouped = groupSouthAsian(whole.toString())
        val text = if (frac == 0) grouped else grouped + "." + frac.toString().padStart(2, '0')
        return "৳ " + digits(text)
    }

    /** ASCII text for pre-filling an edit field, e.g. 1250.0 -> "1250". */
    fun editable(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    /** Parses user input (ASCII or Bengali digits, optional commas). Returns null if invalid. */
    fun parsePrice(text: String): Double? {
        val t = toAscii(text).trim().replace(",", "")
        if (t.isEmpty()) return null
        val v = t.toDoubleOrNull() ?: return null
        return if (v.isFinite()) v else null
    }

    /** 1234567 -> "12,34,567". */
    private fun groupSouthAsian(s: String): String {
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        var rest = s.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    /** Whole days since [timestamp] (never negative). */
    fun ageDays(timestamp: Long, now: Long = System.currentTimeMillis()): Long =
        (now - timestamp).coerceAtLeast(0L) / 86_400_000L

    /** "আজ", "গতকাল", "৫ দিন আগে", "৩ মাস আগে", "২ বছর আগে". */
    fun ageText(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val d = ageDays(timestamp, now)
        return when {
            d == 0L -> "আজ"
            d == 1L -> "গতকাল"
            d < 30L -> "${number(d)} দিন আগে"
            d < 365L -> "${number(d / 30L)} মাস আগে"
            else -> "${number(d / 365L)} বছর আগে"
        }
    }

    fun todayStamp(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
