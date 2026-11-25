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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import com.movtery.zalithlauncher.setting.LauncherSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/*
 * Các nút dưới đây cùng tên và cùng chữ ký với nút Material. Các file trong ui/ chỉ đổi import sang file này,
 * không đổi cách gọi. Khi có ảnh nền, nút hiện kính Liquid Glass (xem LiquidGlass.kt). Không có nền thì giữ
 * đúng giao diện Material.
 */

/**
 * Màu nền để vẽ kính: màu của nút khi bật; khi tắt dùng 12% onSurface như Material.
 */
@Composable
private fun glassTint(containerColor: Color, enabled: Boolean): Color =
    if (enabled) containerColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val liquid = isLiquidGlassEnabled()
    val defaultSource = remember { MutableInteractionSource() }
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = if (liquid) modifier.liquidGlass(shape, glassTint(colors.containerColor, enabled), enabled) else modifier,
        enabled = enabled,
        shape = shape,
        colors = if (liquid) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent) else colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource ?: defaultSource,
        content = content
    )
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val liquid = isLiquidGlassEnabled()
    val defaultSource = remember { MutableInteractionSource() }
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = if (liquid) modifier.liquidGlass(shape, glassTint(colors.containerColor, enabled), enabled) else modifier,
        enabled = enabled,
        shape = shape,
        colors = if (liquid) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent) else colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource ?: defaultSource,
        content = content
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val liquid = isLiquidGlassEnabled()
    val defaultSource = remember { MutableInteractionSource() }
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = if (liquid) modifier.liquidGlass(shape, glassTint(colors.containerColor, enabled), enabled) else modifier,
        enabled = enabled,
        shape = shape,
        colors = if (liquid) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent) else colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource ?: defaultSource,
        content = content
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val liquid = isLiquidGlassEnabled()
    val defaultSource = remember { MutableInteractionSource() }
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = if (liquid) modifier.liquidGlass(shape, glassTint(colors.containerColor, enabled), enabled) else modifier,
        enabled = enabled,
        shape = shape,
        colors = if (liquid) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent) else colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource ?: defaultSource,
        content = content
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val liquid = isLiquidGlassEnabled()
    // XOYO: không ép vùng chạm tối thiểu 48dp của Material. Nút đặt 34dp thì vẽ đúng 34dp,
    // nên các nút cạnh nhau (ví dụ nút tải và nút mũi tên trong thẻ cài đặt) không bị chồng lên nhau.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        androidx.compose.material3.IconButton(
            onClick = onClick,
            modifier = if (liquid) modifier.liquidGlass(shape, glassTint(colors.containerColor, enabled), enabled) else modifier,
            enabled = enabled,
            shape = shape,
            colors = if (liquid) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent) else colors,
            interactionSource = interactionSource,
            content = content
        )
    }
}

/**
 * Nút icon gọn cho thanh trên. Kích thước lấy từ [modifier] (thường 32dp) và không có vùng chạm tối thiểu 48dp
 * của Material, nên các nút đặt cạnh nhau không chồng lên nhau. Khi bật Liquid Glass thì vẽ kính tròn cùng kích thước.
 */
@Composable
fun CompactIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalContentColor.current,
    content: @Composable () -> Unit
) {
    val fill = if (isLiquidGlassEnabled()) {
        Modifier.liquidGlass(shape, glassTint(containerColor, enabled), enabled)
    } else {
        Modifier.background(if (enabled) containerColor else Color.Transparent, shape)
    }
    val iconColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
    // XOYO: nút co nhẹ khi nhấn bằng lò xo; tắt khi bật "Giảm chuyển động"
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LauncherSettings.launcherReducedMotion.state
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "compactIconPressScale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .then(fill)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides iconColor) {
            content()
        }
    }
}
