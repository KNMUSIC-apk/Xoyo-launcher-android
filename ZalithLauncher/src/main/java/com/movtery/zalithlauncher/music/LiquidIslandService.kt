/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.music

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.movtery.zalithlauncher.setting.LiquidIslandSettings

/**
 * Dịch vụ phát nhạc duy nhất của Liquid Island.
 *
 * Chạy ở tiến trình chính của launcher, không phải tiến trình `:game`, nên nhạc vẫn phát
 * khi Minecraft đang chạy và khi người chơi mở bảng điều khiển trong game.
 * Chỉ có một ExoPlayer cho cả ứng dụng; nơi khác nói chuyện qua MediaController.
 */
@OptIn(UnstableApi::class)
class LiquidIslandService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    /** Ghi lại bài vừa phát để mục "Vừa phát" có dữ liệu */
    private val recentListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.toLongOrNull()?.let { LiquidLibrary.addRecent(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!LiquidIslandSettings.enabled.getValue() || !LiquidIslandSettings.playerEnabled.getValue()) {
            stopSelf()
            return
        }

        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            // true: ExoPlayer tự xin/nhả audio focus và tự giảm âm lượng khi có thông báo khác
            setAudioAttributes(attributes, true)
            setHandleAudioBecomingNoisy(true)
            volume = LiquidIslandSettings.volume.getValue() / 100f
            addListener(recentListener)
        }
        player = exoPlayer
        session = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val exoPlayer = player
        // Tắt "phát nền" thì dừng hẳn khi launcher bị vuốt khỏi danh sách ứng dụng gần đây
        if (exoPlayer == null || !exoPlayer.playWhenReady ||
            !LiquidIslandSettings.backgroundPlayback.getValue()
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.let { current ->
            current.player.release()
            current.release()
        }
        session = null
        player = null
        super.onDestroy()
    }
}
