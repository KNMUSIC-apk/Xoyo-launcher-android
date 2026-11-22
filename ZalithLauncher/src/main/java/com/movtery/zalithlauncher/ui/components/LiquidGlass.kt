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
 * Liquid Glass cho nút.
 *
 * Chỉ bật khi người dùng đã đặt ảnh hoặc video nền. Không có nền thì nút giữ đúng giao diện Material gốc,
 * nên người dùng không có nền không thấy thay đổi nào.
 *
 * Ý tưởng tham khảo từ AmberLauncher (GPL-3.0, github.com/ItsVynzzzz/AmberLauncher).
 * Mã ở đây được viết lại, không sao chép.
 */

/**
 * Có đang hiển thị kính Liquid Glass cho nút không (tức là đã có nền).
 */
@Composable
fun isLiquidGlassEnabled(): Boolean = LocalBackgroundViewModel.current?.isValid == true

/**
 * Vẽ mặt kính của một nút. Chỉ gọi khi [isLiquidGlassEnabled] là true.
 *
 * - Đang bật làm mờ nền ở chế độ "Foreground", với độ mờ > 0: nền đã mờ nằm ngay dưới nút, phủ [tint] bán trong suốt.
 * - Còn lại: chỉ phủ [tint] bán trong suốt, không làm mờ.
 * Luôn thêm một lớp bóng nhẹ ở nửa trên và viền sáng để nút trông như kính.
 *
 * @param tint màu nền gốc của nút. Nếu trong suốt (ví dụ nút chữ) thì dùng màu surface.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color,
    enabled: Boolean = true
): Modifier {
    val base = if (tint.alpha > 0f) tint else MaterialTheme.colorScheme.surfaceBright
    // Độ trong suốt theo slider "Độ mờ nền", giống các thẻ khác của launcher.
    val color = influencedByBackgroundColor(color = base, enabled = enabled)
    val blurActive = LauncherSettings.backgroundBlurType.state == BackgroundBlur.Foreground &&
            LauncherSettings.backgroundBlur.state > 0 &&
            LauncherSettings.launcherBackgroundOpacity.state < 100
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
 * Viền kính cho thẻ bấm được. Không có nền thì trả về null để giữ nguyên viền của thẻ.
 */
@Composable
fun liquidRimBorder(): BorderStroke? =
    if (isLiquidGlassEnabled()) BorderStroke(0.8.dp, LiquidRim) else null
