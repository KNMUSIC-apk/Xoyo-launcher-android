/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.performance

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipFile

enum class DiagnosticSeverity { CRITICAL, WARNING, INFO }

enum class DiagnosticCode {
    OUT_OF_MEMORY,
    JAVA_VERSION_MISMATCH,
    MISSING_CLASS,
    DUPLICATE_MOD,
    MIXIN_FAILURE,
    CORRUPTED_FILE,
    NATIVE_LIBRARY,
    MISSING_DEPENDENCY,
    LOW_DISK_SPACE,
    HIGH_RAM_ALLOCATION,
    CORRUPTED_MOD_JAR,
    NO_ISSUES
}

/**
 * Kết quả một luật chẩn đoán.
 * @param detail thông tin kèm theo (ví dụ tên file hoặc số liệu), có thể null
 */
data class DiagnosticFinding(
    val code: DiagnosticCode,
    val severity: DiagnosticSeverity,
    val detail: String? = null
)

/**
 * Dữ liệu đầu vào cho bộ luật. Giá trị null nghĩa là chưa đọc được, luật tương ứng sẽ bị bỏ qua.
 */
data class DiagnosticInput(
    /** Phần cuối của log gần nhất, null nếu không có log */
    val logText: String?,
    /** Dung lượng trống của ổ chứa thư mục game (byte) */
    val freeDiskBytes: Long?,
    /** RAM cấp phát của phiên bản (MB) */
    val allocatedRamMb: Int?,
    /** Tổng RAM thiết bị (MB) */
    val deviceTotalMb: Long?,
    /** Tên các file mod không mở được như file zip */
    val brokenModJars: List<String>
)

/**
 * Trợ lý chẩn đoán dựa trên luật cục bộ.
 * Không dùng AI và không gọi mạng. Mỗi kết quả là một luật khớp với dữ liệu, không phải chẩn đoán chắc chắn.
 */
object LauncherDiagnostics {
    private const val MB = 1024L * 1024L

    private val duplicateRegex = Regex("duplicate mods?|found duplicate", RegexOption.IGNORE_CASE)
    private val dependencyRegex = Regex(
        "Incompatible mod set|Mod resolution failed|requires any version of|Missing dependency",
        RegexOption.IGNORE_CASE
    )
    private val mixinRegex = Regex("InvalidInjectionException|Mixin apply failed|MixinApplyError")
    private val corruptRegex = Regex("ZipException|invalid LOC header|Corrupted")
    private val nativeRegex = Regex("UnsatisfiedLinkError|dlopen failed")

    fun analyze(input: DiagnosticInput): List<DiagnosticFinding> {
        val findings = mutableListOf<DiagnosticFinding>()
        val log = input.logText.orEmpty()

        if (log.isNotEmpty()) {
            if (log.contains("OutOfMemoryError")) {
                findings += DiagnosticFinding(DiagnosticCode.OUT_OF_MEMORY, DiagnosticSeverity.CRITICAL)
            }
            if (log.contains("UnsupportedClassVersionError")) {
                findings += DiagnosticFinding(DiagnosticCode.JAVA_VERSION_MISMATCH, DiagnosticSeverity.CRITICAL)
            }
            if (log.contains("NoClassDefFoundError") || log.contains("ClassNotFoundException")) {
                findings += DiagnosticFinding(DiagnosticCode.MISSING_CLASS, DiagnosticSeverity.WARNING)
            }
            if (duplicateRegex.containsMatchIn(log)) {
                findings += DiagnosticFinding(DiagnosticCode.DUPLICATE_MOD, DiagnosticSeverity.WARNING)
            }
            if (dependencyRegex.containsMatchIn(log)) {
                findings += DiagnosticFinding(DiagnosticCode.MISSING_DEPENDENCY, DiagnosticSeverity.WARNING)
            }
            if (mixinRegex.containsMatchIn(log)) {
                findings += DiagnosticFinding(DiagnosticCode.MIXIN_FAILURE, DiagnosticSeverity.WARNING)
            }
            if (corruptRegex.containsMatchIn(log)) {
                findings += DiagnosticFinding(DiagnosticCode.CORRUPTED_FILE, DiagnosticSeverity.WARNING)
            }
            if (nativeRegex.containsMatchIn(log)) {
                findings += DiagnosticFinding(DiagnosticCode.NATIVE_LIBRARY, DiagnosticSeverity.WARNING)
            }
        }

        val free = input.freeDiskBytes
        if (free != null) {
            if (free < 200 * MB) {
                findings += DiagnosticFinding(
                    DiagnosticCode.LOW_DISK_SPACE,
                    DiagnosticSeverity.CRITICAL,
                    formatMb(free)
                )
            } else if (free < 1024 * MB) {
                findings += DiagnosticFinding(
                    DiagnosticCode.LOW_DISK_SPACE,
                    DiagnosticSeverity.WARNING,
                    formatMb(free)
                )
            }
        }

        val allocated = input.allocatedRamMb
        val total = input.deviceTotalMb
        if (allocated != null && total != null && total > 0L && allocated > total * 0.8) {
            findings += DiagnosticFinding(
                DiagnosticCode.HIGH_RAM_ALLOCATION,
                DiagnosticSeverity.WARNING,
                "$allocated MB / $total MB"
            )
        }

        if (input.brokenModJars.isNotEmpty()) {
            findings += DiagnosticFinding(
                DiagnosticCode.CORRUPTED_MOD_JAR,
                DiagnosticSeverity.WARNING,
                input.brokenModJars.joinToString(", ")
            )
        }

        if (findings.isEmpty()) {
            return listOf(DiagnosticFinding(DiagnosticCode.NO_ISSUES, DiagnosticSeverity.INFO))
        }
        return findings.sortedBy { it.severity.ordinal }
    }

    private fun formatMb(bytes: Long): String = "${bytes / MB} MB"

    /**
     * Các file .jar trong thư mục mod không mở được như file zip (thường là file tải về bị cắt).
     * Hàm này đọc đĩa, phải gọi trên luồng IO.
     */
    fun findBrokenJars(modsDir: File): List<String> {
        val jars = modsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".jar", ignoreCase = true)
        } ?: return emptyList()
        return jars.filter { jar ->
            runCatching { ZipFile(jar).use { zip -> zip.size() } }.isFailure
        }.map { it.name }.sorted()
    }

    /**
     * Đọc phần cuối của file log, tối đa [maxBytes] byte.
     * Hàm này đọc đĩa, phải gọi trên luồng IO. Trả null nếu không đọc được.
     */
    fun readLogTail(file: File, maxBytes: Long = 512L * 1024L): String? {
        if (!file.isFile) return null
        return runCatching {
            val length = file.length()
            val start = (length - maxBytes).coerceAtLeast(0L)
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val bytes = ByteArray((length - start).toInt())
                raf.readFully(bytes)
                String(bytes, Charsets.UTF_8)
            }
        }.getOrNull()
    }
}
