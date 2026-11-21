/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.performance

import com.google.gson.Gson

/** Mức preset có sẵn. RAM đề xuất được tính theo tổng RAM thiết bị. */
enum class PerformancePreset(
    /** Tỷ lệ RAM thiết bị dành cho game */
    val ramFraction: Float,
    /** Trần RAM (MB) để không cấp quá nhiều cho máy có RAM lớn */
    val maxRamMb: Int,
    /** Tham số JVM gợi ý. Đây là cấu hình khởi đầu, nên thử và điều chỉnh trên máy thực. */
    val jvmArgs: String
) {
    LOW(ramFraction = 0.25f, maxRamMb = 2048, jvmArgs = "-XX:+UseSerialGC"),
    BALANCED(ramFraction = 0.35f, maxRamMb = 4096, jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=50"),
    HIGH(
        ramFraction = 0.5f,
        maxRamMb = 6144,
        jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:+ParallelRefProcEnabled"
    ),
    EXTREME(
        ramFraction = 0.6f,
        maxRamMb = 8192,
        jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=40 -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC"
    );
}

const val MIN_RAM_MB = 512
const val RAM_STEP_MB = 256

/**
 * RAM đề xuất (MB) cho [preset] trên thiết bị có [deviceTotalMb] MB.
 * Làm tròn xuống bội số của 256 MB và không thấp hơn 512 MB.
 */
fun recommendedRamMb(preset: PerformancePreset, deviceTotalMb: Long): Int {
    val raw = (deviceTotalMb * preset.ramFraction).toLong().coerceAtMost(preset.maxRamMb.toLong())
    val stepped = (raw / RAM_STEP_MB) * RAM_STEP_MB
    return stepped.coerceAtLeast(MIN_RAM_MB.toLong()).toInt()
}

/**
 * Gợi ý preset theo tổng RAM thiết bị.
 * Đây là ước tính từ dung lượng RAM, không phải kết quả đo hiệu năng.
 */
fun suggestPreset(deviceTotalMb: Long): PerformancePreset = when {
    deviceTotalMb < 6_000 -> PerformancePreset.LOW
    deviceTotalMb < 10_000 -> PerformancePreset.BALANCED
    deviceTotalMb < 14_000 -> PerformancePreset.HIGH
    else -> PerformancePreset.EXTREME
}

/** Bộ cấu hình tối ưu do người dùng lưu lại */
data class CustomPreset(
    val name: String,
    val ramMb: Int,
    val jvmArgs: String
)

private val presetGson = Gson()

/** Đọc danh sách preset đã lưu. JSON hỏng thì trả về danh sách rỗng, không ném lỗi. */
fun parseCustomPresets(json: String): List<CustomPreset> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        presetGson.fromJson(json, Array<CustomPreset>::class.java)?.toList().orEmpty()
    }.getOrDefault(emptyList())
}

fun encodeCustomPresets(presets: List<CustomPreset>): String = presetGson.toJson(presets)
