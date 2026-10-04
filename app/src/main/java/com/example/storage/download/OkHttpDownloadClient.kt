package com.example.storage.download

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Handles resilient streaming downloads of large PDF documents and image files
 * using OkHttp, ensuring low memory footprint and calculating SHA-256 hashes on the fly.
 */
class OkHttpDownloadClient(
    private val client: OkHttpClient = createDefaultClient()
) {

    companion object {
        private const val TAG = "OkHttpDownloadClient"
        private const val BUFFER_SIZE = 32 * 1024 // 32 KB chunk size
        private const val MIN_PROGRESS_INTERVAL_MS = 150L // Throttle progress emissions

        fun createDefaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    /**
     * Downloads a file from [url] directly to [targetFile] in chunks.
     * Computes the SHA-256 checksum during streaming and emits progress callbacks.
     */
    suspend fun downloadToFile(
        url: String,
        targetFile: File,
        headers: Map<String, String> = emptyMap(),
        onProgress: (suspend (bytesRead: Long, totalBytes: Long, percentage: Int) -> Unit)? = null
    ): Result<DownloadedFileInfo> = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "LandArchiveBD/1.0 (Android Land Records Engine)")
            .header("Accept", "application/pdf, image/png, image/jpeg, image/tiff, */*")

        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        val request = requestBuilder.build()

        targetFile.parentFile?.mkdirs()
        // Use a temporary file during download to avoid partial files on interruption
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.downloading")
        if (tempFile.exists()) tempFile.delete()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP download failed: Code ${response.code} ${response.message}")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(IOException("Empty response body from server"))

            val totalBytes = body.contentLength()
            val contentType = body.contentType()?.toString() ?: "application/octet-stream"

            val digest = MessageDigest.getInstance("SHA-256")
            var bytesReadTotal = 0L
            var lastProgressTime = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!currentCoroutineContext().isActive) {
                            tempFile.delete()
                            return@withContext Result.failure(IOException("Download cancelled by user or system"))
                        }

                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        bytesReadTotal += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastProgressTime >= MIN_PROGRESS_INTERVAL_MS || bytesReadTotal == totalBytes) {
                            lastProgressTime = now
                            val percent = if (totalBytes > 0) {
                                ((bytesReadTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            onProgress?.invoke(bytesReadTotal, totalBytes, percent)
                        }
                    }
                    output.flush()
                }
            }

            // Ensure 100% completion callback
            onProgress?.invoke(bytesReadTotal, totalBytes, 100)

            // Atomic rename to final target file
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if rename fails
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            Log.d(TAG, "Download finished: ${targetFile.name} ($bytesReadTotal bytes, SHA: $sha256)")

            Result.success(
                DownloadedFileInfo(
                    fileSize = bytesReadTotal,
                    sha256 = sha256,
                    mimeType = contentType
                )
            )
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Log.e(TAG, "Error downloading from $url", e)
            Result.failure(e)
        }
    }
}
