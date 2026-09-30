package com.example.storage

import android.content.Context
import android.util.Log
import com.example.data.LandRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

data class CacheClearResult(
    val clearedFilesCount: Int,
    val clearedBytes: Long,
    val success: Boolean,
    val details: String
)

class SecureCacheCleaner(
    private val context: Context,
    private val storageManager: StorageManager
) {
    companion object {
        private const val TAG = "SecureCacheCleaner"
    }

    /**
     * Securely clears the internal application temporary cache of the processed files
     * ONLY after the file output stream confirms successful completion.
     */
    suspend fun securelyClearTemporaryCache(
        processedRecords: List<LandRecord>,
        processedZipFile: File? = null,
        outputConfirmed: Boolean = true
    ): CacheClearResult = withContext(Dispatchers.IO) {
        if (!outputConfirmed) {
            Log.w(TAG, "Cache cleanup aborted: Output stream was not confirmed successful.")
            return@withContext CacheClearResult(
                clearedFilesCount = 0,
                clearedBytes = 0L,
                success = false,
                details = "Temporary cache cleanup aborted: Output stream completion was not confirmed."
            )
        }

        if (processedZipFile != null && (!processedZipFile.exists() || processedZipFile.length() <= 0L)) {
            Log.w(TAG, "Cache cleanup aborted: Target ZIP file is missing or empty.")
            return@withContext CacheClearResult(
                clearedFilesCount = 0,
                clearedBytes = 0L,
                success = false,
                details = "Temporary cache cleanup aborted: ZIP output file missing or zero bytes."
            )
        }

        var filesDeleted = 0
        var bytesFreed = 0L

        try {
            // 1. Securely wipe & delete individual processed document files
            for (record in processedRecords) {
                val file = File(record.filePath)
                if (file.exists() && file.isFile) {
                    val size = file.length()
                    if (secureWipeFile(file)) {
                        filesDeleted++
                        bytesFreed += size
                    }
                }
            }

            // 2. Securely wipe & delete temporary exported ZIP from cacheDir/exports
            if (processedZipFile != null && processedZipFile.exists()) {
                val size = processedZipFile.length()
                if (secureWipeFile(processedZipFile)) {
                    filesDeleted++
                    bytesFreed += size
                }
            }

            // 3. Clean any leftover temporary files in cacheDir/exports
            val exportsDir = File(context.cacheDir, "exports")
            if (exportsDir.exists() && exportsDir.isDirectory) {
                exportsDir.listFiles()?.forEach { f ->
                    if (f.isFile) {
                        val size = f.length()
                        if (secureWipeFile(f)) {
                            filesDeleted++
                            bytesFreed += size
                        }
                    }
                }
                exportsDir.delete()
            }

            // 4. Clean empty subdirectories in internal archive
            cleanEmptyDirectories(storageManager.baseArchiveDir)

            // 5. Update StorageManager manifest to remove processed records from internal DB
            val remainingRecords = storageManager.getAllRecords().filterNot { rec ->
                processedRecords.any { it.id == rec.id || it.filePath == rec.filePath }
            }
            storageManager.saveAllRecords(remainingRecords)

            val kbFreed = bytesFreed / 1024
            Log.i(TAG, "Secure cache wipe completed: $filesDeleted files deleted, $kbFreed KB freed.")

            CacheClearResult(
                clearedFilesCount = filesDeleted,
                clearedBytes = bytesFreed,
                success = true,
                details = "Securely wiped $filesDeleted temporary files ($kbFreed KB) from internal application cache."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error securely clearing temporary cache: ${e.message}", e)
            CacheClearResult(
                clearedFilesCount = filesDeleted,
                clearedBytes = bytesFreed,
                success = false,
                details = "Partial cache clean: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Securely overwrites the file bytes with zero buffers before deleting,
     * ensuring sensitive land data cannot be recovered from unallocated storage blocks.
     */
    private fun secureWipeFile(file: File): Boolean {
        return try {
            if (file.length() > 0) {
                RandomAccessFile(file, "rws").use { raf ->
                    raf.seek(0)
                    val buffer = ByteArray(minOf(file.length(), 4096).toInt())
                    var remaining = file.length()
                    while (remaining > 0) {
                        val writeSize = minOf(remaining, buffer.size.toLong()).toInt()
                        raf.write(buffer, 0, writeSize)
                        remaining -= writeSize
                    }
                }
            }
            file.delete()
        } catch (e: Exception) {
            file.delete()
        }
    }

    private fun cleanEmptyDirectories(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.walkBottomUp()
            .filter { it.isDirectory && it != dir }
            .forEach { child ->
                if (child.listFiles()?.isEmpty() == true) {
                    child.delete()
                }
            }
    }
}
