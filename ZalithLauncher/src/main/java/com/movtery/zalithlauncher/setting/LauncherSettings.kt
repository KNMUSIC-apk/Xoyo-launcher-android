/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.setting

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.movtery.zalithlauncher.setting.enums.ActionMenuSide
import com.movtery.zalithlauncher.setting.enums.AppLanguage
import com.movtery.zalithlauncher.setting.enums.BackgroundBlur
import com.movtery.zalithlauncher.setting.enums.DarkMode
import com.movtery.zalithlauncher.setting.enums.MirrorSourceType
import com.movtery.zalithlauncher.ui.theme.ColorThemeType

object LauncherSettings : SettingsRegistry() {
    /**
     * 颜色主题色
     * XOYO: bảng màu mặc định của bản chỉnh sửa này
     */
    val launcherColorTheme = enumSetting(
        "launcherColorTheme",
        ColorThemeType.XOYO
    )

    /**
     * 自定义颜色主题色
     */
    val launcherCustomColor = intSetting("launcherCustomColor", Color.Blue.toArgb())

    /**
     * 自定义颜色配色风格
     */
    val launcherCustomPaletteStyle = enumSetting("launcherCustomPaletteStyle", PaletteStyle.TonalSpot)

    /**
     * 启动器UI深色主题
     */
    val launcherDarkMode = enumSetting("launcherDarkMode", DarkMode.FollowSystem)

    /**
     * 启动器语言
     */
    val launcherLanguage = enumSetting("launcherLanguage", AppLanguage.FOLLOW_SYSTEM)

    /**
     * 启动器部分屏幕全屏
     */
    val launcherFullScreen = boolSetting("launcherFullScreen", true)

    /**
     * 持续型节日彩蛋效果
     */
    val launcherFestivalEffects = boolSetting("launcherFestivalEffects", true)

    /**
     * 启用页面切换动画
     */
    val launcherSwapAnimEnabled = boolSetting("launcherSwapAnimEnabled", true)

    /**
     * 动画速度
     */
    val launcherAnimateSpeedLevel = intSetting("launcherAnimateSpeedLevel", 10, 1..30)

    /**
     * 主界面操作菜单的停泊侧
     */
    val launcherActionMenuSide = enumSetting("launcherActionMenuSide", ActionMenuSide.END)

    /**
     * 启动器任务菜单是否展开
     */
    val launcherTaskMenuExpanded = boolSetting("launcherTaskMenuExpanded", true)

    /**
     * XOYO：sidebar điều hướng có đang mở rộng không (màn hình rộng)
     */
    val launcherSidebarExpanded = boolSetting("launcherSidebarExpanded", true)

    /**
     * XOYO：giảm chuyển động (sidebar, số liệu trên Hero). Dành cho máy cấu hình yếu.
     */
    val launcherReducedMotion = boolSetting("launcherReducedMotion", false)

    /**
     * XOYO：các thao tác nhanh hiển thị trên Tổng quan, theo đúng thứ tự, phân tách bằng dấu phẩy.
     * Tên các thao tác khớp với enum QuickAction.
     */
    val launcherQuickActions = stringSetting(
        "launcherQuickActions",
        "CREATE_PROFILE,MANAGE_VERSIONS,INSTALL_MODS,INSTALL_SHADERS,INSTALL_RESOURCE_PACKS,MANAGE_WORLDS,PERFORMANCE,OPEN_GAME_FOLDER"
    )

    /**
     * XOYO：các bộ cấu hình tối ưu do người dùng lưu (JSON)
     */
    val performanceCustomPresets = stringSetting("performanceCustomPresets", "[]")

    /**
     * 启动器背景元素不透明度
     */
    val launcherBackgroundOpacity = intSetting("launcherBackgroundOpacity", 80, 20..100)

    /**
     * XOYO：độ rõ của chữ (20..100, mặc định 50). Kéo tăng thì chữ rõ hơn, không bị chìm vào nền.
     */
    val launcherTextOpacity = intSetting("launcherTextOpacity", 45, 20..100)

    /**
     * XOYO：bật/tắt chủ đề Liquid Glass cho nút, thanh trượt và thẻ bấm được.
     * Mặc định tắt: giao diện nhẹ hơn và không phải tính lại lớp kính khi kéo thanh độ mờ.
     */
    val launcherLiquidGlass = boolSetting("launcherLiquidGlass", false)

    /**
     * XOYO：đường dẫn font tùy chỉnh đã sao chép vào bộ nhớ riêng của launcher. Rỗng nghĩa là dùng font mặc định.
     */
    val launcherCustomFontPath = stringSetting("launcherCustomFontPath", "")

    /**
     * XOYO：ẩn phần Tổng quan trên trang chủ để nhìn rõ nền phía sau.
     */
    val launcherHomeOverviewHidden = boolSetting("launcherHomeOverviewHidden", false)

    /**
     * 启动器视频背景音量
     */
    val videoBackgroundVolume = intSetting("videoBackgroundVolume", 0, 0..100)

    /**
     * 启动器背景模糊效果
     */
    val backgroundBlur = intSetting("backgroundBlur", 0, 0..40)

    /**
     * 启动器背景模糊效果类型
     */
    val backgroundBlurType = enumSetting("backgroundBlurType", BackgroundBlur.Background)

    /**
     * 启动器日志保留天数
     */
    val launcherLogRetentionDays = intSetting("launcherLogRetentionDays", 7, 1..14)

    /**
     * 游戏内容下载源
     */
    val gameDownloadSource = enumSetting(
        "gameDownloadSource",
        MirrorSourceType.AUTO,
        MirrorSourceType.LEGACY_NAMES
    )

    /**
     * 资源平台下载源
     */
    val addonPlatformSource = enumSetting(
        //历史遗留键名，保留原值以兼容旧版本数据
        "assetPlatformSource",
        MirrorSourceType.AUTO,
        MirrorSourceType.LEGACY_NAMES
    )
}
