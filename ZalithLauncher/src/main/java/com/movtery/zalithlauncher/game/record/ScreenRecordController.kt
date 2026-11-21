/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.record

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Trạng thái ghi hình màn hình trong tiến trình game.
 * [ScreenRecordService] cập nhật giá trị này, giao diện game đọc để hiển thị nút bật/tắt.
 */
object ScreenRecordController {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    internal fun setRecording(value: Boolean) {
        _isRecording.value = value
    }
}
