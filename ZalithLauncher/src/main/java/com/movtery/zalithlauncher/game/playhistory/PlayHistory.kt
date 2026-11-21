/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.playhistory

import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.GSON
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Lịch sử chơi của một phiên bản.
 * @param lastPlayedAt thời điểm (ms, epoch) lần chơi gần nhất được bắt đầu
 * @param totalPlayMs tổng thời gian đã chơi (ms), chỉ cộng khi phiên kết thúc được ghi nhận
 * @param sessions số lần đã khởi chạy
 */
data class PlayRecord(
    val versionName: String,
    val lastPlayedAt: Long = 0L,
    val totalPlayMs: Long = 0L,
    val sessions: Int = 0
)

/** Thuần: ghi nhận một lần khởi chạy */
fun Map<String, PlayRecord>.withSessionStarted(versionName: String, startedAt: Long): Map<String, PlayRecord> {
    val old = this[versionName] ?: PlayRecord(versionName)
    return this + (versionName to old.copy(lastPlayedAt = startedAt, sessions = old.sessions + 1))
}

/** Thuần: cộng thời gian của một phiên đã kết thúc. Thời gian âm (đồng hồ lùi) bị bỏ qua. */
fun Map<String, PlayRecord>.withSessionEnded(versionName: String, startedAt: Long, endedAt: Long): Map<String, PlayRecord> {
    val old = this[versionName] ?: PlayRecord(versionName)
    val elapsed = (endedAt - startedAt).coerceAtLeast(0L)
    return this + (versionName to old.copy(totalPlayMs = old.totalPlayMs + elapsed))
}

/**
 * Lưu lịch sử chơi vào bộ nhớ riêng của ứng dụng.
 * Ghi file chạy trên luồng IO. Không chặn luồng giao diện.
 */
object PlayHistory {
    private const val FILE_NAME = "play_history.json"
    private const val CORRUPT_FILE_NAME = "play_history.corrupt.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileLock = Mutex()

    private val _records = MutableStateFlow<Map<String, PlayRecord>>(emptyMap())
    /** Lịch sử chơi, khoá là tên phiên bản */
    val records: StateFlow<Map<String, PlayRecord>> = _records.asStateFlow()

    @Volatile private var requested = false
    /** Chỉ ghi file sau khi đã đọc xong file cũ, để không ghi đè lịch sử cũ bằng dữ liệu rỗng */
    @Volatile private var ready = false
    @Volatile private var activeVersion: String? = null
    @Volatile private var activeStartedAt: Long = 0L

    private fun file(name: String = FILE_NAME): File = File(PathManager.DIR_FILES_PRIVATE, name)

    /** Đọc file lịch sử ở nền. Chỉ thực hiện một lần. */
    fun ensureLoaded() {
        if (requested) return
        requested = true
        scope.launch {
            fileLock.withLock {
                val source = file()
                if (source.isFile) {
                    val loaded = runCatching {
                        GSON.fromJson(source.readText(), Array<PlayRecord>::class.java)?.toList().orEmpty()
                    }.getOrElse {
                        // File hỏng: giữ lại bản gốc để người dùng còn có thể xem, không xoá
                        runCatching { source.copyTo(file(CORRUPT_FILE_NAME), overwrite = true) }
                        emptyList()
                    }
                    val fromDisk = loaded.associateBy { it.versionName }
                    _records.update { current -> fromDisk + current }
                }
                ready = true
            }
        }
    }

    /** Gọi khi game bắt đầu chạy */
    fun onSessionStarted(versionName: String, nowMs: Long = System.currentTimeMillis()) {
        ensureLoaded()
        activeVersion = versionName
        activeStartedAt = nowMs
        _records.update { it.withSessionStarted(versionName, nowMs) }
        persist()
    }

    /** Gọi khi game thoát. Gọi nhiều lần cũng an toàn: chỉ phiên đang chạy mới được ghi. */
    fun onSessionEnded(nowMs: Long = System.currentTimeMillis()) {
        val name = activeVersion ?: return
        val start = activeStartedAt
        activeVersion = null
        _records.update { it.withSessionEnded(name, start, nowMs) }
        persist()
    }

    private fun persist() {
        if (!ready) return
        val snapshot = _records.value.values.toList()
        scope.launch {
            fileLock.withLock {
                runCatching {
                    val target = file()
                    target.parentFile?.mkdirs()
                    target.writeText(GSON.toJson(snapshot))
                }
            }
        }
    }
}
