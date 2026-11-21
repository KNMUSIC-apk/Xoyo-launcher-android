/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.performance

import org.junit.Assert.assertEquals
import org.junit.Test

class StutterDetectorTest {

    private fun feed(detector: StutterDetector, vararg samples: Int): Int {
        samples.forEach { detector.onSample(it) }
        return detector.count
    }

    @Test
    fun steadyFpsHasNoStutter() {
        assertEquals(0, feed(StutterDetector(), 60, 60, 59, 61, 60, 60, 60))
    }

    @Test
    fun suddenDropIsCountedOnce() {
        // FPS tụt từ 60 xuống 20 và giữ nhiều giây: chỉ tính một đợt
        assertEquals(1, feed(StutterDetector(), 60, 60, 60, 20, 20, 20, 20))
    }

    @Test
    fun recoveryThenDropCountsAgain() {
        assertEquals(2, feed(StutterDetector(), 60, 60, 20, 60, 60, 20))
    }

    @Test
    fun zeroSamplesAreIgnored() {
        // 0 là lúc chưa có khung hình hoặc đang tạm dừng, không phải giật
        assertEquals(0, feed(StutterDetector(), 60, 0, 0, 60, 0))
    }

    @Test
    fun lowPeakDoesNotCount() {
        // đang tải game, FPS thấp ngay từ đầu không được tính là giật
        assertEquals(0, feed(StutterDetector(), 15, 5, 3, 1))
    }

    @Test
    fun resetClearsCountAndPeak() {
        val detector = StutterDetector()
        feed(detector, 60, 20)
        assertEquals(1, detector.count)
        detector.reset()
        assertEquals(0, detector.count)
        // sau khi reset không còn đỉnh nên mẫu 20 không bị tính
        assertEquals(0, feed(detector, 20))
    }
}
