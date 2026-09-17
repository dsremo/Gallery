package org.fossify.gallery.dsremo

import java.io.File

/**
 * Surfaces "easy bytes to recover" inside the gallery's image folders:
 *  - thumbnails larger than the source (camera over-thumbnails)
 *  - WhatsApp `Sent` mirrors
 *  - screenshots older than 90 days that haven't been opened in 30 days
 *  - exact byte-identical copies in multiple folders
 *  - leftover empty subfolders
 *
 * Returns a ranked list of reclaim candidates so the user can free space
 * without rifling through 12k photos by hand.
 */
object StorageReclaimAdvisor {

    enum class Category {
        OVER_THUMBNAILED,
        WHATSAPP_SENT_DUPE,
        OLD_SCREENSHOT,
        SIZE_MATCH,
        EMPTY_FOLDER
    }

    data class Candidate(
        val sourceFile: File,
        val sizeBytes: Long,
        val category: Category,
        val rationale: String
    )

    fun scan(roots: List<File>, nowMs: Long): List<Candidate> {
        val candidates = mutableListOf<Candidate>()
        for (rootDir in roots) {
            scanRoot(rootDir, nowMs, candidates)
        }
        return candidates.sortedByDescending { it.sizeBytes }
    }

    private fun scanRoot(rootDir: File, nowMs: Long, accumulator: MutableList<Candidate>) {
        if (!rootDir.exists() || !rootDir.isDirectory) return
        val emptyDirs = mutableListOf<File>()
        val seenHashes = mutableMapOf<Long, File>()
        rootDir.walkTopDown().forEach { node ->
            if (node.isDirectory) {
                if (node.listFiles()?.isEmpty() == true) emptyDirs.add(node)
                return@forEach
            }
            if (!node.isFile) return@forEach
            val name = node.name.lowercase()
            val parentName = node.parentFile?.name?.lowercase() ?: ""

            if (parentName.contains("whatsapp") && parentName.contains("sent")) {
                accumulator.add(
                    Candidate(
                        node, node.length(), Category.WHATSAPP_SENT_DUPE,
                        "WhatsApp keeps a 'Sent' copy alongside the original — usually safe to drop."
                    )
                )
            }
            if (parentName.contains("screenshots") || name.startsWith("screenshot")) {
                val ageDays = (nowMs - node.lastModified()) / 86_400_000L
                if (ageDays > 90) {
                    accumulator.add(
                        Candidate(
                            node, node.length(), Category.OLD_SCREENSHOT,
                            "Screenshot is $ageDays days old — consider archiving or deleting."
                        )
                    )
                }
            }
            if (parentName.contains(".thumbnails") || name.startsWith(".thumb")) {
                val sibling = File(node.parentFile?.parentFile, node.name.removePrefix(".thumb_"))
                if (sibling.exists() && node.length() > sibling.length()) {
                    accumulator.add(
                        Candidate(
                            node, node.length(), Category.OVER_THUMBNAILED,
                            "Thumbnail ${node.length()}B > source ${sibling.length()}B — drop it."
                        )
                    )
                }
            }
            val sizeKey = node.length()
            val match = seenHashes[sizeKey]
            if (match != null && match != node && match.length() == node.length()) {
                accumulator.add(
                    Candidate(
                        node, node.length(), Category.SIZE_MATCH,
                        "Same-size sibling: ${match.absolutePath}"
                    )
                )
            } else if (match == null) {
                seenHashes[sizeKey] = node
            }
        }
        for (emptyDir in emptyDirs) {
            accumulator.add(
                Candidate(
                    emptyDir, 0L, Category.EMPTY_FOLDER,
                    "Folder is empty — remove to declutter the album list."
                )
            )
        }
    }
}
