package com.hawatraders.manager.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.data.image.ImageStore
import com.hawatraders.manager.data.repository.ProductRepository
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class EditErrors(
    val nameBn: String? = null,
    val unit: String? = null,
    val price: String? = null,
) {
    val hasAny get() = nameBn != null || unit != null || price != null
}

/**
 * Add/edit form. All fields are in the SavedStateHandle, so the form survives rotation and process
 * death. Photos are compressed into a cache "staging" file as soon as they are picked.
 */
class ProductEditViewModel(
    private val handle: SavedStateHandle,
    private val repo: ProductRepository,
    private val images: ImageStore,
) : ViewModel() {

    private val productId: Long = handle.get<Long>("productId") ?: -1L
    val isNew: Boolean = productId <= 0

    var nameBn by handle.saveable { mutableStateOf("") }
    var nameEn by handle.saveable { mutableStateOf("") }
    var company by handle.saveable { mutableStateOf("") }
    var categoryKey by handle.saveable { mutableStateOf(Category.PESTICIDE.key) }
    var unit by handle.saveable { mutableStateOf("") }
    var isFavorite by handle.saveable { mutableStateOf(false) }
    var note by handle.saveable { mutableStateOf("") }
    var barcode by handle.saveable { mutableStateOf("") }
    var priceText by handle.saveable { mutableStateOf("") }
    var existingImage by handle.saveable { mutableStateOf("") }
    var stagedImagePath by handle.saveable { mutableStateOf("") }
    var removeExisting by handle.saveable { mutableStateOf(false) }
    private var loaded by handle.saveable { mutableStateOf(false) }

    var errors by mutableStateOf(EditErrors())
    var loading by mutableStateOf(!isNew && !loaded)
    var imageBusy by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var saved by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    val category: Category get() = Category.fromKey(categoryKey)

    init {
        if (!isNew && !loaded) {
            viewModelScope.launch {
                try {
                    val p = repo.getById(productId)
                    if (p == null) {
                        message = Strings.productNotFound
                    } else {
                        nameBn = p.nameBn
                        nameEn = p.nameEn
                        company = p.company
                        categoryKey = p.category.key
                        unit = p.unit
                        isFavorite = p.isFavorite
                        note = p.note
                        barcode = p.barcode
                        priceText = Bn.editable(p.price)
                        existingImage = p.imagePath ?: ""
                        loaded = true
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = Strings.genericError
                } finally {
                    loading = false
                }
            }
        }
    }

    fun onNameBn(v: String) { nameBn = v; errors = errors.copy(nameBn = null) }
    fun onUnit(v: String) { unit = v; errors = errors.copy(unit = null) }
    fun onPrice(v: String) { priceText = v; errors = errors.copy(price = null) }

    fun onImagePicked(uri: Uri) {
        imageBusy = true
        viewModelScope.launch {
            try {
                val path = images.processToStaging(uri)
                val previous = stagedImagePath
                stagedImagePath = path
                removeExisting = false
                if (previous.isNotBlank() && previous != path) images.discardStaged(previous)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                message = e.userMessage
            } catch (e: Exception) {
                message = Strings.imageFailed
            } finally {
                imageBusy = false
                images.clearCameraTemp()
            }
        }
    }

    fun removeImage() {
        images.discardStaged(stagedImagePath)
        stagedImagePath = ""
        if (existingImage.isNotBlank()) removeExisting = true
    }

    fun save() {
        if (saving || loading) return
        val price = Bn.parsePrice(priceText)
        val e = EditErrors(
            nameBn = if (nameBn.isBlank()) Strings.errNameRequired else null,
            unit = if (unit.isBlank()) Strings.errUnitRequired else null,
            price = if (price == null || price <= 0.0) Strings.errPriceInvalid else null,
        )
        errors = e
        if (e.hasAny || price == null) return

        saving = true
        viewModelScope.launch {
            try {
                val code = barcode.trim()
                if (code.isNotEmpty()) {
                    val other = repo.findByBarcode(code)
                    if (other != null && other.id != productId) {
                        message = Strings.barcodeDuplicate
                        return@launch
                    }
                }
                val entity = ProductEntity(
                    id = if (isNew) 0 else productId,
                    nameBn = nameBn.trim(),
                    nameEn = nameEn.trim(),
                    company = company.trim(),
                    category = category,
                    unit = unit.trim(),
                    price = price,
                    imagePath = existingImage.ifBlank { null },
                    isFavorite = isFavorite,
                    note = note.trim(),
                    barcode = code,
                )
                repo.save(entity, stagedImagePath.ifBlank { null }, removeExisting)
                stagedImagePath = ""
                saved = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                message = e.userMessage
            } catch (e: Exception) {
                message = Strings.saveFailed
            } finally {
                saving = false
            }
        }
    }

    override fun onCleared() {
        if (!saved) images.discardStaged(stagedImagePath)
    }
}
