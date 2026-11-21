/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.performance

/** Số đợt giật trong một lần chơi thì gợi ý người dùng tối ưu */
const val STUTTER_SUGGEST_THRESHOLD = 5

/**
 * Phát hiện giật hình từ chuỗi mẫu FPS (mỗi mẫu cách nhau khoảng một giây).
 *
 * Một đợt giật là lúc FPS tụt xuống dưới [dropRatio] lần đỉnh trong [windowSize] mẫu gần nhất.
 * Cần đỉnh tối thiểu [minPeakFps] để không báo nhầm khi game đang tải.
 * Mẫu 0 (chưa có khung hình, đang tạm dừng) bị bỏ qua.
 * Một đợt tụt kéo dài nhiều giây chỉ được tính một lần, cho đến khi FPS hồi lại.
 */
class StutterDetector(
    private val windowSize: Int = 10,
    private val dropRatio: Float = 0.6f,
    private val minPeakFps: Int = 20
) {
    private val recent = ArrayDeque<Int>()
    private var inDrop = false

    /** Số đợt giật đã ghi nhận từ lần [reset] gần nhất */
    var count: Int = 0
        private set

    /**
     * Nhận một mẫu FPS.
     * @return true nếu mẫu này mở ra một đợt giật mới
     */
    fun onSample(fps: Int): Boolean {
        if (fps <= 0) return false
        val peak = recent.maxOrNull() ?: 0
        val dropping = peak >= minPeakFps && fps < peak * dropRatio
        val started = dropping && !inDrop
        if (started) count++
        inDrop = dropping
        recent.addLast(fps)
        if (recent.size > windowSize) recent.removeFirst()
        return started
    }

    fun reset() {
        recent.clear()
        inDrop = false
        count = 0
    }
}
