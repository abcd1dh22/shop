package com.hawatraders.manager.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * [imagePath] is the image *file name* inside filesDir/images (not an absolute path),
 * so it stays valid after a backup restore or an app data move.
 *
 * [updatedAt] changes on any edit; [priceUpdatedAt] changes only when the price changes.
 * The three trailing columns were added in database version 2 (see AppDatabase.MIGRATION_1_2),
 * so their SQL defaults must match the migration.
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
    @ColumnInfo(defaultValue = "0") val priceUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "''") val note: String = "",
    /** Added in database version 3; empty when the product has no barcode. */
    @ColumnInfo(defaultValue = "''") val barcode: String = "",
)

class Converters {
    @TypeConverter
    fun categoryToString(c: Category): String = c.key

    @TypeConverter
    fun stringToCategory(s: String): Category = Category.fromKey(s)
}
