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

package com.movtery.zalithlauncher.ui.theme.festivals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.MinecraftColorText
import com.movtery.zalithlauncher.utils.festival.Festival
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FestivalTitleText(
    festivals: List<Festival>,
    style: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    if (festivals.isEmpty()) return

    //循环缩放仅在启动器位于前台时进行：游戏覆盖启动器（ON_STOP）期间暂停，避免后台持续重组与重绘
    val lifecycleOwner = LocalLifecycleOwner.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                scale.animateTo(1.1f, tween(durationMillis = 200, easing = LinearEasing))
                scale.animateTo(1f, tween(durationMillis = 200, easing = LinearEasing))
            }
        }
    }

    var festival by remember { mutableStateOf(festivals.first()) }
    if (festivals.size > 1) {
        LaunchedEffect(Unit) {
            var index = 0
            while (isActive) {
                delay(5000L.milliseconds)
                index = (index + 1) % festivals.size
                festival = festivals[index]
            }
        }
    }

    val text = stringResource(festival.textRes)
    MinecraftColorText(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        inputText = "§e$text",
        maxLines = maxLines,
        style = style,
    )
}