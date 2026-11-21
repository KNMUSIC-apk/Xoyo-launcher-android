package com.movtery.zalithlauncher.game.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceStatsTest {

    @Test
    fun parsesCpuLineAndTakesIdleAndIowait() {
        // user nice system idle iowait irq softirq steal guest guest_nice
        val totals = DeviceStats.parseCpuLine("cpu  100 0 50 800 50 0 0 0 0 0")

        assertEquals(requireNotNull(totals).idle, 850L)
        assertEquals(1_000L, totals.total)
    }

    @Test
    fun ignoresGuestFieldsWhenCountingTotal() {
        val totals = requireNotNull(DeviceStats.parseCpuLine("cpu 10 0 10 80 0 0 0 0 999 999"))

        assertEquals(100L, totals.total)
    }

    @Test
    fun rejectsNonCpuLines() {
        assertNull(DeviceStats.parseCpuLine("intr 12345 1 2 3"))
        assertNull(DeviceStats.parseCpuLine("cpu 1 2"))
    }

    @Test
    fun cpuUsageIsBusyShareOfDelta() {
        val previous = DeviceStats.CpuTotals(idle = 800L, total = 1_000L)
        val current = DeviceStats.CpuTotals(idle = 900L, total = 1_200L)

        // Khoảng đo: tổng +200, nhàn rỗi +100, bận 100 => 50%
        assertEquals(50f, requireNotNull(DeviceStats.cpuUsagePercent(previous, current)), 0.001f)
    }

    @Test
    fun cpuUsageIsNullWhenNoTimeElapsed() {
        val same = DeviceStats.CpuTotals(idle = 800L, total = 1_000L)

        assertNull(DeviceStats.cpuUsagePercent(same, same))
    }

    @Test
    fun thermalValueInMilliCelsiusIsConverted() {
        assertEquals(42f, requireNotNull(DeviceStats.parseThermalCelsius("42000\n")), 0.001f)
    }

    @Test
    fun thermalValueInCelsiusIsKept() {
        assertEquals(37.5f, requireNotNull(DeviceStats.parseThermalCelsius("37.5")), 0.001f)
    }

    @Test
    fun thermalNonsenseIsRejected() {
        assertNull(DeviceStats.parseThermalCelsius("abc"))
        assertNull(DeviceStats.parseThermalCelsius("900000"))
    }
}
