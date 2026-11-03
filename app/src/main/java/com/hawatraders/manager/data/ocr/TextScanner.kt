package com.hawatraders.manager.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.googlecode.tesseract.android.TessBaseAPI
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Offline OCR for product labels, built on Tesseract. The language files (ben / eng .traineddata)
 * are bundled in assets/tessdata and copied once to filesDir/ocr/tessdata, which is the layout
 * Tesseract expects. Nothing leaves the device.
 */
class TextScanner(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "ocr")
    private val tessDir = File(root, "tessdata")
    private val mutex = Mutex()

    /** Reads the photo at [uri] and returns the text found in it (may be empty). */
    suspend fun recognize(uri: Uri): String = withContext(Dispatchers.Default) {
        val langs = prepareLanguages()
        val bitmap = decode(uri) ?: throw AppException(Strings.ocrImageFailed)
        try {
            mutex.withLock {
                val api = TessBaseAPI()
                try {
                    if (!api.init(root.absolutePath, langs)) throw AppException(Strings.ocrEngineFailed)
                    api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                    api.setImage(bitmap)
                    api.utf8Text.orEmpty()
                } finally {
                    api.recycle()
                }
            }
        } catch (e: AppException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw AppException(Strings.ocrEngineFailed)
        } finally {
            bitmap.recycle()
        }
    }

    /** Copies bundled language files out of assets; returns "ben+eng" style language string. */
    private fun prepareLanguages(): String {
        tessDir.mkdirs()
        val present = LANGS.filter { lang ->
            val target = File(tessDir, "$lang.traineddata")
            if (!target.exists() || target.length() == 0L) {
                try {
                    appContext.assets.open("tessdata/$lang.traineddata").use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    }
                } catch (e: Exception) {
                    target.delete()
                }
            }
            target.exists() && target.length() > 0L
        }
        // Bengali is the whole point of the feature; English alone would silently misread labels.
        if ("ben" !in present) throw AppException(Strings.ocrDataMissing)
        return present.joinToString("+")
    }

    /** Decodes at most ~2000 px on the long side and applies the camera's EXIF rotation. */
    private fun decode(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val scale = MAX_SIDE.toFloat() / max(raw.width, raw.height)
        if (scale < 1f) matrix.postScale(scale, scale)
        if (matrix.isIdentity) return raw
        val out = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (out !== raw) raw.recycle()
        return out
    }

    private companion object {
        val LANGS = listOf("ben", "eng")
        const val MAX_SIDE = 2000
    }
}
