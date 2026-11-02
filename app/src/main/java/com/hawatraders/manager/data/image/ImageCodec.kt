package com.hawatraders.manager.data.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Decodes a picked/captured photo, fixes EXIF rotation, shrinks it to <= 400px and encodes WebP. */
object ImageCodec {
    private const val MAX_SIDE = 400
    private const val TARGET_BYTES = 40 * 1024
    private val QUALITIES = intArrayOf(80, 70, 60, 50, 40)

    fun compress(resolver: ContentResolver, uri: Uri): ByteArray {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(resolver, uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw AppException(Strings.imageFailed)

            // Power-of-two subsampling while the result is still at least MAX_SIDE on both sides.
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_SIDE && bounds.outHeight / (sample * 2) >= MAX_SIDE) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = open(resolver, uri).use { BitmapFactory.decodeStream(it, null, opts) }
                ?: throw AppException(Strings.imageFailed)

            val orientation = try {
                open(resolver, uri).use {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f); matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(-90f); matrix.postScale(-1f, 1f)
                }
            }
            val longest = maxOf(decoded.width, decoded.height)
            if (longest > MAX_SIDE) {
                val s = MAX_SIDE.toFloat() / longest
                matrix.postScale(s, s)
            }
            val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (transformed !== decoded) decoded.recycle()

            // Flatten onto white so transparent PNGs do not turn black in lossy WebP.
            val flat = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(Color.WHITE)
                drawBitmap(transformed, 0f, 0f, null)
            }
            transformed.recycle()

            var result = ByteArray(0)
            for (q in QUALITIES) {
                val bos = ByteArrayOutputStream()
                if (!flat.compress(webpFormat(), q, bos)) throw AppException(Strings.imageFailed)
                result = bos.toByteArray()
                if (result.size <= TARGET_BYTES) break
            }
            flat.recycle()
            return result
        } catch (e: AppException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw AppException(Strings.imageFailed)
        } catch (e: IOException) {
            throw AppException(Strings.imageFailed, e)
        } catch (e: SecurityException) {
            throw AppException(Strings.imageFailed, e)
        }
    }

    private fun open(resolver: ContentResolver, uri: Uri): InputStream =
        resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else Bitmap.CompressFormat.WEBP
}
