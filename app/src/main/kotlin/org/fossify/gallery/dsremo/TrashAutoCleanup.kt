package org.fossify.gallery.dsremo

import android.content.Context
import android.util.Log
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.mediaDB
import java.io.File
import kotlin.concurrent.thread

object TrashAutoCleanup {

    private const val TAG = "DsremoTrashCleanup"
    private const val DAY_MS = 24L * 60L * 60L * 1000L
    private const val MIN_INTERVAL_MS = 12L * 60L * 60L * 1000L

    fun maybeRun(context: Context) {
        val cfg = context.config
        val retentionDays = cfg.dsremoTrashRetentionDays
        if (retentionDays <= 0) return
        val lastRunMs = cfg.dsremoTrashLastCleanupMs
        val now = System.currentTimeMillis()
        if (now - lastRunMs < MIN_INTERVAL_MS) return
        thread(name = "dsremo-trash-cleanup") {
            var completed = false
            runCatching {
                val cutoffMs = now - retentionDays * DAY_MS
                val dao = context.mediaDB
                val old = dao.getOldRecycleBinItems(cutoffMs)
                var deleted = 0
                for (medium in old) {
                    val file = File(medium.path)
                    val fileGone = !file.exists() || runCatching { file.delete() }.getOrDefault(false)
                    if (fileGone) {
                        runCatching { dao.deleteMediumPath(medium.path) }
                        deleted++
                    }
                }
                if (deleted > 0) {
                    Log.i(TAG, "auto-cleaned $deleted trashed items older than $retentionDays days")
                }
                completed = true
            }.onFailure { Log.w(TAG, "auto-cleanup failed: $it") }
            if (completed) cfg.dsremoTrashLastCleanupMs = now
        }
    }
}
