/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Sau bao lâu không tương tác thì bắt đầu sao băng. */
private const val IDLE_BEFORE_STARS_MS = 15_000L

/** Thời gian một đợt sao băng kéo dài. */
private const val STAR_DURATION_MS = 3_000L

/** Khoảng nghỉ giữa hai đợt sao băng khi vẫn không có tương tác. */
private const val STAR_COOLDOWN_MS = 30_000L

private const val STAR_COUNT = 6

private val StarColor = Color(0xFFD8F4FF)

/**
 * Theo dõi tương tác của sảnh. Mỗi lần chạm hoặc cuộn sẽ gọi [notifyInteraction],
 * làm đếm lại thời gian chờ và tắt đợt sao băng đang chạy.
 */
class ShootingStarsState internal constructor() {
    internal val interactions = Channel<Unit>(Channel.CONFLATED)

    /** Đang trong một đợt sao băng hay không. */
    internal var active by mutableStateOf(false)

    /** Tăng lên mỗi đợt để tạo lại vị trí và hướng sao ngẫu nhiên. */
    internal var seed by mutableStateOf(0)

    fun notifyInteraction() {
        interactions.trySend(Unit)
    }
}

@Composable
fun rememberShootingStarsState(): ShootingStarsState = remember { ShootingStarsState() }

/**
 * Lớp phủ vẽ 5–6 ngôi sao băng bay chéo khắp màn hình sau khi sảnh không có tương tác 15 giây.
 * Mỗi đợt kéo dài 3 giây, nghỉ 30 giây rồi lặp lại nếu vẫn không có tương tác.
 * Lớp phủ không nhận chạm nên không chặn các nút bên dưới.
 */
@Composable
fun ShootingStarsOverlay(
    state: ShootingStarsState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state, enabled) {
        if (!enabled) {
            state.active = false
            return@LaunchedEffect
        }
        var needIdleWait = true
        while (true) {
            if (needIdleWait) awaitIdle(state, IDLE_BEFORE_STARS_MS)
            needIdleWait = false

            state.seed++
            state.active = true
            val interrupted = withTimeoutOrNull(STAR_DURATION_MS) {
                state.interactions.receive()
            } != null
            state.active = false

            if (interrupted) {
                // Có tương tác giữa đợt: đếm lại từ đầu.
                needIdleWait = true
                continue
            }
            val touchedDuringCooldown = withTimeoutOrNull(STAR_COOLDOWN_MS) {
                state.interactions.receive()
            } != null
            // Có tương tác trong lúc nghỉ thì chờ đủ 15 giây mới chạy lại; không thì chạy lại ngay.
            if (touchedDuringCooldown) needIdleWait = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (state.active) 1f else 0f,
        animationSpec = if (state.active) {
            tween(durationMillis = STAR_DURATION_MS.toInt(), easing = LinearEasing)
        } else {
            snap()
        },
        label = "ShootingStarsProgress"
    )
    val stars = remember(state.seed) { randomStars(state.seed) }

    if (progress <= 0f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        val headRadius = 2.5.dp.toPx()
        stars.forEach { star ->
            val local = (progress - star.start) / star.life
            if (local <= 0f || local >= 1f) return@forEach

            // Sao sáng dần rồi mờ đi trong suốt quãng bay của nó.
            val alpha = sin(PI.toFloat() * local)
            val travel = star.travel * w
            val head = Offset(
                x = star.x * w + star.dirX * travel * local,
                y = star.y * h + star.dirY * travel * local
            )
            val tail = Offset(
                x = head.x - star.dirX * star.tail * w,
                y = head.y - star.dirY * star.tail * w
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, StarColor.copy(alpha = alpha)),
                    start = tail,
                    end = head
                ),
                start = tail,
                end = head,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = StarColor.copy(alpha = alpha),
                radius = headRadius,
                center = head
            )
        }
    }
}

/** Chờ cho tới khi không có tương tác nào trong [ms] mili giây. */
private suspend fun awaitIdle(state: ShootingStarsState, ms: Long) {
    while (withTimeoutOrNull(ms) { state.interactions.receive() } != null) {
        // Mỗi tương tác làm đếm lại từ đầu.
    }
}

private class Star(
    val x: Float,
    val y: Float,
    val dirX: Float,
    val dirY: Float,
    val travel: Float,
    val tail: Float,
    val start: Float,
    val life: Float
)

/** Tạo sao ngẫu nhiên, mỗi sao bay chéo xuống bên trái, lệch thời điểm bắt đầu. Tỉ lệ theo [0, 1] của đợt. */
private fun randomStars(seed: Int): List<Star> {
    val random = Random(seed)
    return List(STAR_COUNT) {
        val angle = (25f + random.nextFloat() * 15f) * PI.toFloat() / 180f
        Star(
            x = 0.25f + random.nextFloat() * 0.85f,
            y = -0.1f + random.nextFloat() * 0.6f,
            dirX = -cos(angle),
            dirY = sin(angle),
            travel = 0.35f + random.nextFloat() * 0.25f,
            tail = 0.10f + random.nextFloat() * 0.10f,
            start = random.nextFloat() * 0.5f,
            life = 0.30f + random.nextFloat() * 0.10f
        )
    }
}
