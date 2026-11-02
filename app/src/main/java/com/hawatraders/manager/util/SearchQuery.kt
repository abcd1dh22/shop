package com.hawatraders.manager.util

/** Turns raw user input into a SQL LIKE pattern. */
object SearchQuery {
    private val WHITESPACE = Regex("\\s+")

    /** Trims and collapses runs of whitespace. */
    fun normalize(raw: String): String = raw.trim().replace(WHITESPACE, " ")

    /**
     * "  round   up " -> "%round%up%". Every word must appear (in order) in
     * "nameBn nameEn company". LIKE wildcards typed by the user are escaped with '\'.
     */
    fun toLikePattern(raw: String): String {
        val n = normalize(raw)
        if (n.isEmpty()) return "%"
        return n.split(' ').joinToString(separator = "%", prefix = "%", postfix = "%") { escape(it) }
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
