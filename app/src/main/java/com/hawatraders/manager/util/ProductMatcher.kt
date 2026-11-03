package com.hawatraders.manager.util

import com.hawatraders.manager.data.db.ProductEntity
import java.util.Locale

/**
 * Ranks products against noisy OCR text. Tesseract often misreads a letter or a Bengali vowel sign,
 * so a word also counts when it is within one or two edits of a word in the scanned text.
 */
object ProductMatcher {
    private const val MIN_SCORE = 4.0
    private const val MAX_RESULTS = 6

    fun rank(ocrText: String, products: List<ProductEntity>): List<ProductEntity> {
        val seen = tokens(ocrText).toSet()
        if (seen.isEmpty()) return emptyList()
        return products
            .map { it to score(it, seen) }
            .filter { it.second.matched >= MIN_SCORE }
            .sortedWith(
                compareByDescending<Pair<ProductEntity, Score>> { it.second.matched }
                    .thenByDescending { it.second.matched / it.second.total }
            )
            .take(MAX_RESULTS)
            .map { it.first }
    }

    private class Score(val matched: Double, val total: Double)

    private fun score(p: ProductEntity, seen: Set<String>): Score {
        val words = (tokens(p.nameBn) + tokens(p.nameEn) + tokens(p.company)).toSet()
        var matched = 0.0
        var total = 0.0
        for (w in words) {
            val weight = w.length.toDouble()
            total += weight
            matched += when {
                w in seen -> weight
                w.length >= 4 && seen.any { near(w, it) } -> weight * 0.6
                else -> 0.0
            }
        }
        return Score(matched, if (total == 0.0) 1.0 else total)
    }

    /** Words of two or more characters; Bengali vowel signs count as part of a word. */
    fun tokens(text: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        fun flush() {
            if (sb.length >= 2) out.add(sb.toString())
            sb.setLength(0)
        }
        for (ch in text.lowercase(Locale.ROOT)) {
            val type = Character.getType(ch)
            val part = Character.isLetterOrDigit(ch) ||
                type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt()
            if (part) sb.append(ch) else flush()
        }
        flush()
        return out
    }

    private fun near(a: String, b: String): Boolean {
        val limit = if (a.length >= 8) 2 else 1
        if (kotlin.math.abs(a.length - b.length) > limit) return false
        return editDistance(a, b, limit) <= limit
    }

    /** Levenshtein distance that gives up (returns limit + 1) once it must exceed [limit]. */
    private fun editDistance(a: String, b: String, limit: Int): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (cur[j] < rowMin) rowMin = cur[j]
            }
            if (rowMin > limit) return limit + 1
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }
}
