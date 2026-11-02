package com.hawatraders.manager.data.repository

import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.data.db.ProductDao
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.data.image.ImageStore
import com.hawatraders.manager.util.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProductRepository(
    private val dao: ProductDao,
    private val images: ImageStore,
) {
    fun observeAll(): Flow<List<ProductEntity>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    fun observeSearch(query: String, category: Category?): Flow<List<ProductEntity>> =
        dao.search(SearchQuery.toLikePattern(query), category?.key)

    suspend fun getById(id: Long): ProductEntity? = dao.getById(id)

    /**
     * Inserts (id == 0) or updates a product. [stagedImagePath] is a freshly compressed photo to
     * attach; [removeExistingImage] drops the current one. Old image files are deleted only after
     * the database write succeeds; a new file is removed again if the write fails.
     */
    suspend fun save(
        product: ProductEntity,
        stagedImagePath: String?,
        removeExistingImage: Boolean,
    ): Long = withContext(Dispatchers.IO) {
        val oldName = product.imagePath
        var newName: String? = null
        try {
            if (!stagedImagePath.isNullOrBlank()) newName = images.commitStaged(stagedImagePath)
            val finalImage = when {
                newName != null -> newName
                removeExistingImage -> null
                else -> oldName
            }
            val toSave = product.copy(imagePath = finalImage, updatedAt = System.currentTimeMillis())
            val id = if (product.id == 0L) {
                dao.insert(toSave)
            } else {
                dao.update(toSave)
                product.id
            }
            if (oldName != null && oldName != finalImage) images.delete(oldName)
            id
        } catch (t: Throwable) {
            newName?.let { images.delete(it) }
            throw t
        }
    }

    suspend fun updatePrice(id: Long, price: Double) =
        dao.updatePrice(id, price, System.currentTimeMillis())

    suspend fun delete(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.delete(product)
        images.delete(product.imagePath)
    }
}
