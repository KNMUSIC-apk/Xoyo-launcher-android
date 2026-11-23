/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import kotlin.math.roundToInt

/**
 * Trạng thái của màn hình chờ lúc khởi động.
 * [progress] null nghĩa là chưa xác định được tiến độ (hiển thị thanh chạy vô định).
 * [error] khác null nghĩa là khởi tạo thất bại và người dùng có thể thử lại.
 */
data class SplashLoadingState(
    val progress: Float? = null,
    val message: String? = null,
    val error: String? = null,
)

/** Màu lấy từ logo khối lập phương: xanh chính và viền sáng xanh lơ. */
private val LogoBlue = Color(0xFF0059F8)
private val LogoCyan = Color(0xFF22C7FF)

/**
 * Màn hình chờ XOYO: logo ở giữa, thanh tiến trình kèm phần trăm,
 * tên ứng dụng và phiên bản ở phía dưới. Logo hiện ngay, không có hiệu ứng xuất hiện.
 * Màu tiến trình lấy từ logo; nền vẫn theo theme nên tự đổi theo Light/Dark Mode.
 */
@Composable
fun SplashLoadingScreen(
    state: SplashLoadingState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme

    // Chuyển động lơ lửng rất nhẹ của logo.
    val floatOffset by rememberInfiniteTransition(label = "SplashFloat").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SplashFloatOffset"
    )

    val rawProgress = state.progress
    val animatedProgress by animateFloatAsState(
        targetValue = (rawProgress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "SplashProgress"
    )
    val percent = (animatedProgress * 100).roundToInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Quầng sáng màu chủ đề phía sau logo, rất nhẹ để không gây nhiễu.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LogoBlue.copy(alpha = 0.18f),
                            LogoBlue.copy(alpha = 0.04f),
                            colorScheme.background.copy(alpha = 0f)
                        ),
                        center = Offset.Unspecified,
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = (floatOffset * 2f - 1f) * 3.dp.toPx()
                    }
            ) {
                Image(
                    modifier = Modifier.size(128.dp),
                    painter = painterResource(R.drawable.img_launcher),
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier.widthIn(max = 380.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.error != null) {
                    LoadingError(error = state.error, onRetry = onRetry)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = state.message
                                ?: stringResource(R.string.splash_loading_preparing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Text(
                            text = stringResource(R.string.splash_loading_percent, percent),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = LogoBlue
                        )
                    }

                    val trackShape = CircleShape
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(trackShape)
                            .background(colorScheme.onSurface.copy(alpha = 0.10f))
                    ) {
                        if (rawProgress == null) {
                            // Chưa biết tiến độ: thanh chạy vô định, không giả lập phần trăm.
                            val indeterminate by rememberInfiniteTransition(label = "SplashIndeterminate")
                                .animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1100, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "SplashIndeterminateOffset"
                                )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.32f)
                                    .graphicsLayer {
                                        translationX = (indeterminate * 3f - 1f) * size.width
                                    }
                                    .clip(trackShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(LogoBlue, LogoCyan)
                                        )
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(trackShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(LogoBlue, LogoCyan)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = BuildKeys.LAUNCHER_NAME,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.splash_loading_version,
                    BuildConfig.VERSION_NAME
                ),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingError(
    error: String,
    onRetry: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(30.dp),
            painter = painterResource(R.drawable.ic_warning_filled),
            contentDescription = null,
            tint = colorScheme.error
        )
        Text(
            text = stringResource(R.string.splash_loading_error_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.error
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
        Spacer(modifier = Modifier.height(4.dp))
        ScalingActionButton(onClick = onRetry) {
            Text(text = stringResource(R.string.splash_loading_retry))
        }
    }
}
