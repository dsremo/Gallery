package org.fossify.gallery.dsremo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Finds near-duplicate (not byte-identical) photos using a perceptual hash.
 *
 * 8x8 dHash:
 *   1. Downscale to 9x8 greyscale.
 *   2. For each row, compare left pixel to right; 1 bit if left > right.
 *   3. Concatenate the 64 bits → one 64-bit signature.
 *
 * Two photos are "near-duplicates" if Hamming distance ≤ 5.
 *
 * Catches the "same shot at f/1.8 vs f/2.0" and "live photo + still"
 * combos that byte-hash deduplication misses.
 */
object PerceptualHashDeduper {

    data class Hashed(val file: File, val signature: Long)

    data class NearDuplicateGroup(
        val canonicalFile: File,
        val nearDuplicates: List<File>,
        val maxHammingDistance: Int
    )

    fun computeDhash(imageFile: File): Long? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = chooseSampleSize(imageFile.length())
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val source = runCatching { BitmapFactory.decodeFile(imageFile.absolutePath, options) }.getOrNull() ?: return null
        val scaled = Bitmap.createScaledBitmap(source, 9, 8, true)
        if (scaled !== source) source.recycle()
        val luma = IntArray(72)
        val rgb = IntArray(72)
        scaled.getPixels(rgb, 0, 9, 0, 0, 9, 8)
        for (index in 0 until 72) {
            val pixel = rgb[index]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            luma[index] = (red * 30 + green * 59 + blue * 11) / 100
        }
        scaled.recycle()
        var signature = 0L
        var bitIndex = 0
        for (rowIndex in 0 until 8) {
            for (columnIndex in 0 until 8) {
                val left = luma[rowIndex * 9 + columnIndex]
                val right = luma[rowIndex * 9 + columnIndex + 1]
                if (left > right) signature = signature or (1L shl bitIndex)
                bitIndex++
            }
        }
        return signature
    }

    fun findNearDuplicates(images: List<File>, maxDistance: Int = 5): List<NearDuplicateGroup> {
        val hashed = images.mapNotNull { file ->
            computeDhash(file)?.let { Hashed(file, it) }
        }
        val visited = mutableSetOf<File>()
        val results = mutableListOf<NearDuplicateGroup>()
        for (anchor in hashed) {
            if (anchor.file in visited) continue
            val cluster = mutableListOf<File>()
            var maxDistanceInCluster = 0
            for (candidate in hashed) {
                if (candidate.file == anchor.file || candidate.file in visited) continue
                val distance = java.lang.Long.bitCount(anchor.signature xor candidate.signature)
                if (distance <= maxDistance) {
                    cluster.add(candidate.file)
                    if (distance > maxDistanceInCluster) maxDistanceInCluster = distance
                }
            }
            if (cluster.isNotEmpty()) {
                cluster.forEach { visited.add(it) }
                visited.add(anchor.file)
                results.add(NearDuplicateGroup(anchor.file, cluster, maxDistanceInCluster))
            }
        }
        return results
    }

    private fun chooseSampleSize(byteCount: Long): Int = when {
        byteCount > 4 * 1024 * 1024L -> 16
        byteCount > 1 * 1024 * 1024L -> 8
        byteCount > 256 * 1024L -> 4
        else -> 2
    }
}
