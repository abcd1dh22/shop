package com.hawatraders.manager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * [imagePath] is the image *file name* inside filesDir/images (not an absolute path),
 * so it stays valid after a backup restore or an app data move.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameBn: String,
    val nameEn: String = "",
    val company: String = "",
    val category: Category = Category.OTHER,
    val unit: String,
    val price: Double,
    val imagePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

class Converters {
    @TypeConverter
    fun categoryToString(c: Category): String = c.key

    @TypeConverter
    fun stringToCategory(s: String): Category = Category.fromKey(s)
}
