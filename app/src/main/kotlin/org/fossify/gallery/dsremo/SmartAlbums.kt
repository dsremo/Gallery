package org.fossify.gallery.dsremo

import android.content.Context
import android.provider.MediaStore
import org.fossify.gallery.R
import org.fossify.gallery.helpers.DSREMO_SMART_ALBUM_EARLIER
import org.fossify.gallery.helpers.DSREMO_SMART_ALBUM_THIS_MONTH
import org.fossify.gallery.helpers.DSREMO_SMART_ALBUM_THIS_WEEK
import org.fossify.gallery.helpers.DSREMO_SMART_ALBUM_TODAY
import org.fossify.gallery.helpers.DSREMO_SMART_ALBUM_YESTERDAY
import org.fossify.gallery.helpers.LOCATION_INTERNAL
import org.fossify.gallery.helpers.TYPE_IMAGES
import org.fossify.gallery.models.Directory
import java.util.Calendar

object SmartAlbums {

    data class SmartAlbumRange(val path: String, val name: String, val fromMs: Long, val toMs: Long)

    fun isSmartAlbumPath(path: String): Boolean {
        return path == DSREMO_SMART_ALBUM_TODAY ||
            path == DSREMO_SMART_ALBUM_YESTERDAY ||
            path == DSREMO_SMART_ALBUM_THIS_WEEK ||
            path == DSREMO_SMART_ALBUM_THIS_MONTH ||
            path == DSREMO_SMART_ALBUM_EARLIER
    }

    fun computeRanges(context: Context): List<SmartAlbumRange> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = startOfToday.timeInMillis
        val nowMs = System.currentTimeMillis()

        val yesterdayStart = todayStart - 24L * 60L * 60L * 1000L
        val weekStart = todayStart - 6L * 24L * 60L * 60L * 1000L

        val monthStartCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthStart = monthStartCal.timeInMillis

        return listOf(
            SmartAlbumRange(
                path = DSREMO_SMART_ALBUM_TODAY,
                name = context.getString(R.string.dsremo_smart_album_today),
                fromMs = todayStart,
                toMs = nowMs
            ),
            SmartAlbumRange(
                path = DSREMO_SMART_ALBUM_YESTERDAY,
                name = context.getString(R.string.dsremo_smart_album_yesterday),
                fromMs = yesterdayStart,
                toMs = todayStart
            ),
            SmartAlbumRange(
                path = DSREMO_SMART_ALBUM_THIS_WEEK,
                name = context.getString(R.string.dsremo_smart_album_this_week),
                fromMs = weekStart,
                toMs = System.currentTimeMillis()
            ),
            SmartAlbumRange(
                path = DSREMO_SMART_ALBUM_THIS_MONTH,
                name = context.getString(R.string.dsremo_smart_album_this_month),
                fromMs = monthStart,
                toMs = System.currentTimeMillis()
            ),
            SmartAlbumRange(
                path = DSREMO_SMART_ALBUM_EARLIER,
                name = context.getString(R.string.dsremo_smart_album_earlier),
                fromMs = 0L,
                toMs = monthStart
            )
        )
    }

    fun buildVirtualDirectories(context: Context): ArrayList<Directory> {
        val virtualDirs = ArrayList<Directory>()
        val ranges = computeRanges(context)
        ranges.forEach { range ->
            val (count, thumbnailPath, latestTaken) = queryRange(context, range.fromMs, range.toMs)
            if (count > 0) {
                virtualDirs.add(
                    Directory(
                        id = null,
                        path = range.path,
                        tmb = thumbnailPath,
                        name = range.name,
                        mediaCnt = count,
                        modified = latestTaken,
                        taken = latestTaken,
                        size = 0L,
                        location = LOCATION_INTERNAL,
                        types = TYPE_IMAGES,
                        sortValue = ""
                    )
                )
            }
        }
        return virtualDirs
    }

    private data class RangeStats(val count: Int, val thumbnailPath: String, val latestTaken: Long)

    private fun queryRange(context: Context, fromMs: Long, toMs: Long): RangeStats {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val mediaTypeImage = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val mediaTypeVideo = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val takenCol = MediaStore.MediaColumns.DATE_TAKEN
        val modCol = MediaStore.MediaColumns.DATE_MODIFIED
        val effectiveDate = "COALESCE($takenCol, ${modCol} * 1000)"
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) " +
            "AND $effectiveDate >= ? AND $effectiveDate < ?"
        val args = arrayOf(
            mediaTypeImage.toString(),
            mediaTypeVideo.toString(),
            fromMs.toString(),
            toMs.toString()
        )
        val sortOrder = "$effectiveDate DESC"

        var count = 0
        var thumbnailPath = ""
        var latestTaken = 0L

        try {
            context.contentResolver.query(uri, projection, selection, args, sortOrder)?.use { cursor ->
                val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val takenIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                while (cursor.moveToNext()) {
                    val rowPath = if (dataIdx >= 0) cursor.getString(dataIdx) ?: "" else ""
                    val rowTaken = if (takenIdx >= 0) cursor.getLong(takenIdx) else 0L
                    if (thumbnailPath.isEmpty() && rowPath.isNotEmpty()) {
                        thumbnailPath = rowPath
                        latestTaken = rowTaken
                    }
                    count++
                }
            }
        } catch (ignored: Exception) {
        }

        return RangeStats(count, thumbnailPath, latestTaken)
    }
}
