package com.hawatraders.manager.data.db

/**
 * Product category. [key] is what gets stored in the database and in backups, so it must never change
 * (and stays correct even if R8 renames enum constants).
 */
enum class Category(val key: String, val labelBn: String) {
    PESTICIDE("PESTICIDE", "কীটনাশক"),
    FUNGICIDE("FUNGICIDE", "ছত্রাকনাশক"),
    HERBICIDE("HERBICIDE", "আগাছানাশক"),
    FERTILIZER("FERTILIZER", "সার"),
    SEED("SEED", "বীজ"),
    OTHER("OTHER", "অন্যান্য");

    companion object {
        fun fromKeyOrNull(key: String?): Category? = entries.firstOrNull { it.key == key }
        fun fromKey(key: String?): Category = fromKeyOrNull(key) ?: OTHER
    }
}
