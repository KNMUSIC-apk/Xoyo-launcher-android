/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.ui.screens.content.liquid

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.music.LiquidPlayback
import com.movtery.zalithlauncher.music.LiquidSong
import com.movtery.zalithlauncher.setting.LiquidIslandSettings
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import kotlin.math.abs
import kotlin.math.sin

/** "m:ss" hoặc "h:mm:ss" */
fun formatTrackTime(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

/**
 * Mặt kính của Liquid Island.
 *
 * shortcut: Compose không làm mờ được SurfaceView của Minecraft, nên "độ mờ kính" điều khiển
 * độ đục của lớp phủ thay vì blur thật; nâng cấp bằng RenderEffect khi có lớp nền chụp được.
 */
@Composable
fun LiquidIslandSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LiquidIslandSettings.liquidGlass.state
    val alpha = LiquidIslandSettings.opacity.state / 100f
    val blur = LiquidIslandSettings.blur.state / 40f
    // Càng "mờ kính" thì lớp phủ càng đục để chữ vẫn đọc được
    val container = cardColor(false).copy(alpha = (alpha * (0.72f + blur * 0.28f)).coerceIn(0f, 1f))

    BackgroundCard(
        modifier = modifier,
        influencedByBackground = false,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onCardColor()),
        border = if (glass) BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)) else null
    ) {
        content()
    }
}

/** Ảnh bìa album, tự chuyển sang icon khi không có ảnh */
@Composable
fun LiquidArtwork(
    song: LiquidSong?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    Box(
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val artwork = song?.artworkUri
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Dải sóng âm trang trí.
 *
 * shortcut: biên độ là hoạt ảnh, không phải phổ thật (không đụng vào luồng âm thanh);
 * nâng cấp bằng android.media.audiofx.Visualizer nếu cần phổ thật.
 */
@Composable
fun LiquidWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    bars: Int = 18,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animate = isPlaying && LiquidIslandSettings.waveform.state && LiquidIslandSettings.animations.state
    val phase = if (animate) {
        val transition = rememberInfiniteTransition(label = "liquidWave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1500, easing = LinearEasing)),
            label = "liquidWavePhase"
        ).value
    } else 0f

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val gap = size.width / (bars * 3f)
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (index in 0 until bars) {
            val wave = sin((phase + index * 0.11f) * 2f * Math.PI.toFloat())
            val height = size.height * (if (animate) 0.28f + abs(wave) * 0.72f else 0.3f + abs(sin(index * 0.7f)) * 0.4f)
            val left = index * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(left, (size.height - height) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

/** Thanh tiến trình có thể tua; giữ giá trị kéo tay để không giật khi đang phát */
@Composable
fun LiquidProgress(
    playback: LiquidPlayback,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val max = playback.durationMs.coerceAtLeast(1L).toFloat()
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val value = if (dragging) dragValue else playback.positionMs.toFloat().coerceIn(0f, max)

    Column(modifier = modifier) {
        Slider(
            value = value,
            valueRange = 0f..max,
            enabled = playback.song != null,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                dragging = false
                onSeek(dragValue.toLong())
            }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTrackTime(value.toLong()), style = MaterialTheme.typography.labelSmall)
            Text(formatTrackTime(playback.durationMs), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Cụm điều khiển: trộn bài, lùi, phát/dừng, tới, lặp */
@Composable
fun LiquidTransport(
    playback: LiquidPlayback,
    modifier: Modifier = Modifier,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    compact: Boolean = false
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 8.dp)
    ) {
        IconButton(onClick = onShuffle) {
            Icon(
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = stringResource(R.string.liquid_shuffle),
                tint = if (playback.shuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onPrevious, enabled = playback.count > 0) {
            Icon(
                modifier = Modifier.size(if (compact) 24.dp else 28.dp),
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = stringResource(R.string.liquid_previous)
            )
        }
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(if (compact) 40.dp else 48.dp)
                .clip(CircleShape)
                .background(accent)
        ) {
            Icon(
                modifier = Modifier.size(if (compact) 22.dp else 26.dp),
                painter = painterResource(
                    if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow_filled
                ),
                contentDescription = stringResource(
                    if (playback.isPlaying) R.string.liquid_pause else R.string.liquid_play
                ),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        IconButton(onClick = onNext, enabled = playback.count > 0) {
            Icon(
                modifier = Modifier.size(if (compact) 24.dp else 28.dp),
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.liquid_next)
            )
        }
        IconButton(onClick = onRepeat) {
            Icon(
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                painter = painterResource(R.drawable.ic_repeat),
                contentDescription = stringResource(
                    when (playback.repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> R.string.liquid_repeat_all
                        androidx.media3.common.Player.REPEAT_MODE_ONE -> R.string.liquid_repeat_one
                        else -> R.string.liquid_repeat_off
                    }
                ),
                tint = if (playback.repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else accent
            )
        }
    }
}

/**
 * Hàng âm lượng. Khi kéo chỉ áp dụng cho trình phát, thả tay mới ghi cài đặt
 * để không ghi MMKV ở mỗi điểm ảnh.
 */
@Composable
fun LiquidVolume(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onVolumeCommit: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(volume) }
    LaunchedEffect(volume, dragging) {
        if (!dragging) draft = volume
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.ic_volume_up),
            contentDescription = stringResource(R.string.liquid_volume)
        )
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            value = draft.coerceIn(0f, 1f),
            valueRange = 0f..1f,
            onValueChange = {
                dragging = true
                draft = it
                onVolumeChange(it)
            },
            onValueChangeFinished = {
                dragging = false
                onVolumeCommit(draft)
            }
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = "${(draft * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/** Một dòng bài hát trong danh sách */
@Composable
fun LiquidSongRow(
    song: LiquidSong,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidArtwork(song = song, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp))
        Column(modifier = Modifier
            .weight(1f)
            .padding(horizontal = 10.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isCurrent) accent else onCardColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (song.artist.isBlank()) song.album else song.artist,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(
                    if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outlined
                ),
                contentDescription = stringResource(R.string.liquid_favorite),
                tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Tiêu đề nhóm dùng chung */
@Composable
fun LiquidSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(top = 4.dp, bottom = 2.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}
