package com.example.storage.download

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.LandRecord
import com.example.storage.SaveResult
import com.example.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Service managing background and immediate downloads of large land record PDFs and image files
 * using OkHttp and WorkManager with real-time progress tracking.
 */
class LandRecordDownloadService(private val context: Context) {

    companion object {
        const val TAG_LAND_RECORD_DOWNLOAD = "tag_land_record_download"
        private const val TAG = "LandRecordDownloadService"

        @Volatile
        private var instance: LandRecordDownloadService? = null

        fun getInstance(context: Context): LandRecordDownloadService {
            return instance ?: synchronized(this) {
                instance ?: LandRecordDownloadService(context.applicationContext).also { instance = it }
            }
        }

        fun resetInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)
    private val okHttpClient = OkHttpDownloadClient()
    private val storageManager = StorageManager(context)

    /**
     * Enqueues a background download task managed by WorkManager.
     * Guaranteed to execute and survive app restarts or process termination.
     */
    fun enqueueDownload(request: DownloadRequest): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (request.requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()

        val inputData = workDataOf(
            LandRecordDownloadWorker.KEY_URL to request.url,
            LandRecordDownloadWorker.KEY_FILE_NAME to request.fileName,
            LandRecordDownloadWorker.KEY_DIVISION to request.division,
            LandRecordDownloadWorker.KEY_DISTRICT to request.district,
            LandRecordDownloadWorker.KEY_UPAZILA to request.upazila,
            LandRecordDownloadWorker.KEY_MOUZA to request.mouza,
            LandRecordDownloadWorker.KEY_DOC_TYPE to request.docType.code,
            LandRecordDownloadWorker.KEY_KHATIAN_NO to request.khatianOrPlotNo,
            LandRecordDownloadWorker.KEY_IS_FOREGROUND to request.isForeground
        )

        val workRequest = OneTimeWorkRequestBuilder<LandRecordDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(TAG_LAND_RECORD_DOWNLOAD)
            .addTag("doc_${request.docType.code}")
            .addTag("upz_${request.upazila}")
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        val uniqueWorkName = "download_${request.upazila}_${request.fileName.hashCode()}"
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Enqueued download work ${workRequest.id} for ${request.fileName}")
        return workRequest.id
    }

    /**
     * Enqueues a batch of download requests.
     */
    fun enqueueBatch(requests: List<DownloadRequest>): List<UUID> {
        return requests.map { enqueueDownload(it) }
    }

    /**
     * Observes real-time progress for a specific download task.
     */
    fun observeDownloadProgress(workId: UUID): Flow<DownloadProgress> {
        return workManager.getWorkInfoByIdFlow(workId).map { workInfo ->
            if (workInfo == null) {
                DownloadProgress(
                    workId = workId,
                    fileName = "",
                    status = DownloadStatus.FAILED,
                    errorMessage = "Work not found"
                )
            } else {
                mapWorkInfoToProgress(workInfo)
            }
        }
    }

    /**
     * Observes all active and recent land record downloads.
     */
    fun observeAllDownloads(): Flow<List<DownloadProgress>> {
        return workManager.getWorkInfosByTagFlow(TAG_LAND_RECORD_DOWNLOAD).map { list ->
            list.map { mapWorkInfoToProgress(it) }
        }
    }

    /**
     * Cancels an active or queued download.
     */
    fun cancelDownload(workId: UUID) {
        workManager.cancelWorkById(workId)
        Log.d(TAG, "Cancelled download work $workId")
    }

    /**
     * Cancels all scheduled or active land record downloads.
     */
    fun cancelAllDownloads() {
        workManager.cancelAllWorkByTag(TAG_LAND_RECORD_DOWNLOAD)
        Log.d(TAG, "Cancelled all download works")
    }

