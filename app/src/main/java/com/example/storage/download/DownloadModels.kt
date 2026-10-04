package com.example.storage.download

import com.example.data.DocumentType
import com.example.data.LandRecord
import java.util.UUID

/**
 * Encapsulates a download request for a land record document.
 */
data class DownloadRequest(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileName: String,
    val division: String,
    val district: String,
    val upazila: String,
    val mouza: String,
    val docType: DocumentType,
    val khatianOrPlotNo: String = "",
    val requiresWifi: Boolean = false,
    val isForeground: Boolean = true
)

/**
 * Status of the download task.
 */
enum class DownloadStatus {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    BLOCKED
}

/**
 * Real-time progress and state of an ongoing or completed download.
 */
data class DownloadProgress(
    val workId: UUID,
    val fileName: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercentage: Int = 0,
    val status: DownloadStatus = DownloadStatus.ENQUEUED,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val savedRecordId: String? = null,
    val savedFilePath: String? = null,
    val sha256: String? = null
)

/**
 * Result metadata from OkHttp streaming.
 */
data class DownloadedFileInfo(
    val fileSize: Long,
    val sha256: String,
    val mimeType: String
)

/**
 * Outcome of a direct or worker download.
 */
sealed class DownloadResult {
    data class Success(val record: LandRecord, val bytes: Long, val sha256: String) : DownloadResult()
    data class Failure(val error: String, val throwable: Throwable? = null) : DownloadResult()
    object Cancelled : DownloadResult()
}
