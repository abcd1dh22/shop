package com.hawatraders.manager.data.backup

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.withTransaction
import com.hawatraders.manager.data.crypto.BackupCrypto
import com.hawatraders.manager.data.db.AppDatabase
import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.data.image.ImageStore
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.Strings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A decrypted, fully validated backup that has not been applied yet. */
class BackupPayload(val products: List<ProductEntity>, val images: Map<String, ByteArray>)

/**
 * Creates and restores encrypted backups. Everything here is blocking IO/CPU work wrapped in
 * Dispatchers.IO, so callers can invoke it from the main thread's coroutine scope.
 *
 * ZIP contents: products.json + images/<file>. The ZIP is encrypted by [BackupCrypto].
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val images: ImageStore,
) {
    private val dao = db.productDao()
    private val backupDir get() = File(context.cacheDir, "backup")

    /** Builds the encrypted backup into cacheDir/backup and returns the file. */
    suspend fun createBackupFile(password: CharArray): File = withContext(Dispatchers.IO) {
        try {
            val products = dao.getAll()
            val bos = ByteArrayOutputStream()
            ZipOutputStream(bos).use { zip ->
                val array = JSONArray()
                val written = HashSet<String>()
                for (p in products) {
                    var imageName: String? = null
                    val f = images.fileOf(p.imagePath)
                    if (f != null) {
                        if (written.add(f.name)) {
                            zip.putNextEntry(ZipEntry("images/${f.name}"))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                        imageName = f.name
                    }
                    array.put(
                        JSONObject().apply {
                            put("id", p.id)
                            put("nameBn", p.nameBn)
                            put("nameEn", p.nameEn)
                            put("company", p.company)
                            put("category", p.category.key)
                            put("unit", p.unit)
                            put("price", p.price)
                            put("image", imageName ?: JSONObject.NULL)
                            put("updatedAt", p.updatedAt)
                            put("priceUpdatedAt", p.priceUpdatedAt)
                            put("isFavorite", p.isFavorite)
                            put("note", p.note)
                        }
                    )
                }
                val root = JSONObject().apply {
                    put("version", FORMAT_VERSION)
                    put("exportedAt", System.currentTimeMillis())
                    put("products", array)
                }
                zip.putNextEntry(ZipEntry(JSON_ENTRY))
                zip.write(root.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val encrypted = BackupCrypto.encrypt(bos.toByteArray(), password)

            backupDir.mkdirs()
            backupDir.listFiles()?.forEach { it.delete() }
            val out = File(backupDir, "hawa-traders-backup-${Bn.todayStamp()}.htb")
            out.writeBytes(encrypted)
            out
        } catch (e: AppException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw AppException(Strings.backupFailed)
        } catch (e: Exception) {
            throw AppException(Strings.backupFailed, e)
        }
    }

    /** Copies a prepared backup file to a SAF destination. */
    suspend fun copyTo(file: File, destination: Uri) = withContext(Dispatchers.IO) {
        try {
            val out = context.contentResolver.openOutputStream(destination, "wt")
                ?: throw IOException("openOutputStream returned null")
            out.use { o -> file.inputStream().use { it.copyTo(o) } }
        } catch (e: Exception) {
            throw AppException(Strings.fileSaveFailed, e)
        }
    }

    fun deleteTempBackups() {
        backupDir.deleteRecursively()
    }

    /** Reads, decrypts and validates a backup. Does not touch existing data. */
    suspend fun readBackup(uri: Uri, password: CharArray): BackupPayload = withContext(Dispatchers.IO) {
        try {
            val raw = readLimited(uri)
            val zipBytes = BackupCrypto.decrypt(raw, password)
            parse(zipBytes)
        } catch (e: AppException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw AppException(Strings.errFileTooLarge)
        } catch (e: Exception) {
            throw AppException(Strings.errBackupCorrupt, e)
        }
    }

    /**
     * Replaces all products and images. Images are staged first, then the database change and the
     * folder swap happen inside a single Room transaction: if anything throws, the transaction
     * rolls back and the previous image folder is put back, leaving existing data unchanged.
     */
    suspend fun restore(payload: BackupPayload) = withContext(Dispatchers.IO) {
        val staging = File(context.filesDir, "images_staging")
        val old = File(context.filesDir, "images_old")
        try {
            staging.deleteRecursively()
            staging.mkdirs()
            for ((name, bytes) in payload.images) File(staging, name).writeBytes(bytes)

            db.withTransaction {
                dao.deleteAll()
                dao.insertAll(payload.products)
                swapImageFolders(staging, old)
            }
            old.deleteRecursively()
        } catch (e: Throwable) {
            staging.deleteRecursively()
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw AppException(Strings.errRestore, e)
        }
    }

    private fun swapImageFolders(staging: File, old: File) {
        val live = File(context.filesDir, ImageStore.DIR_IMAGES)
        old.deleteRecursively()
        val hadLive = live.exists()
        if (hadLive && !live.renameTo(old)) throw IOException("cannot move images aside")
        if (!staging.renameTo(live)) {
            if (hadLive) old.renameTo(live)
            throw IOException("cannot activate restored images")
        }
    }

    private fun readLimited(uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri) ?: throw AppException(Strings.errReadFile)
        input.use {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = it.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_FILE_BYTES) throw AppException(Strings.errFileTooLarge)
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    private fun parse(zipBytes: ByteArray): BackupPayload {
        var json: String? = null
        val imageData = HashMap<String, ByteArray>()
        var total = 0L
        var count = 0
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (++count > MAX_ENTRIES) throw AppException(Strings.errBackupCorrupt)
                val name = entry.name
                if (!entry.isDirectory) {
                    val isJson = name == JSON_ENTRY
                    val isImage = name.startsWith("images/") && ImageStore.SAFE_NAME.matches(name.removePrefix("images/"))
                    if (isJson || isImage) {
                        val data = readEntry(zis, if (isJson) MAX_JSON_BYTES else MAX_IMAGE_BYTES)
                        total += data.size
                        if (total > MAX_TOTAL_BYTES) throw AppException(Strings.errFileTooLarge)
                        if (isJson) json = String(data, Charsets.UTF_8)
                        else imageData[name.removePrefix("images/")] = data
                    }
                    // Any other entry (including path-traversal names) is ignored, never extracted.
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        val text = json ?: throw AppException(Strings.errBackupCorrupt)
        return validate(text, imageData)
    }

    private fun readEntry(zis: ZipInputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = zis.read(buf)
            if (n < 0) break
            total += n
            if (total > limit) throw AppException(Strings.errFileTooLarge)
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun validate(text: String, imageData: Map<String, ByteArray>): BackupPayload {
        try {
            val root = JSONObject(text)
            if (root.optInt("version", -1) != FORMAT_VERSION) throw AppException(Strings.errUnsupportedVersion)
            val arr = root.getJSONArray("products")
            if (arr.length() > MAX_PRODUCTS) throw AppException(Strings.errBackupCorrupt)

            val products = ArrayList<ProductEntity>(arr.length())
            val usedImages = HashMap<String, ByteArray>()
            val now = System.currentTimeMillis()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val nameBn = o.optString("nameBn", "").trim()
                val unit = o.optString("unit", "").trim()
                val price = o.optDouble("price", Double.NaN)
                val category = Category.fromKeyOrNull(o.optString("category", ""))
                if (nameBn.isEmpty() || nameBn.length > 300 || unit.isEmpty() || unit.length > 100 ||
                    !price.isFinite() || price < 0 || category == null
                ) throw AppException(Strings.errBackupCorrupt)

                var image: String? = if (o.isNull("image")) null else o.optString("image", "")
                if (image != null) {
                    val bytes = imageData[image]
                    val ok = bytes != null && isDecodable(bytes)
                    if (ok) usedImages[image] = bytes!! else image = null
                }
                products.add(
                    ProductEntity(
                        id = 0, // fresh auto-generated ids on restore
                        nameBn = nameBn,
                        nameEn = o.optString("nameEn", "").take(300),
                        company = o.optString("company", "").take(300),
                        category = category,
                        unit = unit,
                        price = price,
                        imagePath = image,
                        updatedAt = o.optLong("updatedAt", now),
                        priceUpdatedAt = o.optLong("priceUpdatedAt", o.optLong("updatedAt", now)),
                        isFavorite = o.optBoolean("isFavorite", false),
                        note = o.optString("note", "").take(2000),
                    )
                )
            }
            return BackupPayload(products, usedImages)
        } catch (e: JSONException) {
            throw AppException(Strings.errBackupCorrupt, e)
        }
    }

    private fun isDecodable(bytes: ByteArray): Boolean {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        return o.outWidth > 0 && o.outHeight > 0
    }

    private companion object {
        const val FORMAT_VERSION = 1
        const val JSON_ENTRY = "products.json"
        const val MAX_FILE_BYTES = 200L * 1024 * 1024
        const val MAX_TOTAL_BYTES = 300L * 1024 * 1024
        const val MAX_JSON_BYTES = 30 * 1024 * 1024
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        const val MAX_ENTRIES = 50_000
        const val MAX_PRODUCTS = 50_000
    }
}
