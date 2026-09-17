package org.fossify.gallery.dsremo

import android.app.Activity
import android.app.ProgressDialog
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.concurrent.thread

object DuplicateFinder {

    private data class FileEntry(val path: String, val size: Long, val modified: Long)

    fun launch(activity: Activity) {
        val progress = ProgressDialog(activity).apply {
            setMessage("Scanning photos…")
            setCancelable(false)
            isIndeterminate = false
            max = 100
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            show()
        }
        thread(name = "dsremo-dup-finder") {
            val results = runCatching { scan(activity, progress) }.getOrElse { emptyMap() }
            Handler(Looper.getMainLooper()).post {
                progress.dismiss()
                showResults(activity, results)
            }
        }
    }

    private fun scan(activity: Activity, progress: ProgressDialog): Map<String, List<FileEntry>> {
        val entries = collectImages(activity)
        if (entries.isEmpty()) return emptyMap()
        Handler(Looper.getMainLooper()).post { progress.max = entries.size }

        val bySize = entries.groupBy { it.size }.filterValues { it.size > 1 }
        if (bySize.isEmpty()) return emptyMap()

        val byHash = HashMap<String, MutableList<FileEntry>>()
        var done = 0
        for ((_, candidates) in bySize) {
            for (entry in candidates) {
                runCatching {
                    val hash = sha256OfFile(entry.path)
                    byHash.getOrPut(hash) { mutableListOf() }.add(entry)
                }
                done++
                if (done % 25 == 0) {
                    val snapshot = done
                    Handler(Looper.getMainLooper()).post { progress.progress = snapshot }
                }
            }
        }
        return byHash.filterValues { it.size > 1 }
    }

    private fun collectImages(activity: Activity): List<FileEntry> {
        val out = ArrayList<FileEntry>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val cursor = runCatching {
            activity.contentResolver.query(uri, projection, null, null, null)
        }.getOrNull() ?: return out
        cursor.use {
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val modCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            while (it.moveToNext()) {
                val path = runCatching { it.getString(dataCol) }.getOrNull() ?: continue
                val size = runCatching { it.getLong(sizeCol) }.getOrDefault(0L)
                val modified = runCatching { it.getLong(modCol) }.getOrDefault(0L)
                if (size <= 0) continue
                if (!File(path).isFile) continue
                out.add(FileEntry(path, size, modified))
            }
        }
        return out
    }

    private fun sha256OfFile(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(path).use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun showResults(activity: Activity, groups: Map<String, List<FileEntry>>) {
        if (groups.isEmpty()) {
            AlertDialog.Builder(activity)
                .setTitle("No duplicates found")
                .setMessage("Every photo has a unique SHA-256 hash. Nothing to delete.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val totalExtras = groups.values.sumOf { it.size - 1 }
        val totalWaste = groups.values.sumOf { (it.size - 1) * it.first().size }
        val mb = String.format(java.util.Locale.US, "%.1f", totalWaste / 1_048_576.0)
        val message = "Found ${groups.size} duplicate groups.\n" +
                "$totalExtras extra copies use $mb MB.\n\n" +
                "Tapping Delete extras will keep the oldest copy of each group and delete the rest."
        AlertDialog.Builder(activity)
            .setTitle("Duplicate photos")
            .setMessage(message)
            .setPositiveButton("Delete extras") { _, _ -> deleteExtras(activity, groups) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteExtras(activity: Activity, groups: Map<String, List<FileEntry>>) {
        val progress = ProgressDialog(activity).apply {
            setMessage("Deleting duplicates…")
            setCancelable(false)
            show()
        }
        thread(name = "dsremo-dup-delete") {
            var deleted = 0
            var failed = 0
            var scopedStorageBlocked = 0
            for ((_, group) in groups) {
                val sorted = group.sortedBy { it.modified }
                val toDelete = sorted.drop(1)
                for (entry in toDelete) {
                    val file = File(entry.path)
                    val outcome = runCatching { file.delete() }
                    if (outcome.getOrDefault(false)) {
                        deleted++
                        runCatching {
                            android.media.MediaScannerConnection.scanFile(
                                activity.applicationContext,
                                arrayOf(entry.path),
                                null,
                                null
                            )
                        }
                    } else {
                        failed++
                        if (file.exists() && !file.canWrite()) scopedStorageBlocked++
                    }
                }
            }
            if (scopedStorageBlocked > 0) {
                android.util.Log.w("DuplicateFinder", "$scopedStorageBlocked files failed due to scoped-storage restrictions (Android 10+)")
            }
            Handler(Looper.getMainLooper()).post {
                progress.dismiss()
                AlertDialog.Builder(activity)
                    .setTitle("Done")
                    .setMessage("Deleted $deleted duplicate files. Failed: $failed.\nRescan the gallery to refresh thumbnails.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }
}
