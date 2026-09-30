package com.example.storage

import android.content.Context
import android.util.Log
import com.example.data.DocumentType
import com.example.data.LandRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

sealed class SaveResult {
    data class Success(val record: LandRecord) : SaveResult()
    data class Duplicate(val existingRecord: LandRecord) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class StorageManager(private val context: Context) {
    private val gson = Gson()
    val baseArchiveDir: File
        get() = File(context.filesDir, "LandArchive").apply { if (!exists()) mkdirs() }

    private val manifestFile: File
        get() = File(baseArchiveDir, "records_manifest.json")

    @Synchronized
    fun getAllRecords(): List<LandRecord> {
        if (!manifestFile.exists()) return emptyList()
        return try {
            val json = manifestFile.readText()
            val type = object : TypeToken<List<LandRecord>>() {}.type
            gson.fromJson<List<LandRecord>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("StorageManager", "Error reading records manifest", e)
            emptyList()
        }
    }

    @Synchronized
    fun saveAllRecords(records: List<LandRecord>) {
        try {
            val json = gson.toJson(records)
            manifestFile.writeText(json)
        } catch (e: Exception) {
            Log.e("StorageManager", "Error saving records manifest", e)
        }
    }

    fun calculateSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun saveDocument(
        division: String,
        district: String,
        upazila: String,
        mouza: String,
        docType: DocumentType,
        khatianOrPlotNo: String,
        rawFileName: String,
        data: ByteArray,
        sourceUrl: String = ""
    ): SaveResult {
        return try {
            val sha256 = calculateSha256(data)
            val currentRecords = getAllRecords().toMutableList()

            // Deduplication check
            val existing = currentRecords.firstOrNull { it.sha256.equals(sha256, ignoreCase = true) }
            if (existing != null) {
                val existingFile = File(existing.filePath)
                if (existingFile.exists() && existingFile.length() > 0) {
                    return SaveResult.Duplicate(existing)
                }
            }

            // Create hierarchical directory
            val targetDir = File(
                baseArchiveDir,
                "${sanitize(division)}/${sanitize(district)}/${sanitize(upazila)}/${docType.code}"
            ).apply { if (!exists()) mkdirs() }

            val cleanName = sanitizeFileName(rawFileName)
            val finalFile = File(targetDir, cleanName)

            FileOutputStream(finalFile).use { fos ->
                fos.write(data)
                fos.flush()
            }

            val record = LandRecord(
                id = UUID.randomUUID().toString(),
                fileName = cleanName,
                filePath = finalFile.absolutePath,
                fileSize = data.size.toLong(),
                sha256 = sha256,
                division = division,
                district = district,
                upazila = upazila,
                mouza = mouza.ifBlank { "Unspecified" },
                docType = docType,
                khatianOrPlotNo = khatianOrPlotNo,
                timestamp = System.currentTimeMillis(),
                sourceUrl = sourceUrl
            )

            // Replace or add
            currentRecords.removeAll { it.sha256.equals(sha256, ignoreCase = true) }
            currentRecords.add(0, record)
            saveAllRecords(currentRecords)
            SaveResult.Success(record)
        } catch (e: Exception) {
            Log.e("StorageManager", "Error saving document", e)
            SaveResult.Error(e.localizedMessage ?: "Unknown storage error")
        }
    }

    @Synchronized
    fun saveStream(
        division: String,
        district: String,
        upazila: String,
        mouza: String,
        docType: DocumentType,
        khatianOrPlotNo: String,
        rawFileName: String,
        inputStream: InputStream,
        sourceUrl: String = ""
    ): SaveResult {
        return try {
            val bytes = inputStream.readBytes()
            saveDocument(
                division = division,
                district = district,
                upazila = upazila,
                mouza = mouza,
                docType = docType,
                khatianOrPlotNo = khatianOrPlotNo,
                rawFileName = rawFileName,
                data = bytes,
                sourceUrl = sourceUrl
            )
        } catch (e: Exception) {
            SaveResult.Error(e.localizedMessage ?: "Failed to read input stream")
        }
    }

    @Synchronized
    fun deleteRecord(recordId: String): Boolean {
        val records = getAllRecords().toMutableList()
        val record = records.firstOrNull { it.id == recordId } ?: return false

        try {
            val file = File(record.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Error deleting physical file", e)
        }

        records.removeAll { it.id == recordId }
        saveAllRecords(records)
        return true
    }

    fun getRecordsForUpazila(division: String, district: String, upazila: String): List<LandRecord> {
        val all = getAllRecords()
        val exact = all.filter {
            it.upazila.equals(upazila, ignoreCase = true) &&
            (district.isBlank() || it.district.equals(district, ignoreCase = true))
        }
        if (exact.isNotEmpty()) return exact
        return all.filter { it.upazila.equals(upazila, ignoreCase = true) }
    }

    @Synchronized
    fun clearAllDownloadedData(): Boolean {
        return try {
            if (baseArchiveDir.exists()) {
                baseArchiveDir.deleteRecursively()
                baseArchiveDir.mkdirs()
            }
            val exportsDir = File(context.cacheDir, "exports")
            if (exportsDir.exists()) {
                exportsDir.deleteRecursively()
            }
            saveAllRecords(emptyList())
            true
        } catch (e: Exception) {
            Log.e("StorageManager", "Error clearing all downloaded data", e)
            false
        }
    }

    @Synchronized
    fun clearUpazilaDownloadedData(upazila: String): Boolean {
        return try {
            val records = getAllRecords().toMutableList()
            val upazilaRecords = records.filter { it.upazila.equals(upazila, ignoreCase = true) }
            for (rec in upazilaRecords) {
                try {
                    val f = File(rec.filePath)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            records.removeAll { it.upazila.equals(upazila, ignoreCase = true) }
            saveAllRecords(records)
            true
        } catch (e: Exception) {
            Log.e("StorageManager", "Error clearing upazila data", e)
            false
        }
    }

    private fun sanitize(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "").trim().replace(" ", "_")
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (sanitized.isNotBlank()) sanitized else "record_${System.currentTimeMillis()}.bin"
    }
}
