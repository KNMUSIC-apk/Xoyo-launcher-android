/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.record

import kotlin.math.roundToInt

/**
 * Kích thước video ghi hình: giữ nguyên tỷ lệ và hướng của màn hình,
 * không vượt quá [maxLong] x [maxShort]. Hai cạnh luôn là số chẵn vì bộ mã hoá H.264 yêu cầu.
 */
fun recordSizeFor(
    screenWidth: Int,
    screenHeight: Int,
    maxLong: Int = 1920,
    maxShort: Int = 1080
): Pair<Int, Int> {
    val longSide = maxOf(screenWidth, screenHeight).coerceAtLeast(2).toFloat()
    val shortSide = minOf(screenWidth, screenHeight).coerceAtLeast(2).toFloat()
    val scale = minOf(1f, maxLong / longSide, maxShort / shortSide)
    val long = evenAtLeastTwo(longSide * scale)
    val short = evenAtLeastTwo(shortSide * scale)
    return if (screenWidth >= screenHeight) long to short else short to long
}

private fun evenAtLeastTwo(value: Float): Int = ((value / 2f).roundToInt() * 2).coerceAtLeast(2)

/**
 * Bitrate (bit/giây) cho kích thước và tốc độ khung hình đã cho, tối đa 12 Mbps.
 * Hệ số 0,15 bit cho mỗi điểm ảnh mỗi khung là mức đủ rõ với H.264 trên thiết bị di động.
 */
fun recordBitrateFor(width: Int, height: Int, fps: Int): Int {
    val raw = width.toLong() * height * fps * 15L / 100L
    return raw.coerceAtMost(12_000_000L).toInt()
}
