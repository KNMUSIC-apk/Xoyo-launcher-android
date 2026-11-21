/*
 * XOYO Launcher (bản chỉnh sửa của ZalithLauncher2, GPL-3.0; xem NOTICE.md)
 */

package com.movtery.zalithlauncher.game.record

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.notification.NOTIFICATION_ID_SCREEN_RECORD
import com.movtery.zalithlauncher.notification.NotificationChannelData
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ScreenRecordService"
private const val ACTION_START = "com.movtery.zalithlauncher.record.START"
private const val ACTION_STOP = "com.movtery.zalithlauncher.record.STOP"
private const val EXTRA_RESULT_CODE = "record_result_code"
private const val EXTRA_PROJECTION_DATA = "record_projection_data"
private const val RECORD_FOLDER = "XOYO"

/** 30 FPS để việc ghi hình ít ảnh hưởng đến FPS của game */
private const val TARGET_FPS = 30

/**
 * Ghi hình màn hình bằng MediaProjection và MediaRecorder (chỉ hình, không có âm thanh game).
 * Chạy như dịch vụ foreground kiểu mediaProjection để Android 14 trở lên cho phép.
 *
 * Video được lưu vào Movies/XOYO trong bộ nhớ chung (Android 10 trở lên), hoặc thư mục riêng của ứng dụng trên Android cũ hơn.
 */
class ScreenRecordService : Service() {
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var pendingUri: Uri? = null
    private var legacyFile: File? = null
    private var savedLabel: String? = null
    private var active = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        when (intent.action) {
            ACTION_START -> {
                if (!enterForeground()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (active) return START_NOT_STICKY
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.projectionData()
                if (resultCode != Activity.RESULT_OK || data == null) {
                    abortRecording()
                } else {
                    startRecording(resultCode, data)
                }
            }
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun enterForeground(): Boolean {
        val notification = buildNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID_SCREEN_RECORD,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID_SCREEN_RECORD, notification)
            }
            true
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to enter foreground for screen recording", e)
            false
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenRecordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationChannelData.SCREEN_RECORD_CHANNEL.channelId)
            .setContentTitle(getString(R.string.record_notification_title))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(0, getString(R.string.record_notification_stop), stopIntent)
            .build()
    }

    private fun startRecording(resultCode: Int, data: Intent) {
        try {
            val metrics = resources.displayMetrics
            val (width, height) = recordSizeFor(metrics.widthPixels, metrics.heightPixels)
            val bitrate = recordBitrateFor(width, height, TARGET_FPS)

            val manager = getSystemService(MediaProjectionManager::class.java)
                ?: throw IOException("MediaProjectionManager unavailable")
            val proj = manager.getMediaProjection(resultCode, data)
                ?: throw IOException("Media projection was not granted")
            projection = proj
            // Android 14 yêu cầu đăng ký callback trước khi tạo VirtualDisplay.
            // registerCallback chỉ có từ Android 10, nên bản cũ hơn bỏ qua callback này.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val callback = object : MediaProjection.Callback() {
                    override fun onStop() {
                        // Người dùng bấm "dừng chia sẻ" trên hệ thống
                        stopRecording()
                    }
                }
                projectionCallback = callback
                proj.registerCallback(callback, mainHandler)
            }

            val rec = createRecorder()
            recorder = rec
            rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            rec.setVideoSize(width, height)
            rec.setVideoFrameRate(TARGET_FPS)
            rec.setVideoEncodingBitRate(bitrate)
            setupOutput(rec, newFileName())
            rec.prepare()

            virtualDisplay = proj.createVirtualDisplay(
                "XoyoScreenRecord",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                rec.surface,
                null,
                null
            )
            rec.start()
            active = true
            ScreenRecordController.setRecording(true)
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to start screen recording", e)
            abortRecording()
        }
    }

    /** Dừng hẳn sau khi không bắt đầu được, bỏ file vừa tạo */
    private fun abortRecording() {
        releaseAll()
        finishOutput(saved = false)
        ScreenRecordController.setRecording(false)
        Toast.makeText(this, R.string.record_failed, Toast.LENGTH_LONG).show()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRecording() {
        if (!active) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        active = false

        var saved = true
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // stop() ném lỗi khi bản ghi quá ngắn hoặc không có khung hình nào
            Logger.error(TAG, "Recorder stopped with an error", e)
            saved = false
        }
        releaseAll()
        finishOutput(saved)
        ScreenRecordController.setRecording(false)

        val label = savedLabel
        if (saved && label != null) {
            Toast.makeText(this, getString(R.string.record_saved, label), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.record_stop_failed, Toast.LENGTH_LONG).show()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()

    private fun newFileName(): String =
        "xoyo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())}.mp4"

    /** Gắn đích ghi cho bộ ghi hình: bộ nhớ chung trên Android 10 trở lên, thư mục riêng của ứng dụng trên bản cũ hơn */
    private fun setupOutput(rec: MediaRecorder, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/$RECORD_FOLDER")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Cannot create the video entry")
            pendingUri = uri
            val pfd = contentResolver.openFileDescriptor(uri, "rw")
                ?: throw IOException("Cannot open the video file")
            descriptor = pfd
            rec.setOutputFile(pfd.fileDescriptor)
            savedLabel = "Movies/$RECORD_FOLDER/$fileName"
        } else {
            val base = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
            val dir = File(base, RECORD_FOLDER).apply { mkdirs() }
            val file = File(dir, fileName)
            legacyFile = file
            rec.setOutputFile(file.absolutePath)
            savedLabel = file.absolutePath
        }
    }

    /** Đóng toàn bộ tài nguyên ghi hình. Gọi được nhiều lần. */
    private fun releaseAll() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        val proj = projection
        val callback = projectionCallback
        if (proj != null && callback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { proj.unregisterCallback(callback) }
        }
        projectionCallback = null
        runCatching { proj?.stop() }
        projection = null
        runCatching { descriptor?.close() }
        descriptor = null
    }

    /** Hoàn tất hoặc bỏ file đầu ra sau khi ghi */
    private fun finishOutput(saved: Boolean) {
        val uri = pendingUri
        pendingUri = null
        if (uri != null) {
            if (saved) {
                val values = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                runCatching { contentResolver.update(uri, values, null, null) }
            } else {
                runCatching { contentResolver.delete(uri, null, null) }
            }
        }
        legacyFile?.let { file ->
            if (!saved) file.delete()
        }
        legacyFile = null
    }

    override fun onDestroy() {
        if (active) {
            active = false
            releaseAll()
            finishOutput(saved = false)
            ScreenRecordController.setRecording(false)
        }
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun Intent.projectionData(): Intent? =
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            getParcelableExtra(EXTRA_PROJECTION_DATA)
        }

    companion object {
        /** Bắt đầu ghi hình bằng kết quả cấp quyền của hệ thống */
        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenRecordService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_PROJECTION_DATA, data)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Dừng ghi hình và lưu file */
        fun stop(context: Context) {
            context.startService(Intent(context, ScreenRecordService::class.java).setAction(ACTION_STOP))
        }
    }
}
