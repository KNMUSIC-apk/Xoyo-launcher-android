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

import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.screens.content.elements.DisabledAlpha
import com.movtery.zalithlauncher.utils.math.addBigDecimal
import com.movtery.zalithlauncher.utils.math.subtractBigDecimal
import java.text.DecimalFormat

/**
 * 简单的文本滑动条，支持实时显示当前滑动条的数值，支持显示自定义数值的单位
 * @param shorter 是否使用更短的指示器的滑动条
 */
@Composable
fun SimpleTextSlider(
    modifier: Modifier = Modifier,
    shorter: Boolean = false,
    value: Float,
    decimalFormat: String = "#0.00",
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    toInt: Boolean = false,
    suffix: String? = null,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    fineTuningControl: Boolean = false,
    fineTuningStep: Float = 0.5f,
    valueText: ((Float) -> String)? = null,
    appendContent: @Composable () -> Unit = {}
) {
    val formatter = remember(decimalFormat) { DecimalFormat(decimalFormat) }
    fun getTextString(value: Float) = valueText?.invoke(value) ?: "${if (toInt) {
        value.toInt()
    } else {
        formatter.format(value)
    }}"

    fun changeValue(newValue: Float, finished: Boolean) {
        onValueChange(newValue)
        if (finished) onValueChangeFinished?.invoke()
    }

    LaunchedEffect(Unit) {
        //检查值是否被刻意的修改为超出范围
        if (value !in valueRange) {
            val newValue = value.coerceIn(valueRange)
            //调回范围内
            changeValue(newValue, true)
        }
    }

    val liquid = isLiquidGlassEnabled()
    val pillShape = RoundedCornerShape(percent = 50)
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (shorter) {
            IndicatorSlider(
                value = value,
                enabled = enabled,
                onValueChange = { changeValue(it, false) },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f)
            )
        } else {
            Slider(
                value = value,
                enabled = enabled,
                onValueChange = { changeValue(it, false) },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                colors = glassSliderColors(),
                modifier = Modifier.weight(1f)
            )
        }
        // Nhãn giá trị và hai nút ±: một viên kính duy nhất, không dùng vùng chạm tối thiểu
        // của Material (48dp) nên các phần tử không đè lên nhau.
        Row(
            modifier = Modifier
                .padding(start = 12.dp)
                .alpha(alpha = if (enabled) 1f else DisabledAlpha)
                .align(Alignment.CenterVertically)
                .then(
                    if (liquid) {
                        Modifier.liquidGlass(pillShape, pillColor.copy(alpha = 0.5f), enabled)
                    } else {
                        Modifier.background(pillColor, pillShape)
                    }
                )
                .padding(PaddingValues(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = 40.dp)
                    .then(
                        if (onTextClick != null) {
                            Modifier.clickable(enabled = enabled, onClick = onTextClick)
                        } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getTextString(value),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                suffix?.let { text ->
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (fineTuningControl) {
                Spacer(modifier = Modifier.width(4.dp))
                FineTuneButton(
                    painter = painterResource(R.drawable.ic_arrow_left_rounded),
                    enabled = enabled,
                    onClick = {
                        val newValue = value.subtractBigDecimal(fineTuningStep)
                        if (newValue <= valueRange.start) {
                            changeValue(valueRange.start, true)
                        } else {
                            changeValue(newValue, true)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                FineTuneButton(
                    painter = painterResource(R.drawable.ic_arrow_right_rounded),
                    enabled = enabled,
                    onClick = {
                        val newValue = value.addBigDecimal(fineTuningStep)
                        if (newValue >= valueRange.endInclusive) {
                            changeValue(valueRange.endInclusive, true)
                        } else {
                            changeValue(newValue, true)
                        }
                    }
                )
            }
        }
        appendContent()
    }
}

/**
 * Nút tăng/giảm nhỏ (28dp) cho thanh trượt. Tự dựng bằng Box thay cho IconButton để không bị
 * vùng chạm tối thiểu 48dp của Material làm nở bố cục.
 */
@Composable
private fun FineTuneButton(
    painter: Painter,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val liquid = isLiquidGlassEnabled()
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                if (liquid) {
                    Modifier.liquidGlass(CircleShape, Color.Transparent, enabled)
                } else {
                    Modifier.background(surface, CircleShape)
                }
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Màu thanh trượt hợp với Liquid Glass: rãnh mờ, phần đã kéo tô bằng màu chủ đề.
 * Tắt Liquid Glass thì dùng màu Material gốc.
 */
@Composable
fun glassSliderColors(): SliderColors {
    if (!isLiquidGlassEnabled()) return SliderDefaults.colors()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    return SliderDefaults.colors(
        thumbColor = primary,
        activeTrackColor = primary.copy(alpha = 0.85f),
        activeTickColor = primary.copy(alpha = 0.5f),
        inactiveTrackColor = onSurface.copy(alpha = 0.16f),
        inactiveTickColor = onSurface.copy(alpha = 0.3f),
    )
}

@Composable
fun IndicatorSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    colors: SliderColors = glassSliderColors()
) {
    /** Slider顶部需要裁切的像素 */
    val sliderTopCut = with(LocalDensity.current) { 8.dp.toPx().toInt() }
    /** Slider底部需要裁切的像素 */
    val sliderBottomCut = with(LocalDensity.current) { 6.dp.toPx().toInt() }
    Layout(
        modifier = modifier,
        content = {
            val state = rememberSliderState(
                value = value,
                steps = steps,
                trackRange = valueRange,
            )
            Slider(
                state = state,
                onValueChange = { value ->
                    onValueChange(value)
                    state.value = value
                },
                enabled = enabled,
                onValueChangeFinished = onValueChangeFinished,
                interactionSource = interactionSource,
                colors = colors,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = colors,
                        enabled = enabled,
                        thumbSize = DpSize(4.0.dp, 18.5.dp)
                    )
                }
            )
        }
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        val newHeight = (placeable.height - sliderTopCut - sliderBottomCut).coerceAtLeast(0)
        layout(placeable.width, newHeight) {
            placeable.place(0, -sliderTopCut)
        }
    }
}
