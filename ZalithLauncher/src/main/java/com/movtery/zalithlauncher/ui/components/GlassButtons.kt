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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/*
 * Các nút dưới đây cùng tên và cùng chữ ký với nút Material. Các file trong ui/ chỉ đổi import sang file này,
 * không đổi cách gọi. Khi có ảnh nền, nút hiện kính Liquid Glass (xem LiquidGlass.kt). Không có nền thì giữ
 * đúng giao diện Material.
 *
 * Nút chỉ dùng trong ui/components và các file đã import từ đây, không import lại từ androidx.compose.material3.
 */

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
        modifier = if (liquid) modifier.liquidGlass(shape, colors.containerColor(enabled).value, enabled) else modifier,
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
        modifier = if (liquid) modifier.liquidGlass(shape, colors.containerColor(enabled).value, enabled) else modifier,
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
        modifier = if (liquid) modifier.liquidGlass(shape, colors.containerColor(enabled).value, enabled) else modifier,
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
        modifier = if (liquid) modifier.liquidGlass(shape, colors.containerColor(enabled).value, enabled) else modifier,
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
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val liquid = isLiquidGlassEnabled()
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = if (liquid) modifier.liquidGlass(CircleShape, colors.containerColor(enabled).value, enabled) else modifier,
        enabled = enabled,
        colors = if (liquid) colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent) else colors,
        interactionSource = interactionSource,
        content = content
    )
}
