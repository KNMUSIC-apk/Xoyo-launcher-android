/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.music

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.movtery.zalithlauncher.setting.LiquidIslandSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/** Trạng thái phát nhạc mà giao diện cần */
@Stable
data class LiquidPlayback(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val song: LiquidSong? = null,
    val index: Int = 0,
    val count: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

/**
 * Cầu nối duy nhất tới [LiquidIslandService]. Mỗi tiến trình giữ đúng một MediaController
 * (launcher và `:game`), không tự tạo ExoPlayer ở bất kỳ đâu khác.
 */
object LiquidIsland {
    private var future: ListenableFuture<MediaController>? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private val controllerFlow = MutableStateFlow<MediaController?>(null)

    val controller: StateFlow<MediaController?> = controllerFlow

    /** Công tắc tổng và công tắc trình phát đều phải bật */
    fun isEnabled(): Boolean =
        LiquidIslandSettings.enabled.getValue() && LiquidIslandSettings.playerEnabled.getValue()

    fun withController(context: Context, action: (MediaController) -> Unit) {
        val current = controllerFlow.value
        if (current != null) {
            action(current)
            return
        }
        pending += action
        connect(context)
    }

    private fun connect(context: Context) {
        if (future != null) return
        val appContext = context.applicationContext
        val token = SessionToken(appContext, ComponentName(appContext, LiquidIslandService::class.java))
        val controllerFuture = MediaController.Builder(appContext, token).buildAsync()
        future = controllerFuture
        controllerFuture.addListener(
            {
                val controller = runCatching { controllerFuture.get() }.getOrNull()
                if (controller == null) {
                    future = null
                } else {
                    controller.volume = LiquidIslandSettings.volume.getValue() / 100f
                    controllerFlow.value = controller
                    pending.forEach { action -> runCatching { action(controller) } }
                }
                pending.clear()
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    /** Phát [songs] từ [startIndex]; toàn bộ danh sách thành hàng đợi của session */
    fun play(context: Context, songs: List<LiquidSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        withController(context) { controller ->
            controller.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), 0L)
            controller.prepare()
            controller.play()
        }
    }

    fun playOrToggle(context: Context) = withController(context) { controller ->
        when {
            controller.isPlaying -> controller.pause()
            controller.playbackState == Player.STATE_IDLE -> {
                controller.prepare()
                controller.play()
            }
            else -> controller.play()
        }
    }

    fun next(context: Context) = withController(context) { it.seekToNextMediaItem() }
    fun previous(context: Context) = withController(context) { it.seekToPreviousMediaItem() }
    fun seekTo(context: Context, positionMs: Long) = withController(context) { it.seekTo(positionMs) }

    fun toggleShuffle(context: Context) = withController(context) { controller ->
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun cycleRepeat(context: Context) = withController(context) { controller ->
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Đổi âm lượng ngay (không ghi cài đặt) */
    fun applyVolume(context: Context, volume: Float) = withController(context) {
        it.volume = volume.coerceIn(0f, 1f)
    }

    /** Ghi âm lượng vào cài đặt khi người dùng thả tay */
    fun saveVolume(volume: Float) {
        LiquidIslandSettings.volume.save((volume.coerceIn(0f, 1f) * 100).roundToInt())
    }

    /** Hàng đợi hiện tại, dùng để lưu thành danh sách phát */
    fun currentQueue(): List<LiquidSong> {
        val controller = controllerFlow.value ?: return emptyList()
        return (0 until controller.mediaItemCount).mapNotNull { index ->
            controller.getMediaItemAt(index).let { item -> LiquidSong.from(item, item.durationMs) }
        }
    }
}

/**
 * Đọc trạng thái phát theo nhịp khi đang hiển thị.
 * MediaController nằm ở tiến trình khác nên không có StateFlow cục bộ để quan sát.
 */
@Composable
fun rememberLiquidPlayback(enabled: Boolean = true, pollMillis: Long = 500L): LiquidPlayback {
    val context = LocalContext.current
    var playback by remember { mutableStateOf(LiquidPlayback()) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            playback = LiquidPlayback()
            return@LaunchedEffect
        }
        LiquidIsland.withController(context) { }
        while (true) {
            playback = LiquidIsland.controller.value?.readPlayback() ?: LiquidPlayback()
            delay(pollMillis)
        }
    }
    return playback
}

private fun MediaController.readPlayback(): LiquidPlayback {
    val item = currentMediaItem
    val knownDuration = duration.takeIf { it > 0L }
        ?: item?.durationMs?.takeIf { it > 0L }
        ?: 0L
    return LiquidPlayback(
        connected = true,
        isPlaying = isPlaying,
        song = item?.let { LiquidSong.from(it, knownDuration) },
        index = currentMediaItemIndex,
        count = mediaItemCount,
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = knownDuration,
        volume = volume,
        shuffle = shuffleModeEnabled,
        repeatMode = repeatMode,
        hasNext = nextMediaItemIndex != C.INDEX_UNSET,
        hasPrevious = previousMediaItemIndex != C.INDEX_UNSET
    )
}
