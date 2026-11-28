/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Một bài hát trong thư viện Liquid Island.
 * Mọi thông tin hiển thị được nhét vào [MediaMetadata] để tiến trình game
 * (chỉ nói chuyện với dịch vụ qua MediaController) vẫn đọc lại được.
 */
data class LiquidSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String? = null
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let(Uri::parse))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()

    companion object {
        /** Dựng lại bài hát từ MediaItem lấy qua MediaController (khác tiến trình) */
        fun from(item: MediaItem, durationMs: Long): LiquidSong? {
            val id = item.mediaId.toLongOrNull() ?: return null
            val metadata = item.mediaMetadata
            return LiquidSong(
                id = id,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(),
                durationMs = durationMs,
                uri = item.localConfiguration?.uri?.toString().orEmpty(),
                artworkUri = metadata.artworkUri?.toString()
            )
        }
    }
}
