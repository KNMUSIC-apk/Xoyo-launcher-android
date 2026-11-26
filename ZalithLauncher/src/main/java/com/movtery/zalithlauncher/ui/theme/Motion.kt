/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Bộ lò xo dùng chung cho giao diện XOYO.
 * Dùng lò xo thay cho tween: chuyển động tự nhiên và không khựng khi bị ngắt giữa chừng.
 */
object XoyoMotion {
    /** Tỉ lệ thu nhỏ khi nhấn giữ một mục bấm được */
    const val PressedScale = 0.96f

    /** Nảy nhẹ, phản hồi nhanh: nút nhấn, icon, mũi tên */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)

    /** Mềm, không nảy: nền hover, chiều rộng sidebar */
    fun <T> soft(): SpringSpec<T> = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * Thu nhỏ nhẹ bằng lò xo khi nhấn giữ. Trả về đúng tỉ lệ 1f khi thả tay.
 * @param enabled tắt khi bật giảm chuyển động
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) XoyoMotion.PressedScale else 1f,
        animationSpec = XoyoMotion.bouncy(),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
