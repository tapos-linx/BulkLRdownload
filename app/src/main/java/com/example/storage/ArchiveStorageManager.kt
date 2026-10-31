package com.example.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import com.example.data.db.DownloadJobEntity
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ArchiveWriteResult(
    val success: Boolean,
    val finalRelativePath: String,
    val absoluteOrUriPath: String,
    val isCollisionResolved: Boolean = false,
    val errorBn: String = ""
)

class ArchiveStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("lrgetbd_storage_prefs", Context.MODE_PRIVATE)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private const val KEY_SAF_TREE_URI = "saf_tree_uri"
        private const val KEY_CUSTOM_SUBFOLDER = "custom_subfolder_label"
        const val ROOT_FOLDER_NAME = "LRgetBD"
        const val COUNTRY_FOLDER_NAME = "Bangladesh"
    }

    fun getSelectedFolderTreeUri(): Uri? {
        val raw = prefs.getString(KEY_SAF_TREE_URI, null)
        return if (!raw.isNullOrBlank()) Uri.parse(raw) else null
    }

    fun setSelectedFolderTreeUri(uri: Uri?) {
        if (uri == null) {
            prefs.edit().remove(KEY_SAF_TREE_URI).apply()
            return
        }
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {
            // In unit test or non-SAF mock URI environments
        }
        prefs.edit().putString(KEY_SAF_TREE_URI, uri.toString()).apply()
    }

    fun getCustomFolderLabel(): String {
        val uri = getSelectedFolderTreeUri()
        if (uri != null) {
            val pathSegment = uri.lastPathSegment ?: uri.toString()
            return "ফোন মেমোরি (SAF নির্বাচিত): $pathSegment"
        }
        return "ফোন মেমোরি (ডিফল্ট): /Documents/$ROOT_FOLDER_NAME/$COUNTRY_FOLDER_NAME/"
    }

    fun getDefaultLocalArchiveRoot(): File {
        val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "Documents")
        val root = File(docsDir, ROOT_FOLDER_NAME)
        if (!root.exists()) root.mkdirs()
        return root
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            val dir = context.filesDir
            val stat = StatFs(dir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            1024L * 1024L * 500L
        }
    }

    fun isLowStorageSpace(thresholdBytes: Long = 20L * 1024L * 1024L): Boolean {
        return getAvailableStorageBytes() < thresholdBytes
    }

    fun buildPredictableRelativePath(job: DownloadJobEntity, fileName: String): String {
        val div = PdfValidator.sanitizeFileNameSegment(job.division)
        val dist = PdfValidator.sanitizeFileNameSegment(job.district)
        val upz = PdfValidator.sanitizeFileNameSegment(job.upazila)
        val mouza = PdfValidator.sanitizeFileNameSegment("${job.mouzaNameBn}_${job.jlNo}")
        val survey = PdfValidator.sanitizeFileNameSegment(job.surveyType)
        val recType = PdfValidator.sanitizeFileNameSegment(job.recordType)
        return "$ROOT_FOLDER_NAME/$COUNTRY_FOLDER_NAME/$div/$dist/$upz/$mouza/$survey/$recType/$fileName"
    }

    /**
     * Atomically moves or copies the validated `.part` file into the final archive hierarchy,
     * preventing accidental overwrites if a different document has the same filename.
     */
    suspend fun moveValidatedPartToArchive(
        partFile: File,
        job: DownloadJobEntity,
        sha256Checksum: String
    ): ArchiveWriteResult = withContext(Dispatchers.IO) {
        try {
            val cleanMouza = PdfValidator.sanitizeFileNameSegment(job.mouzaNameBn)
            val cleanJl = PdfValidator.sanitizeFileNameSegment(job.jlNo)
            val baseName = "${job.surveyType}_${job.recordType}_${cleanMouza}_${cleanJl}"
            var fileName = "$baseName.pdf"

            val localRoot = getDefaultLocalArchiveRoot()
            val targetDir = File(
                localRoot,
                "$COUNTRY_FOLDER_NAME/" +
                    "${PdfValidator.sanitizeFileNameSegment(job.division)}/" +
                    "${PdfValidator.sanitizeFileNameSegment(job.district)}/" +
                    "${PdfValidator.sanitizeFileNameSegment(job.upazila)}/" +
                    "${PdfValidator.sanitizeFileNameSegment("${job.mouzaNameBn}_${job.jlNo}")}/" +
                    "${PdfValidator.sanitizeFileNameSegment(job.surveyType)}/" +
                    PdfValidator.sanitizeFileNameSegment(job.recordType)
            )
            if (!targetDir.exists()) targetDir.mkdirs()

            var targetFile = File(targetDir, fileName)
            var collisionResolved = false

            // Prevent overwriting a different file with the same name
            if (targetFile.exists()) {
                val existingSha = PdfValidator.computeSha256(targetFile)
                if (existingSha != sha256Checksum) {
                    val shortHash = sha256Checksum.take(6)
                    fileName = "${baseName}_$shortHash.pdf"
                    targetFile = File(targetDir, fileName)
                    collisionResolved = true
                }
            }

            partFile.inputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            partFile.delete()

            // Also mirror to user-selected SAF directory if configured
            val safUri = getSelectedFolderTreeUri()
            var safOutputInfo = targetFile.absolutePath
            if (safUri != null) {
                mirrorToSafTreeIfPermitted(safUri, targetFile, job, fileName)?.let {
                    safOutputInfo = it
                }
            }

            val relativePath = buildPredictableRelativePath(job, fileName)
            ArchiveWriteResult(
                success = true,
                finalRelativePath = relativePath,
                absoluteOrUriPath = safOutputInfo,
                isCollisionResolved = collisionResolved
            )
        } catch (e: Exception) {
            ArchiveWriteResult(
                success = false,
                finalRelativePath = "",
                absoluteOrUriPath = "",
                errorBn = "আর্কাইভ ফাইলে সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "IO Error"}"
            )
        }
    }

    private fun mirrorToSafTreeIfPermitted(
        treeUri: Uri,
        sourceFile: File,
        job: DownloadJobEntity,
        fileName: String
    ): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val createdUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocumentUri,
                "application/pdf",
                "${job.district}_${job.upazila}_$fileName"
            )
            if (createdUri != null) {
                context.contentResolver.openOutputStream(createdUri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                }
                createdUri.toString()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun writeManifestAndReports(jobs: List<DownloadJobEntity>): File = withContext(Dispatchers.IO) {
        val root = getDefaultLocalArchiveRoot()
        val manifestFile = File(root, "manifest.json")
        manifestFile.writeText(gson.toJson(jobs), Charsets.UTF_8)

        val reportsDir = File(root, "Reports").apply { if (!exists()) mkdirs() }
        val jsonReport = File(reportsDir, "collection_report.json")
        jsonReport.writeText(gson.toJson(jobs), Charsets.UTF_8)

        val csvReport = File(reportsDir, "collection_report.csv")
        val csvBuilder = StringBuilder()
        csvBuilder.append("jobId,division,district,upazila,mouzaNameBn,jlNo,surveyType,recordType,status,fileSizeBytes,sha256Checksum,archiveRelativePath\n")
        jobs.forEach { j ->
            csvBuilder.append(
                "\"${j.jobId}\",\"${j.division}\",\"${j.district}\",\"${j.upazila}\",\"${j.mouzaNameBn}\",\"${j.jlNo}\",\"${j.surveyType}\",\"${j.recordType}\",\"${j.status}\",${j.fileSizeBytes},\"${j.sha256Checksum}\",\"${j.archiveRelativePath}\"\n"
            )
        }
        csvReport.writeText(csvBuilder.toString(), Charsets.UTF_8)
        manifestFile
    }

    suspend fun exportQueueToJsonFile(jobs: List<DownloadJobEntity>, customOutputUri: Uri? = null): File = withContext(Dispatchers.IO) {
        val reportsDir = File(getDefaultLocalArchiveRoot(), "Reports").apply { if (!exists()) mkdirs() }
        val exportFile = File(reportsDir, "lrgetbd_queue_session_${System.currentTimeMillis()}.json")
        val jsonText = gson.toJson(jobs)
        exportFile.writeText(jsonText, Charsets.UTF_8)

        if (customOutputUri != null) {
            try {
                context.contentResolver.openOutputStream(customOutputUri)?.use { out ->
                    out.write(jsonText.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
        exportFile
    }

    suspend fun importQueueFromJsonText(jsonText: String): List<DownloadJobEntity> = withContext(Dispatchers.IO) {
        return@withContext try {
            val type = object : com.google.gson.reflect.TypeToken<List<DownloadJobEntity>>() {}.type
            gson.fromJson<List<DownloadJobEntity>>(jsonText, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createMasterZipArchive(
        jobs: List<DownloadJobEntity>,
        districtName: String,
        upazilaName: String,
        destinationUri: Uri? = null
    ): File = withContext(Dispatchers.IO) {
        val validatedJobs = jobs.filter { it.status == "VALIDATED" || it.status == "VERIFIED" }
        val reportsDir = File(getDefaultLocalArchiveRoot(), "Reports").apply { if (!exists()) mkdirs() }
        val cleanDist = PdfValidator.sanitizeFileNameSegment(districtName.ifBlank { "AllDistricts" })
        val cleanUpz = PdfValidator.sanitizeFileNameSegment(upazilaName.ifBlank { "AllUpazilas" })
        val zipFile = File(reportsDir, "LRgetBD_Master_${cleanDist}_${cleanUpz}.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Add manifest.json
            val manifestEntry = ZipEntry("LRgetBD/manifest.json")
            zos.putNextEntry(manifestEntry)
            zos.write(gson.toJson(validatedJobs).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Add each validated PDF file
            val rootParent = getDefaultLocalArchiveRoot().parentFile ?: getDefaultLocalArchiveRoot()
            validatedJobs.forEach { job ->
                if (job.archiveRelativePath.isNotBlank()) {
                    val pdfFile = File(rootParent, job.archiveRelativePath)
                    if (pdfFile.exists()) {
                        val entry = ZipEntry(job.archiveRelativePath)
                        zos.putNextEntry(entry)
                        pdfFile.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }

        if (destinationUri != null) {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    zipFile.inputStream().use { input -> input.copyTo(out) }
                }
            } catch (_: Exception) {
            }
        }
        zipFile
    }
}