    /**
     * Performs a direct download inside a coroutine using OkHttp, bypassing WorkManager.
     * Useful for synchronous or in-app foreground transfers with immediate progress callbacks.
     */
    suspend fun downloadDirect(
        request: DownloadRequest,
        onProgress: (suspend (bytesRead: Long, totalBytes: Long, percentage: Int) -> Unit)? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        val tempDownloadDir = File(context.cacheDir, "direct_downloads").apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDownloadDir, "${UUID.randomUUID()}_${request.fileName}")

        try {
            val result = okHttpClient.downloadToFile(
                url = request.url,
                targetFile = tempFile,
                onProgress = onProgress
            )

            if (result.isFailure) {
                val ex = result.exceptionOrNull()
                return@withContext DownloadResult.Failure(
                    ex?.message ?: "Direct download failed",
                    ex
                )
            }

            val fileInfo = result.getOrThrow()
            val saveResult = storageManager.registerDownloadedFile(
                downloadedFile = tempFile,
                division = request.division,
                district = request.district,
                upazila = request.upazila,
                mouza = request.mouza,
                docType = request.docType,
                khatianOrPlotNo = request.khatianOrPlotNo,
                rawFileName = request.fileName,
                sha256Hash = fileInfo.sha256,
                sourceUrl = request.url
            )

            when (saveResult) {
                is SaveResult.Success -> DownloadResult.Success(
                    record = saveResult.record,
                    bytes = fileInfo.fileSize,
                    sha256 = fileInfo.sha256
                )
                is SaveResult.Duplicate -> DownloadResult.Success(
                    record = saveResult.existingRecord,
                    bytes = fileInfo.fileSize,
                    sha256 = fileInfo.sha256
                )
                is SaveResult.Error -> DownloadResult.Failure(saveResult.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in direct download", e)
            DownloadResult.Failure(e.localizedMessage ?: "Unknown download error", e)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun mapWorkInfoToProgress(workInfo: WorkInfo): DownloadProgress {
        val status = when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> DownloadStatus.ENQUEUED
            WorkInfo.State.RUNNING -> DownloadStatus.RUNNING
            WorkInfo.State.SUCCEEDED -> DownloadStatus.SUCCEEDED
            WorkInfo.State.FAILED -> DownloadStatus.FAILED
            WorkInfo.State.BLOCKED -> DownloadStatus.BLOCKED
            WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
        }

        val progressData = workInfo.progress
        val outputData = workInfo.outputData

        val fileName = progressData.getString(LandRecordDownloadWorker.KEY_FILE_NAME)
            ?: outputData.getString(LandRecordDownloadWorker.KEY_FILE_NAME)
            ?: "Land Record"

        val percent = if (workInfo.state == WorkInfo.State.SUCCEEDED) {
            100
        } else {
            progressData.getInt(LandRecordDownloadWorker.KEY_PROGRESS_PERCENT, 0)
        }

        val bytesDownloaded = progressData.getLong(LandRecordDownloadWorker.KEY_BYTES_DOWNLOADED, 0L)
        val totalBytes = progressData.getLong(LandRecordDownloadWorker.KEY_TOTAL_BYTES, 0L)
        val errorMessage = outputData.getString(LandRecordDownloadWorker.KEY_ERROR_MESSAGE)
            ?: progressData.getString(LandRecordDownloadWorker.KEY_ERROR_MESSAGE)

        val savedRecordId = outputData.getString(LandRecordDownloadWorker.KEY_OUTPUT_RECORD_ID)
        val savedFilePath = outputData.getString(LandRecordDownloadWorker.KEY_OUTPUT_FILE_PATH)
        val sha256 = outputData.getString(LandRecordDownloadWorker.KEY_OUTPUT_SHA256)

        return DownloadProgress(
            workId = workInfo.id,
            fileName = fileName,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            progressPercentage = percent,
            status = status,
            errorMessage = errorMessage,
            savedRecordId = savedRecordId,
            savedFilePath = savedFilePath,
            sha256 = sha256
        )
    }
}
