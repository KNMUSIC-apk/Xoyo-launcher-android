/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.music

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.setting.LiquidIslandSettings
import com.movtery.zalithlauncher.setting.LiquidIslandStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Danh sách phát do người dùng tạo, lưu id bài hát */
data class LiquidPlaylist(val name: String, val songIds: List<Long>)

/**
 * Thư viện nhạc cục bộ: quét MediaStore, tìm kiếm, yêu thích, vừa phát và danh sách phát.
 * Không giữ ExoPlayer, chỉ đọc dữ liệu — cả hai tiến trình đều dùng được.
 */
object LiquidLibrary {
    private const val RECENT_LIMIT = 40
    private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

    /** Có quyền đọc nhạc trên máy chưa */
    fun canReadAudio(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        ) {
            return true
        }
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /** Quét toàn bộ nhạc trên máy (hoặc trong thư mục đã chọn) */
    suspend fun scan(context: Context): List<LiquidSong> = withContext(Dispatchers.IO) {
        if (!canReadAudio(context)) return@withContext emptyList()

        val minDurationMs = LiquidIslandSettings.minDurationSeconds.getValue() * 1000L
        val folderPrefix = folderPrefix(LiquidIslandSettings.libraryFolder.getValue())

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val args = mutableListOf(minDurationMs.toString())
        if (folderPrefix != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projection += MediaStore.Audio.Media.RELATIVE_PATH
                selection += " AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
            } else {
                projection += MediaStore.Audio.Media.DATA
                selection += " AND ${MediaStore.Audio.Media.DATA} LIKE ?"
            }
            args += "%$folderPrefix%"
        }

        val songs = mutableListOf<LiquidSong>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                args.toTypedArray(),
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    songs += LiquidSong(
                        id = id,
                        title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() } ?: "Unknown",
                        artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "",
                        album = cursor.getString(albumColumn).orEmpty(),
                        durationMs = cursor.getLong(durationColumn),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                        artworkUri = if (albumId > 0) {
                            ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                        } else null
                    )
                }
            }
        }
        songs
    }

    /**
     * Tiền tố đường dẫn tương đối của thư mục đã chọn.
     * Tree URI của SAF có dạng `.../tree/primary%3AMusic`, docId là `primary:Music`.
     */
    private fun folderPrefix(treeUri: String): String? {
        if (treeUri.isBlank()) return null
        val docId = runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(treeUri)) }.getOrNull()
            ?: return null
        return docId.substringAfter(':', "").trim().trim('/').ifBlank { null }
    }

    fun search(songs: List<LiquidSong>, query: String): List<LiquidSong> {
        val keyword = query.trim()
        if (keyword.isEmpty()) return songs
        return songs.filter {
            it.title.contains(keyword, ignoreCase = true) ||
                    it.artist.contains(keyword, ignoreCase = true) ||
                    it.album.contains(keyword, ignoreCase = true)
        }
    }

    fun favorites(): Set<Long> =
        LiquidIslandStore.favorites.getValue().mapNotNull { it.toLongOrNull() }.toSet()

    fun toggleFavorite(id: Long): Set<Long> {
        val current = LiquidIslandStore.favorites.getValue().toMutableList()
        if (!current.remove(id.toString())) current.add(0, id.toString())
        LiquidIslandStore.favorites.save(current)
        return current.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun recentIds(): List<Long> = LiquidIslandStore.recent.getValue().mapNotNull { it.toLongOrNull() }

    fun addRecent(id: Long) {
        val current = LiquidIslandStore.recent.getValue().toMutableList()
        current.remove(id.toString())
        current.add(0, id.toString())
        LiquidIslandStore.recent.save(current.take(RECENT_LIMIT))
    }

    /** Giữ thứ tự của [ids] nhưng chỉ lấy bài hát có trong thư viện */
    fun pick(songs: List<LiquidSong>, ids: Collection<Long>): List<LiquidSong> {
        val byId = songs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    fun playlists(): List<LiquidPlaylist> = LiquidIslandStore.playlists.getValue().mapNotNull { raw ->
        val name = raw.substringBefore('|')
        if (name.isBlank()) return@mapNotNull null
        LiquidPlaylist(
            name = name,
            songIds = raw.substringAfter('|', "").split(',').mapNotNull { it.trim().toLongOrNull() }
        )
    }

    fun savePlaylist(name: String, ids: List<Long>) {
        val rest = LiquidIslandStore.playlists.getValue().filterNot { it.substringBefore('|') == name }
        LiquidIslandStore.playlists.save(rest + "$name|${ids.joinToString(",")}")
    }

    fun deletePlaylist(name: String) {
        LiquidIslandStore.playlists.save(
            LiquidIslandStore.playlists.getValue().filterNot { it.substringBefore('|') == name }
        )
    }
}
