/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.world

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** File khoá do game tạo khi mở thế giới. Bỏ qua khi sao lưu. */
private const val SESSION_LOCK = "session.lock"

/**
 * Nén cả thư mục thế giới [worldDir] thành một file zip trong [targetDir].
 * Tên file có dạng `<tên thế giới>_<timestamp>.zip`. Ghi vào file tạm rồi đổi tên, nên file dở dang không để lại.
 * Các mục trong zip bắt đầu bằng tên thư mục thế giới, để giải nén ra đúng thư mục.
 *
 * @throws IOException khi không ghi được file
 */
fun backupWorldToZip(worldDir: File, targetDir: File, timestamp: String): File {
    if (!worldDir.isDirectory) throw IOException("World folder not found: ${worldDir.name}")
    targetDir.mkdirs()
    val target = uniqueZipFile(targetDir, "${safeFileName(worldDir.name)}_$timestamp")
    val partial = File(targetDir, "${target.name}.part")
    try {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(partial))).use { zip ->
            worldDir.walkTopDown()
                .filter { it.isFile && it.name != SESSION_LOCK }
                .forEach { file ->
                    val relative = file.relativeTo(worldDir).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry("${worldDir.name}/$relative"))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        if (!partial.renameTo(target)) throw IOException("Cannot finish backup file: ${target.name}")
    } catch (e: Exception) {
        partial.delete()
        throw e
    }
    return target
}

/** Thay các ký tự không hợp lệ trong tên file bằng dấu gạch dưới */
internal fun safeFileName(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").trim().ifEmpty { "world" }

private fun uniqueZipFile(dir: File, baseName: String): File {
    var candidate = File(dir, "$baseName.zip")
    var index = 2
    while (candidate.exists()) {
        candidate = File(dir, "${baseName}_$index.zip")
        index++
    }
    return candidate
}
