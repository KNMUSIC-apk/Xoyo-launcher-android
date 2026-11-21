/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.account.wardrobe

import android.graphics.BitmapFactory
import java.io.File

/** Loại ảnh nhận diện theo kích thước: skin, cape, hoặc không dùng được */
enum class WebImageKind { SKIN, CAPE, INVALID }

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
)

/** Kiểm tra 8 byte đầu của file có đúng chữ ký PNG không */
fun File.isPngFile(): Boolean = runCatching {
    inputStream().use { input ->
        val header = ByteArray(PNG_SIGNATURE.size)
        input.read(header) == header.size && header.contentEquals(PNG_SIGNATURE)
    }
}.getOrDefault(false)

/**
 * Phân loại ảnh PNG theo kích thước.
 * Skin: 64x64 hoặc 64x32. Cape: tỉ lệ 2:1 (64x32, 128x64, ...).
 */
fun File.classifyPngImage(): WebImageKind {
    if (!isPngFile()) return WebImageKind.INVALID
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(absolutePath, options)
    val width = options.outWidth
    val height = options.outHeight
    return when {
        width == 64 && (height == 64 || height == 32) -> WebImageKind.SKIN
        width >= 64 && height > 0 && width == height * 2 -> WebImageKind.CAPE
        else -> WebImageKind.INVALID
    }
}
