package com.hawatraders.manager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY isFavorite DESC, nameBn COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    /**
     * [pattern] comes from SearchQuery.toLikePattern (already trimmed, escaped with '\').
     * SQLite LIKE is case-insensitive for Latin text; Bengali has no case.
     * A null [category] means "all categories". Favorites are listed first.
     */
    @Query(
        "SELECT * FROM products " +
            "WHERE (:category IS NULL OR category = :category) " +
            "AND (:favoritesOnly = 0 OR isFavorite = 1) " +
            "AND (nameBn || ' ' || nameEn || ' ' || company) LIKE :pattern ESCAPE '\\' " +
            "ORDER BY isFavorite DESC, nameBn COLLATE NOCASE ASC"
    )
    fun search(pattern: String, category: String?, favoritesOnly: Boolean): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY nameBn COLLATE NOCASE ASC")
    fun observeByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    /** Room runs a list update inside a single transaction. */
    @Update
    suspend fun updateAll(products: List<ProductEntity>)

    @Query("UPDATE products SET price = :price, updatedAt = :now, priceUpdatedAt = :now WHERE id = :id")
    suspend fun updatePrice(id: Long, price: Double, now: Long)

    @Query("UPDATE products SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}
