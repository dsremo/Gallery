package org.fossify.gallery.dsremo

import java.io.File

/**
 * Surfaces "this HEIC file may not display correctly on your Android version"
 * advisories.
 *
 * HEIC/HEIF photos came from iPhone or recent Samsung Galaxy cameras. Android
 * support is patchy:
 *   - API 28+ (Android 9+): HEIC display works
 *   - API 29+ (Android 10+): HEIC sharing/encoding works
 *   - Pre-API 28: shows broken thumbnail
 *
 * For a file the user can't see properly, suggest converting via Android's
 * built-in MediaCodec or showing a "convert to JPEG" hint.
 */
object HeicSupportAdvisor {

    enum class FormatVerdict {
        NATIVE_SUPPORT,
        DECODE_ONLY,
        UNSUPPORTED,
        NOT_HEIC
    }

    data class Advisory(
        val file: File,
        val format: String,
        val verdict: FormatVerdict,
        val advice: String?
    )

    fun inspect(file: File, androidSdkInt: Int): Advisory {
        val name = file.name.lowercase(java.util.Locale.ROOT)
        val isHeicLike = name.endsWith(".heic") || name.endsWith(".heif") || name.endsWith(".heics")
        if (!isHeicLike) {
            return Advisory(file, "non-HEIC", FormatVerdict.NOT_HEIC, null)
        }
        return when {
            androidSdkInt >= 29 -> Advisory(file, "HEIC", FormatVerdict.NATIVE_SUPPORT,
                "Native decode + encode. Sharing to non-HEIC recipients still needs conversion.")
            androidSdkInt >= 28 -> Advisory(file, "HEIC", FormatVerdict.DECODE_ONLY,
                "Display works. Sharing might fall back to JPEG via MediaStore.")
            else -> Advisory(file, "HEIC", FormatVerdict.UNSUPPORTED,
                "Android < 9 cannot decode HEIC natively. Convert to JPEG before viewing.")
        }
    }

    fun suggestedJpegName(file: File): String {
        val name = file.nameWithoutExtension
        return "$name.jpg"
    }
}
