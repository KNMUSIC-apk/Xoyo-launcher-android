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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.setting.enums.BackgroundBlur
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel

/*
 * Liquid Glass cho nút và thẻ bấm được.
 *
 * Ý tưởng tham khảo từ AmberLauncher (GPL-3.0, github.com/ItsVynzzzz/AmberLauncher).
 * Mã ở đây được viết lại, không sao chép.
 */

/**
 * Chủ đề Liquid Glass có bật hay không. Đọc từ cài đặt "launcherLiquidGlass" (mặc định tắt).
 * Tắt thì nút, thanh trượt và thẻ quay về giao diện Material gốc.
 */
@Composable
fun isLiquidGlassEnabled(): Boolean = LauncherSettings.launcherLiquidGlass.state

/** Độ đục của lớp kính khi không có ảnh nền. Có ảnh nền thì dùng slider "Độ mờ nền". */
private const val GlassFillWithoutWallpaper = 0.72f

/**
 * Vẽ mặt kính của một nút.
 *
 * - Có ảnh nền, đang bật Foreground với độ mờ > 0: nền đã mờ nằm dưới nút, phủ màu bán trong suốt.
 * - Có ảnh nền, không làm mờ: màu bán trong suốt theo slider "Độ mờ nền".
 * - Không có ảnh nền: màu bán trong suốt cố định.
 * Luôn có viền sáng và một lớp bóng nhẹ ở nửa trên.
 *
 * @param tint màu nền gốc của nút. Nếu trong suốt (nút chữ, nút icon) thì dùng màu surface.
 * @param enabled nút đang bật. Khi tắt, lớp kính mờ hơn.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color,
    enabled: Boolean = true
): Modifier {
    if (!LauncherSettings.launcherLiquidGlass.state) return this
    val hasWallpaper = LocalBackgroundViewModel.current?.isValid == true
    val blurActive = hasWallpaper &&
            LauncherSettings.backgroundBlurType.state == BackgroundBlur.Foreground &&
            LauncherSettings.backgroundBlur.state > 0 &&
            LauncherSettings.launcherBackgroundOpacity.state < 100

    val base = if (tint.alpha > 0f) tint else MaterialTheme.colorScheme.surfaceBright
    val wallpaperFill = LauncherSettings.launcherBackgroundOpacity.state / 100f
    val fillAlpha = (if (hasWallpaper) wallpaperFill else GlassFillWithoutWallpaper) *
            (if (enabled) 1f else 0.6f)
    // Giữ nguyên độ trong suốt sẵn có của màu gốc (ví dụ màu nút tắt đã mờ sẵn).
    val color = base.copy(alpha = base.alpha * fillAlpha)

    val clipped = clip(shape)
    val filled = if (blurActive) {
        clipped.backgroundGlass(blur = LauncherSettings.backgroundBlur.state, color = color)
    } else {
        clipped.background(color)
    }
    return filled
        .background(LiquidGloss)
        .border(0.8.dp, LiquidRim, shape)
}

/** Viền sáng của kính, dùng chung cho nút và thẻ bấm được. */
private val LiquidRim = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.55f),
    0.5f to Color.White.copy(alpha = 0.08f),
    1f to Color.White.copy(alpha = 0.28f)
)

/** Lớp bóng nhẹ ở nửa trên của kính. */
private val LiquidGloss = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.16f),
    0.5f to Color.White.copy(alpha = 0.03f),
    1f to Color.Transparent
)

/**
 * Viền kính cho thẻ bấm được (chip tài khoản, các thẻ có onClick).
 */
fun liquidRimBorder(): BorderStroke = BorderStroke(0.8.dp, LiquidRim)
