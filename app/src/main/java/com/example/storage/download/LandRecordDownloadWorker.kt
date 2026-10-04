package com.example.storage.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.DocumentType
import com.example.storage.DownloadNotificationHelper
import com.example.storage.SaveResult
import com.example.storage.StorageManager
import java.io.File
import java.io.IOException

/**
 * WorkManager CoroutineWorker for background downloading of large land record PDFs and image files.
 * Provides live progress tracking and persistent foreground notification.
 */
class LandRecordDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "LandRecordDownloadWorker"

        const val KEY_URL = "key_url"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_DIVISION = "key_division"
        const val KEY_DISTRICT = "key_district"
        const val KEY_UPAZILA = "key_upazila"
        const val KEY_MOUZA = "key_mouza"
        const val KEY_DOC_TYPE = "key_doc_type"
        const val KEY_KHATIAN_NO = "key_khatian_no"
        const val KEY_IS_FOREGROUND = "key_is_foreground"

        const val KEY_PROGRESS_PERCENT = "key_progress_percent"
        const val KEY_BYTES_DOWNLOADED = "key_bytes_downloaded"
        const val KEY_TOTAL_BYTES = "key_total_bytes"
        const val KEY_STATUS = "key_status"
        const val KEY_ERROR_MESSAGE = "key_error_message"

        const val KEY_OUTPUT_RECORD_ID = "key_output_record_id"
        const val KEY_OUTPUT_FILE_PATH = "key_output_file_path"
        const val KEY_OUTPUT_SHA256 = "key_output_sha256"
        const val KEY_OUTPUT_FILE_SIZE = "key_output_file_size"

        const val NOTIFICATION_ID_BASE = 5000
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val storageManager = StorageManager(context)
    private val downloadClient = OkHttpDownloadClient()
    private val notificationHelper = DownloadNotificationHelper(context)

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "land_record_${System.currentTimeMillis()}.pdf"
        val division = inputData.getString(KEY_DIVISION) ?: "Dhaka"
        val district = inputData.getString(KEY_DISTRICT) ?: "Dhaka"
        val upazila = inputData.getString(KEY_UPAZILA) ?: "Savar"
        val mouza = inputData.getString(KEY_MOUZA) ?: "All Mouzas"
        val docTypeCode = inputData.getString(KEY_DOC_TYPE) ?: DocumentType.RS.code
        val docType = DocumentType.entries.firstOrNull { it.code == docTypeCode } ?: DocumentType.RS
        val khatianNo = inputData.getString(KEY_KHATIAN_NO) ?: ""
        val runInForeground = inputData.getBoolean(KEY_IS_FOREGROUND, true)

        if (url.isNullOrBlank()) {
            Log.e(TAG, "Download failed: Missing URL")
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "URL must not be empty"))
        }

        val notificationId = NOTIFICATION_ID_BASE + (id.hashCode() and 0x7FFF)

        // Set initial progress
        setProgress(
            workDataOf(
                KEY_STATUS to "CONNECTING",
                KEY_FILE_NAME to fileName,
                KEY_PROGRESS_PERCENT to 0,
                KEY_BYTES_DOWNLOADED to 0L,
                KEY_TOTAL_BYTES to 0L
            )
        )

        if (runInForeground) {
            try {
                val foregroundInfo = createForegroundInfo(notificationId, fileName, 0, 0L, 0L)
                setForeground(foregroundInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set foreground service: ${e.message}")
            }
        }

        val tempDownloadDir = File(context.cacheDir, "workmanager_downloads").apply { if (!exists()) mkdirs() }
        val tempDestFile = File(tempDownloadDir, "${id}_${fileName}")

        return try {
            // Track download streaming with OkHttp
            val downloadResult = downloadClient.downloadToFile(
                url = url,
                targetFile = tempDestFile
            ) { bytesRead, totalBytes, percent ->
                // Publish progress to WorkManager
                setProgress(
                    workDataOf(
                        KEY_STATUS to "DOWNLOADING",
                        KEY_FILE_NAME to fileName,
                        KEY_PROGRESS_PERCENT to percent,
                        KEY_BYTES_DOWNLOADED to bytesRead,
                        KEY_TOTAL_BYTES to totalBytes
                    )
                )

                // Update foreground notification if applicable
                if (runInForeground && percent >= 0) {
                    val notif = createNotification(fileName, percent, bytesRead, totalBytes)
                    notificationManager.notify(notificationId, notif)
                }
            }

            if (downloadResult.isFailure) {
                val ex = downloadResult.exceptionOrNull()
                val errorMsg = ex?.message ?: "Unknown download failure"
                Log.e(TAG, "Download failed for $url: $errorMsg", ex)

                setProgress(
                    workDataOf(
                        KEY_STATUS to "FAILED",
                        KEY_ERROR_MESSAGE to errorMsg
                    )
                )

                if (ex is IOException && runAttemptCount < 3) {
                    return Result.retry()
                }
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to errorMsg))
            }

            val downloadedInfo = downloadResult.getOrThrow()

            // Save and register into Land Archive hierarchy
            setProgress(
                workDataOf(
                    KEY_STATUS to "SAVING",
                    KEY_FILE_NAME to fileName,
                    KEY_PROGRESS_PERCENT to 99,
                    KEY_BYTES_DOWNLOADED to downloadedInfo.fileSize,
                    KEY_TOTAL_BYTES to downloadedInfo.fileSize
                )
            )

            val saveResult = storageManager.registerDownloadedFile(
                downloadedFile = tempDestFile,
                division = division,
                district = district,
                upazila = upazila,
                mouza = mouza,
                docType = docType,
                khatianOrPlotNo = khatianNo,
                rawFileName = fileName,
                sha256Hash = downloadedInfo.sha256,
                sourceUrl = url
            )

            when (saveResult) {
                is SaveResult.Success -> {
                    notificationManager.cancel(notificationId)
                    notificationHelper.showDownloadCompleteNotification(
                        upazilaName = "$upazila / $mouza",
                        savedPath = saveResult.record.filePath,
                        fileCount = 1
                    )

                    val outputData = workDataOf(
                        KEY_STATUS to "SUCCESS",
                        KEY_OUTPUT_RECORD_ID to saveResult.record.id,
                        KEY_OUTPUT_FILE_PATH to saveResult.record.filePath,
                        KEY_OUTPUT_SHA256 to saveResult.record.sha256,
                        KEY_OUTPUT_FILE_SIZE to saveResult.record.fileSize,
                        KEY_PROGRESS_PERCENT to 100
                    )
                    Result.success(outputData)
                }
                is SaveResult.Duplicate -> {
                    notificationManager.cancel(notificationId)
                    val outputData = workDataOf(
                        KEY_STATUS to "SUCCESS",
                        KEY_OUTPUT_RECORD_ID to saveResult.existingRecord.id,
                        KEY_OUTPUT_FILE_PATH to saveResult.existingRecord.filePath,
                        KEY_OUTPUT_SHA256 to saveResult.existingRecord.sha256,
                        KEY_OUTPUT_FILE_SIZE to saveResult.existingRecord.fileSize,
                        KEY_PROGRESS_PERCENT to 100
                    )
                    Result.success(outputData)
                }
                is SaveResult.Error -> {
                    Result.failure(workDataOf(KEY_ERROR_MESSAGE to saveResult.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in LandRecordDownloadWorker", e)
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.localizedMessage ?: "Unknown error")))
        } finally {
            if (tempDestFile.exists()) {
                tempDestFile.delete()
            }
        }
    }

    private fun createForegroundInfo(
        notificationId: Int,
        fileName: String,
        percent: Int,
        bytesDownloaded: Long,
        totalBytes: Long
    ): ForegroundInfo {
        val notification = createNotification(fileName, percent, bytesDownloaded, totalBytes)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun createNotification(
        fileName: String,
        percent: Int,
        bytesDownloaded: Long,
        totalBytes: Long
    ): android.app.Notification {
        val title = "Downloading Land Record / খতিয়ান ডাউনলোড"
        val formattedDownloaded = formatFileSize(bytesDownloaded)
        val formattedTotal = if (totalBytes > 0) formatFileSize(totalBytes) else "..."
        val content = if (percent >= 0) {
            "$fileName • $percent% ($formattedDownloaded of $formattedTotal)"
        } else {
            "$fileName • $formattedDownloaded downloaded"
        }

        return NotificationCompat.Builder(context, DownloadNotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, percent.coerceIn(0, 100), percent < 0)
            .build()
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        return "%.1f MB".format(mb)
    }
}
