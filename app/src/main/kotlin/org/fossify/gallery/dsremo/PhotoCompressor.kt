package org.fossify.gallery.dsremo

import android.app.Activity
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

object PhotoCompressor {

    private data class CompressResult(
        val scanned: Int,
        val compressed: Int,
        val skipped: Int,
        val failed: Int,
        val bytesBefore: Long,
        val bytesAfter: Long
    )

    fun launch(activity: Activity) {
        val qualities = arrayOf("50%", "75%", "90%")
        val qualityValues = intArrayOf(50, 75, 90)
        AlertDialog.Builder(activity)
            .setTitle("Compress photos")
            .setItems(qualities) { _, which ->
                confirmAndRun(activity, qualityValues[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmAndRun(activity: Activity, quality: Int) {
        AlertDialog.Builder(activity)
            .setTitle("Compress all JPEGs at $quality%?")
            .setMessage(
                "This re-encodes every JPEG in your library at $quality% JPEG quality. " +
                "Files that would not shrink are skipped.\n\n" +
                "Originals are overwritten in place — there is no undo. Continue?"
            )
            .setPositiveButton("Compress") { _, _ -> runCompress(activity, quality) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun runCompress(activity: Activity, quality: Int) {
        val progress = ProgressDialog(activity).apply {
            setMessage("Scanning photos…")
            setCancelable(false)
            isIndeterminate = false
            max = 100
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            show()
        }
        thread(name = "dsremo-photo-compress") {
            val paths = collectJpegPaths(activity)
            Handler(Looper.getMainLooper()).post { progress.max = paths.size }
            var compressed = 0
            var skipped = 0
            var failed = 0
            var bytesBefore = 0L
            var bytesAfter = 0L
            for ((index, path) in paths.withIndex()) {
                val outcome = compressOne(path, quality)
                when (outcome.status) {
                    Status.COMPRESSED -> {
                        compressed++
                        bytesBefore += outcome.before
                        bytesAfter += outcome.after
                    }
                    Status.SKIPPED -> skipped++
                    Status.FAILED -> failed++
                }
                if (index % 5 == 0) {
                    val snapshot = index
                    Handler(Looper.getMainLooper()).post { progress.progress = snapshot }
                }
            }
            val result = CompressResult(paths.size, compressed, skipped, failed, bytesBefore, bytesAfter)
            Handler(Looper.getMainLooper()).post {
                progress.dismiss()
                showSummary(activity, result)
            }
        }
    }

    private enum class Status { COMPRESSED, SKIPPED, FAILED }

    private data class OneResult(val status: Status, val before: Long, val after: Long)

    private fun compressOne(path: String, quality: Int): OneResult {
        return try {
            val file = File(path)
            if (!file.isFile || !file.canWrite()) return OneResult(Status.FAILED, 0L, 0L)
            val originalSize = file.length()
            if (originalSize <= 0L) return OneResult(Status.FAILED, 0L, 0L)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return OneResult(Status.FAILED, 0L, 0L)

            val maxDimension = 4096
            var sample = 1
            var effectiveMax = maxOf(bounds.outWidth, bounds.outHeight)
            while (effectiveMax / sample > maxDimension) sample *= 2

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = BitmapFactory.decodeFile(path, decodeOpts) ?: return OneResult(Status.FAILED, 0L, 0L)

            val originalOrientation = runCatching {
                ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

            val bitmap = rotateBitmapForExif(raw, originalOrientation)
            if (bitmap !== raw) raw.recycle()

            val buffer = ByteArrayOutputStream()
            val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, buffer)
            bitmap.recycle()
            if (!ok) return OneResult(Status.FAILED, 0L, 0L)

            val newBytes = buffer.toByteArray()
            if (newBytes.size.toLong() >= originalSize) {
                return OneResult(Status.SKIPPED, originalSize, originalSize)
            }

            val preservedExif = runCatching { snapshotExif(path) }.getOrNull()

            val tempFile = File(file.parentFile, ".${file.name}.dsremo-tmp")
            try {
                FileOutputStream(tempFile, false).use { stream ->
                    stream.write(newBytes)
                    stream.flush()
                    stream.fd.sync()
                }
                if (!tempFile.renameTo(file)) {
                    tempFile.delete()
                    return OneResult(Status.FAILED, 0L, 0L)
                }
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }

            if (preservedExif != null) {
                runCatching { restoreExif(path, preservedExif) }
            }

            OneResult(Status.COMPRESSED, originalSize, file.length())
        } catch (_: Throwable) {
            OneResult(Status.FAILED, 0L, 0L)
        }
    }

    private fun rotateBitmapForExif(source: Bitmap, orientation: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postScale(-1f, 1f); matrix.postRotate(-90f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postScale(-1f, 1f); matrix.postRotate(90f) }
            else -> return source
        }
        return runCatching {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }.getOrDefault(source)
    }

    private fun snapshotExif(path: String): Map<String, String> {
        val exif = ExifInterface(path)
        val tags = listOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_ISO_SPEED_RATINGS,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP
        )
        val map = HashMap<String, String>()
        for (tag in tags) {
            val value = exif.getAttribute(tag)
            if (!value.isNullOrEmpty()) map[tag] = value
        }
        return map
    }

    private fun restoreExif(path: String, snapshot: Map<String, String>) {
        if (snapshot.isEmpty()) return
        val exif = ExifInterface(path)
        for ((tag, value) in snapshot) {
            exif.setAttribute(tag, value)
        }
        exif.saveAttributes()
    }

    private fun collectJpegPaths(activity: Activity): List<String> {
        val out = ArrayList<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = runCatching {
            activity.contentResolver.query(uri, projection, null, null, null)
        }.getOrNull() ?: return out
        cursor.use {
            val col = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val path = runCatching { it.getString(col) }.getOrNull() ?: continue
                val lower = path.lowercase(java.util.Locale.ROOT)
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                    if (File(path).isFile) out.add(path)
                }
            }
        }
        return out
    }

    private fun showSummary(activity: Activity, result: CompressResult) {
        val savedBytes = (result.bytesBefore - result.bytesAfter).coerceAtLeast(0L)
        val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes / 1_048_576.0)
        val beforeMb = String.format(java.util.Locale.US, "%.2f", result.bytesBefore / 1_048_576.0)
        val afterMb = String.format(java.util.Locale.US, "%.2f", result.bytesAfter / 1_048_576.0)
        val message = "Scanned ${result.scanned} JPEGs.\n" +
                "Compressed: ${result.compressed}\n" +
                "Skipped (no gain): ${result.skipped}\n" +
                "Failed: ${result.failed}\n\n" +
                "Before: $beforeMb MB\n" +
                "After: $afterMb MB\n" +
                "Saved: $savedMb MB"
        AlertDialog.Builder(activity)
            .setTitle("Done")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
