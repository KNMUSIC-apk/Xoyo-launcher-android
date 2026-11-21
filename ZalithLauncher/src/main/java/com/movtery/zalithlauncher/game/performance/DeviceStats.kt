/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.performance

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File

/**
 * Đọc số liệu thiết bị từ hệ thống.
 * Mọi hàm đều trả về null khi hệ thống không cho đọc, không bao giờ trả số giả.
 */
object DeviceStats {

    /** Ảnh chụp RAM toàn thiết bị, byte */
    data class RamSnapshot(val totalBytes: Long, val availableBytes: Long) {
        val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    }

    /** Tổng hợp thời gian CPU từ /proc/stat, đơn vị jiffies */
    data class CpuTotals(val idle: Long, val total: Long)

    fun readRam(context: Context): RamSnapshot? = runCatching {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        RamSnapshot(totalBytes = info.totalMem, availableBytes = info.availMem)
    }.getOrNull()

    /** Nhiệt độ pin (độ C). Đây là dữ liệu đo thật từ hệ thống. */
    fun readBatteryTempCelsius(context: Context): Float? = runCatching {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        if (raw == Int.MIN_VALUE) null else raw / 10f
    }.getOrNull()

    /**
     * Phân tích dòng "cpu  user nice system idle iowait irq softirq steal ..." của /proc/stat.
     * Chỉ lấy 8 trường đầu để không đếm trùng guest/guest_nice.
     */
    fun parseCpuLine(line: String): CpuTotals? {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.firstOrNull() != "cpu") return null
        val values = parts.drop(1).mapNotNull { it.toLongOrNull() }.take(8)
        if (values.size < 4) return null
        val idle = values[3] + (values.getOrNull(4) ?: 0L)
        return CpuTotals(idle = idle, total = values.sum())
    }

    fun readCpuTotals(): CpuTotals? = runCatching {
        File("/proc/stat").useLines { lines ->
            lines.firstOrNull { it.startsWith("cpu ") }?.let { parseCpuLine(it) }
        }
    }.getOrNull()

    /** Tỷ lệ CPU đang bận (%) giữa hai lần đo. Trả null nếu khoảng đo quá ngắn hoặc không hợp lệ. */
    fun cpuUsagePercent(previous: CpuTotals, current: CpuTotals): Float? {
        val totalDelta = current.total - previous.total
        val idleDelta = current.idle - previous.idle
        if (totalDelta <= 0L || idleDelta < 0L) return null
        val busy = (totalDelta - idleDelta).toFloat() / totalDelta.toFloat() * 100f
        return busy.coerceIn(0f, 100f)
    }

    /**
     * Chuyển giá trị nhiệt độ thô của sysfs sang độ C.
     * Một số máy ghi theo mili-độ (ví dụ 42000), số khác ghi theo độ (42).
     */
    fun parseThermalCelsius(raw: String): Float? {
        val value = raw.trim().toFloatOrNull() ?: return null
        val celsius = if (value > 200f) value / 1000f else value
        return celsius.takeIf { it in -20f..150f }
    }

    /**
     * Nhiệt độ cao nhất trong các thermal zone đọc được.
     * Nhiều máy Android chặn quyền đọc này, khi đó trả null và giao diện phải nói rõ là không có dữ liệu.
     */
    fun readMaxThermalCelsius(): Float? = runCatching {
        File("/sys/class/thermal").listFiles { file -> file.name.startsWith("thermal_zone") }
            ?.mapNotNull { zone ->
                File(zone, "temp").takeIf { it.canRead() }?.readText()?.let { parseThermalCelsius(it) }
            }
            ?.maxOrNull()
    }.getOrNull()
}
