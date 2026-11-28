/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.setting

/**
 * Vị trí của bảng Liquid Island trong game.
 */
enum class IslandAnchor {
    TopStart, TopCenter, TopEnd, BottomStart, BottomCenter, BottomEnd
}

/**
 * Cài đặt của Liquid Island (trình phát nhạc). Tất cả lưu vào MMKV như các cài đặt khác
 * nên tự khôi phục sau khi khởi động lại launcher.
 */
object LiquidIslandSettings : SettingsRegistry() {
    /** Công tắc tổng */
    val enabled = boolSetting("liquidIsland.enabled", true)

    /** Bật trình phát nhạc (tắt thì bảng chỉ hiện trạng thái trống) */
    val playerEnabled = boolSetting("liquidIsland.playerEnabled", true)

    /** Tự mở bảng khi nhạc bắt đầu phát (trong game) */
    val autoShowOnPlay = boolSetting("liquidIsland.autoShowOnPlay", true)

    /** Hiện bảng Liquid Island trong menu game */
    val inGameOverlay = boolSetting("liquidIsland.inGameOverlay", true)

    /** Vị trí bảng trong game */
    val panelAnchor = enumSetting("liquidIsland.panelAnchor", IslandAnchor.TopEnd)

    /** Kích thước bảng (%) */
    val panelScale = intSetting("liquidIsland.panelScale", 100, 70..130)

    /** Hiệu ứng chuyển động */
    val animations = boolSetting("liquidIsland.animations", true)

    /** Hiệu ứng kính (nền mờ, viền sáng) */
    val liquidGlass = boolSetting("liquidIsland.liquidGlass", true)

    /** Hiện dải sóng âm */
    val waveform = boolSetting("liquidIsland.waveform", true)

    /** Độ đục của bảng (%) */
    val opacity = intSetting("liquidIsland.opacity", 88, 40..100)

    /** Độ mờ kính của bảng (0 = không mờ) */
    val blur = intSetting("liquidIsland.blur", 18, 0..40)

    /** Thư mục nhạc (tree URI của SAF); rỗng = toàn bộ nhạc trên máy */
    val libraryFolder = stringSetting("liquidIsland.libraryFolder", "")

    /** Bỏ qua bản nhạc ngắn hơn số giây này (âm thanh chuông, hiệu ứng) */
    val minDurationSeconds = intSetting("liquidIsland.minDurationSeconds", 30, 0..300)

    /** Tiếp tục phát khi launcher bị đẩy xuống nền */
    val backgroundPlayback = boolSetting("liquidIsland.backgroundPlayback", true)

    /** Âm lượng trình phát (%) */
    val volume = intSetting("liquidIsland.volume", 100, 0..100)
}

/**
 * Dữ liệu người dùng của Liquid Island: yêu thích, vừa phát và danh sách phát.
 * Lưu dạng chuỗi để dùng chung MMKV đa tiến trình với tiến trình game.
 *
 * Định dạng:
 * - favorites/recent: danh sách id bài hát
 * - playlists: "tên|id,id,id"
 */
object LiquidIslandStore : SettingsRegistry() {
    val favorites = stringListSetting("liquidIsland.favorites", emptyList())
    val recent = stringListSetting("liquidIsland.recent", emptyList())
    val playlists = stringListSetting("liquidIsland.playlists", emptyList())
}
