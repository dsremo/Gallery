package org.fossify.gallery.dsremo

import android.app.Activity
import android.app.ProgressDialog
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.concurrent.thread

object ExifLocationStripper {

    private val LOCATION_TAGS = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_VERSION_ID
    )

    fun launch(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Strip location from all photos?")
            .setMessage(
                "This scans every photo and removes only the GPS / location EXIF tags. " +
                "Date, camera, and exposure metadata are kept.\n\n" +
                "Originals are overwritten in place — there is no undo. Continue?"
            )
            .setPositiveButton("Strip location") { _, _ -> runStrip(activity) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun runStrip(activity: Activity) {
        val progress = ProgressDialog(activity).apply {
            setMessage("Scanning photos…")
            setCancelable(false)
            isIndeterminate = false
            max = 100
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            show()
        }
        thread(name = "dsremo-exif-strip") {
            val paths = collectImagePaths(activity)
            Handler(Looper.getMainLooper()).post { progress.max = paths.size }
            var stripped = 0
            var unchanged = 0
            var failed = 0
            for ((index, path) in paths.withIndex()) {
                when (stripOne(activity, path)) {
                    StripResult.STRIPPED -> stripped++
                    StripResult.NO_LOCATION -> unchanged++
                    StripResult.FAILED -> failed++
                }
                if (index % 10 == 0) {
                    val snapshot = index
                    Handler(Looper.getMainLooper()).post { progress.progress = snapshot }
                }
            }
            Handler(Looper.getMainLooper()).post {
                progress.dismiss()
                AlertDialog.Builder(activity)
                    .setTitle("Done")
                    .setMessage(
                        "Scanned ${paths.size} photos.\n" +
                        "Stripped location from $stripped.\n" +
                        "Already clean: $unchanged.\n" +
                        "Failed: $failed."
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private enum class StripResult { STRIPPED, NO_LOCATION, FAILED }

    private fun stripOne(activity: Activity, path: String): StripResult {
        return try {
            val file = File(path)
            if (!file.isFile || !file.canWrite()) return StripResult.FAILED
            val lower = path.lowercase(java.util.Locale.ROOT)
            if (lower.endsWith(".tif") || lower.endsWith(".tiff")) return StripResult.FAILED
            val exif = ExifInterface(path)
            var hadAny = false
            for (tag in LOCATION_TAGS) {
                val value = exif.getAttribute(tag)
                if (!value.isNullOrEmpty()) {
                    hadAny = true
                    exif.setAttribute(tag, null)
                }
            }
            if (!hadAny) return StripResult.NO_LOCATION
            exif.saveAttributes()
            runCatching {
                android.media.MediaScannerConnection.scanFile(
                    activity.applicationContext,
                    arrayOf(path),
                    null,
                    null
                )
            }
            StripResult.STRIPPED
        } catch (_: Throwable) {
            StripResult.FAILED
        }
    }

    private fun collectImagePaths(activity: Activity): List<String> {
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
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".tif") || lower.endsWith(".tiff")) {
                    out.add(path)
                }
            }
        }
        return out
    }
}
