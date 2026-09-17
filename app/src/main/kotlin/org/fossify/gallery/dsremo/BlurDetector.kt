package org.fossify.gallery.dsremo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.pow

/**
 * Detects blurry images using a Laplacian variance heuristic.
 *
 * Score ranges roughly:
 *   < 30   → very blurry (likely throwaway)
 *   30-70  → soft / out-of-focus
 *   70-150 → acceptable
 *   > 150  → sharp
 *
 * Works on a 128px downscale to keep per-image cost in the single-digit-ms range.
 */
object BlurDetector {

    private const val SCALED_DIMENSION = 128
    private const val THROWAWAY_THRESHOLD = 30.0
    private const val SOFT_THRESHOLD = 70.0

    enum class Verdict { THROWAWAY, SOFT, ACCEPTABLE, SHARP, UNREADABLE }

    data class Result(
        val sourceFile: File,
        val laplacianVariance: Double,
        val verdict: Verdict
    )

    fun analyseFile(file: File): Result? {
        if (!file.exists() || !file.isFile) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(file)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = runCatching { BitmapFactory.decodeFile(file.absolutePath, options) }.getOrNull()
            ?: return Result(file, 0.0, Verdict.UNREADABLE)
        val scaled = Bitmap.createScaledBitmap(sampled, SCALED_DIMENSION, SCALED_DIMENSION, true)
        if (scaled !== sampled) sampled.recycle()
        val variance = laplacianVariance(scaled)
        scaled.recycle()
        val verdict = when {
            variance < THROWAWAY_THRESHOLD -> Verdict.THROWAWAY
            variance < SOFT_THRESHOLD -> Verdict.SOFT
            variance < 150.0 -> Verdict.ACCEPTABLE
            else -> Verdict.SHARP
        }
        return Result(file, variance, verdict)
    }

    fun findBlurryIn(directory: File, recurse: Boolean = true, limit: Int = 500): List<Result> {
        val results = mutableListOf<Result>()
        directory.walkTopDown().maxDepth(if (recurse) 8 else 1).forEach { candidate ->
            if (results.size >= limit) return@forEach
            if (!candidate.isFile) return@forEach
            if (!candidate.name.lowercase(java.util.Locale.ROOT).let { name ->
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
                }) return@forEach
            analyseFile(candidate)?.let { outcome ->
                if (outcome.verdict == Verdict.THROWAWAY || outcome.verdict == Verdict.SOFT) {
                    results.add(outcome)
                }
            }
        }
        return results.sortedBy { it.laplacianVariance }
    }

    private fun computeSampleSize(file: File): Int {
        val sizeBucket = file.length()
        return when {
            sizeBucket > 4 * 1024 * 1024L -> 8
            sizeBucket > 1 * 1024 * 1024L -> 4
            sizeBucket > 256 * 1024L -> 2
            else -> 1
        }
    }

    private fun laplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 0.0
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val responses = DoubleArray((width - 2) * (height - 2))
        var responseIndex = 0
        for (rowIndex in 1 until height - 1) {
            for (columnIndex in 1 until width - 1) {
                val center = luma(pixels[rowIndex * width + columnIndex])
                val up = luma(pixels[(rowIndex - 1) * width + columnIndex])
                val down = luma(pixels[(rowIndex + 1) * width + columnIndex])
                val left = luma(pixels[rowIndex * width + columnIndex - 1])
                val right = luma(pixels[rowIndex * width + columnIndex + 1])
                responses[responseIndex++] = 4 * center - up - down - left - right
            }
        }
        val mean = responses.average()
        return responses.fold(0.0) { acc, value -> acc + (value - mean).pow(2) } / responses.size
    }

    private fun luma(argb: Int): Double {
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF
        return 0.299 * red + 0.587 * green + 0.114 * blue
    }

    @Suppress("UNUSED_PARAMETER")
    fun describe(context: Context, result: Result): String {
        return "${result.sourceFile.name}: ${result.verdict} (Laplacian variance = ${"%.1f".format(result.laplacianVariance)})"
    }
}
