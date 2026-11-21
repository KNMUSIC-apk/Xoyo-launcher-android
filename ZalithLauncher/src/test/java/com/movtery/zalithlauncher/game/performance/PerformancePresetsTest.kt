package com.movtery.zalithlauncher.game.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformancePresetsTest {

    @Test
    fun recommendedRamIsSteppedAndCapped() {
        // 8192 MB * 0.25 = 2048 => đúng trần LOW, chia hết cho 256
        assertEquals(2048, recommendedRamMb(PerformancePreset.LOW, deviceTotalMb = 8192))
        // 8192 * 0.35 = 2867.2 => làm tròn xuống bội số 256 = 2816
        assertEquals(2816, recommendedRamMb(PerformancePreset.BALANCED, deviceTotalMb = 8192))
        // 32768 * 0.6 = 19660 => bị chặn bởi trần EXTREME 8192
        assertEquals(8192, recommendedRamMb(PerformancePreset.EXTREME, deviceTotalMb = 32768))
    }

    @Test
    fun recommendedRamHasFloor() {
        assertEquals(512, recommendedRamMb(PerformancePreset.LOW, deviceTotalMb = 1000))
    }

    @Test
    fun suggestedPresetFollowsDeviceRam() {
        assertEquals(PerformancePreset.LOW, suggestPreset(4_000))
        assertEquals(PerformancePreset.BALANCED, suggestPreset(8_000))
        assertEquals(PerformancePreset.HIGH, suggestPreset(12_000))
        assertEquals(PerformancePreset.EXTREME, suggestPreset(16_000))
    }

    @Test
    fun customPresetsRoundTrip() {
        val presets = listOf(
            CustomPreset(name = "Pin lâu", ramMb = 3072, jvmArgs = "-XX:+UseSerialGC"),
            CustomPreset(name = "Mượt", ramMb = 4096, jvmArgs = "-XX:+UseG1GC")
        )

        val decoded = parseCustomPresets(encodeCustomPresets(presets))

        assertEquals(presets, decoded)
    }

    @Test
    fun brokenCustomPresetJsonGivesEmptyList() {
        assertTrue(parseCustomPresets("{not json").isEmpty())
        assertTrue(parseCustomPresets("").isEmpty())
    }
}
