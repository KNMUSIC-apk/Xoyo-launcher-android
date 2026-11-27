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

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.ui.components.AnimatedDirection
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import kotlin.math.roundToInt

/**
 * Hàng danh mục nằm ngang, cuộn được, có nền đánh dấu trượt tới mục đang chọn.
 * Dùng chung cho màn Cài đặt và màn Cài đặt phiên bản để hai nơi có cùng một kiểu thanh chọn.
 *
 * @param items danh sách danh mục, có thể có vạch chia nhóm
 * @param selectedKey danh mục đang mở
 * @param isVisible dùng để chạy hiệu ứng xuất hiện theo hướng [AnimatedDirection.Top]
 * @param navigateTo gọi khi người dùng bấm một danh mục
 */
@Composable
fun CategoryTabRow(
    items: List<CategoryItem>,
    selectedKey: TitledNavKey?,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    navigateTo: (TitledNavKey) -> Unit = {}
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val reducedMotion = LauncherSettings.launcherReducedMotion.state
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Vị trí và độ rộng của từng mục trong hàng (px), để nền đánh dấu trượt tới mục đang chọn
    val itemX = remember(items) { mutableStateMapOf<TitledNavKey, Float>() }
    val itemWidth = remember(items) { mutableStateMapOf<TitledNavKey, Float>() }
    val indicatorSpec: AnimationSpec<Float> = if (reducedMotion) {
        snap()
    } else {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }
    val indicatorX by animateFloatAsState(
        targetValue = selectedKey?.let { itemX[it] } ?: 0f,
        animationSpec = indicatorSpec,
        label = "categoryTabIndicatorX"
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = selectedKey?.let { itemWidth[it] } ?: 0f,
        animationSpec = indicatorSpec,
        label = "categoryTabIndicatorWidth"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .animated(ctx, from = AnimatedDirection.Top)
    ) {
        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            if (indicatorWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorX.roundToInt(), 0) }
                        .width(with(density) { indicatorWidth.toDp() })
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                items.forEach { item ->
                    if (item.division) {
                        VerticalDivider(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .height(24.dp)
                                .alpha(0.4f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    CategoryTab(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            itemX[item.key] = coordinates.positionInParent().x
                            itemWidth[item.key] = coordinates.size.width.toFloat()
                        },
                        selected = selectedKey == item.key,
                        icon = item.icon,
                        text = stringResource(item.textRes),
                        onClick = { navigateTo(item.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "categoryTabContent"
    )

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
