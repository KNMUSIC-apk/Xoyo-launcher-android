/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.record

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenRecordGeometryTest {

    @Test
    fun keepsSizeWhenWithinLimit() {
        assertEquals(1920 to 1080, recordSizeFor(1920, 1080))
        assertEquals(1280 to 720, recordSizeFor(1280, 720))
    }

    @Test
    fun scalesDownKeepingAspectRatio() {
        // 2400x1080: cạnh dài giới hạn 1920, cạnh ngắn theo cùng tỷ lệ là 864
        assertEquals(1920 to 864, recordSizeFor(2400, 1080))
    }

    @Test
    fun portraitScreenKeepsOrientation() {
        assertEquals(864 to 1920, recordSizeFor(1080, 2400))
    }

    @Test
    fun sidesAreAlwaysEven() {
        val (width, height) = recordSizeFor(2961, 1441)
        assertEquals(0, width % 2)
        assertEquals(0, height % 2)
    }

    @Test
    fun bitrateScalesWithPixelsAndIsCapped() {
        assertEquals(9_331_200, recordBitrateFor(1920, 1080, 30))
        assertEquals(12_000_000, recordBitrateFor(3840, 2160, 60))
    }
}
