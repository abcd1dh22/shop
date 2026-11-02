package com.hawatraders.manager.data.image

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the image files. Final images live in filesDir/images. A freshly picked photo is first
 * compressed into cacheDir/staged (so a form can be previewed and survives process death) and only
 * moved into filesDir/images when the product is actually saved.
 */
class ImageStore(private val context: Context) {

    val imagesDir: File get() = File(context.filesDir, DIR_IMAGES)
    private val stagedDir: File get() = File(context.cacheDir, "staged")
    private val cameraDir: File get() = File(context.cacheDir, "camera")

    fun fileOf(name: String?): File? {
        if (name.isNullOrBlank() || !SAFE_NAME.matches(name)) return null
        val f = File(imagesDir, name)
        return if (f.isFile) f else null
    }

    /** Decodes + compresses [uri] off the main thread and returns the staged file's absolute path. */
    suspend fun processToStaging(uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = ImageCodec.compress(context.contentResolver, uri)
        stagedDir.mkdirs()
        val f = File(stagedDir, "${UUID.randomUUID()}.webp")
        f.writeBytes(bytes)
        f.absolutePath
    }

    fun stagedFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        return if (isInStaged(f) && f.isFile) f else null
    }

    /** Moves a staged file into the permanent images folder and returns its new file name. */
    fun commitStaged(path: String): String {
        val src = stagedFile(path) ?: throw AppException(Strings.imageMissing)
        imagesDir.mkdirs()
        val name = "${UUID.randomUUID()}.webp"
        val dst = File(imagesDir, name)
        if (!src.renameTo(dst)) {
            src.copyTo(dst, overwrite = false)
            src.delete()
        }
        return name
    }

    fun discardStaged(path: String?) {
        stagedFile(path)?.delete()
    }

    fun delete(name: String?) {
        fileOf(name)?.delete()
    }

    /** A FileProvider URI for the camera app to write a full-size photo to. */
    fun newCameraUri(): Uri {
        cameraDir.mkdirs()
        val f = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
        f.createNewFile()
        return FileProvider.getUriForFile(context, authority(context), f)
    }

    fun clearCameraTemp() {
        cameraDir.deleteRecursively()
    }

    /** Removes leftovers (abandoned staged photos, old backups) older than a day. */
    fun purgeStaleTemp() {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        listOf(stagedDir, cameraDir, File(context.cacheDir, "backup")).forEach { dir ->
            dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
        }
    }

    private fun isInStaged(f: File): Boolean =
        try {
            f.canonicalFile.parentFile == stagedDir.canonicalFile
        } catch (e: Exception) {
            false
        }

    companion object {
        const val DIR_IMAGES = "images"
        val SAFE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")
        fun authority(context: Context) = "${context.packageName}.fileprovider"
    }
}
