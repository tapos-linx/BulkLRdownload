package com.example.storage.download

import android.content.Context
import com.example.data.JobStatus
import com.example.data.MouzaInfo
import com.example.data.db.AuditLogEntity
import com.example.data.db.DownloadJobDao
import com.example.data.db.DownloadJobEntity
import com.example.storage.ArchiveStorageManager
import com.example.storage.DownloadNotificationHelper
import com.example.storage.PdfValidationOutcome
import com.example.storage.PdfValidator
import com.example.storage.source.LandRecordSourceAdapter
import com.example.storage.source.SourceRetrievalResult
import com.example.storage.source.UnverifiedDirectGovScrapingAdapter
import com.example.storage.source.VerifiedLocalArchiveSourceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class QueueSummaryStats(
    val totalDiscovered: Int = 0,
    val pendingCount: Int = 0,
    val downloadingCount: Int = 0,
    val validatedCount: Int = 0,
    val duplicateCount: Int = 0,
    val missingNotFoundCount: Int = 0,
    val failedCount: Int = 0,
    val incompleteCount: Int = 0,
    val authRequiredCount: Int = 0,
    val accessRestrictedCount: Int = 0,
    val activeWorkers: Int = 0
)

class RealDownloadQueueEngine(
    private val context: Context,
    private val dao: DownloadJobDao,
    private val storageManager: ArchiveStorageManager,
    private val notificationHelper: DownloadNotificationHelper,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    companion object {
        const val MAX_CONCURRENT_WORKERS = 2
        const val MAX_RETRY_ATTEMPTS = 2
    }

    private val verifiedAdapter: LandRecordSourceAdapter = VerifiedLocalArchiveSourceAdapter()
    private val disabledGovAdapter: LandRecordSourceAdapter = UnverifiedDirectGovScrapingAdapter()

    val availableAdapters: List<LandRecordSourceAdapter> = listOf(verifiedAdapter, disabledGovAdapter)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _activeWorkerCount = MutableStateFlow(0)
    val activeWorkerCount: StateFlow<Int> = _activeWorkerCount.asStateFlow()

    private val _lastActivityMessageBn = MutableStateFlow("প্রস্তুত (Ready)")
    val lastActivityMessageBn: StateFlow<String> = _lastActivityMessageBn.asStateFlow()

    private var runnerJob: Job? = null
    private val workerSemaphore = Semaphore(MAX_CONCURRENT_WORKERS)

    val allJobsFlow: Flow<List<DownloadJobEntity>> = dao.observeAllJobs()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = dao.observeRecentAuditLogs()

    /**
     * Recover any interrupted DOWNLOADING jobs on startup by marking them INCOMPLETE
     * so they are never falsely marked successful.
     */
    suspend fun recoverInterruptedQueueOnStartup() = withContext(Dispatchers.IO) {
        dao.transitionStatuses(
            oldStatus = JobStatus.DOWNLOADING.name,
            newStatus = JobStatus.INCOMPLETE.name
        )
    }

    /**
     * Enqueues all selected Mouzas for the chosen Upazila, Survey Types, and Record Types.
     */
    suspend fun enqueueMouzaBatch(
        division: String,
        district: String,
        districtBn: String,
        upazila: String,
        upazilaBn: String,
        mouzas: List<MouzaInfo>,
        surveyTypes: List<String>,
        recordTypes: List<String>,
        sourceAdapterId: String = verifiedAdapter.descriptor.id
    ): List<DownloadJobEntity> = withContext(Dispatchers.IO) {
        val selectedMouzas = mouzas.filter { it.isSelected }
        if (selectedMouzas.isEmpty()) return@withContext emptyList()

        val existingJobs = dao.getAllJobsSnapshot()
        val existingIds = existingJobs.map { it.jobId }.toSet()
        val archiveUriStr = storageManager.getSelectedFolderTreeUri()?.toString()
            ?: storageManager.getDefaultLocalArchiveRoot().absolutePath

        val newEntities = mutableListOf<DownloadJobEntity>()
        val now = System.currentTimeMillis()

        selectedMouzas.forEach { mouza ->
            surveyTypes.forEach { survey ->
                recordTypes.forEach { recType ->
                    val cleanJl = mouza.jlNo.replace("\\s+".toRegex(), "_")
                    val sourceRecordId = "${district}_${upazila}_${cleanJl}_${survey}_${recType}"
                    val jobId = if (existingIds.contains(sourceRecordId)) {
                        "${sourceRecordId}_$now"
                    } else {
                        sourceRecordId
                    }

                    newEntities.add(
                        DownloadJobEntity(
                            jobId = jobId,
                            sourceName = sourceAdapterId,
                            sourceRecordId = sourceRecordId,
                            division = division,
                            district = district,
                            districtBn = districtBn,
                            upazila = upazila,
                            upazilaBn = upazilaBn,
                            mouzaId = mouza.id.ifBlank { "${upazila}_${cleanJl}" },
                            mouzaName = mouza.name,
                            mouzaNameBn = mouza.bnName,
                            jlNo = mouza.jlNo,
                            surveyType = survey,
                            recordType = recType,
                            status = JobStatus.PENDING.name,
                            progress = 0,
                            userSelectedArchiveUri = archiveUriStr,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }
        }

        dao.insertJobs(newEntities)
        dao.insertAuditLog(
            AuditLogEntity(
                jobId = "BATCH_$now",
                eventType = "BATCH_ENQUEUED",
                messageBn = "$upazilaBn উপজেলার ${selectedMouzas.size} টি মৌজা (${newEntities.size} টি কাজ) কার্যসারিতে যুক্ত হয়েছে",
                details = "District=$district, Upazila=$upazila, Folder=$archiveUriStr"
            )
        )
        _lastActivityMessageBn.value = "$upazilaBn এর ${newEntities.size} টি ফাইল কার্যসারিতে প্রস্তুত"
        newEntities
    }

    /**
     * Parses and enqueues multiple records from a user-uploaded CSV batch text.
     * Supports columns: District,Upazila,MouzaNameBn,JLNo,SurveyType,RecordType
     * or simple RecordID lines.
     */
    suspend fun enqueueCsvBatch(
        csvContent: String,
        defaultDivision: String,
        defaultDistrict: String,
        defaultDistrictBn: String,
        defaultUpazila: String,
        defaultUpazilaBn: String
    ): Int = withContext(Dispatchers.IO) {
        val lines = csvContent.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        if (lines.isEmpty()) return@withContext 0

        val archiveUriStr = storageManager.getSelectedFolderTreeUri()?.toString()
            ?: storageManager.getDefaultLocalArchiveRoot().absolutePath
        val now = System.currentTimeMillis()
        val jobsToInsert = mutableListOf<DownloadJobEntity>()

        lines.forEachIndexed { index, line ->
            // Skip header row if present
            if (index == 0 && (line.lowercase().contains("mouza") || line.lowercase().contains("record") || line.lowercase().contains("district"))) {
                return@forEachIndexed
            }

            val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
            val jobEntity = when {
                cols.size >= 4 -> {
                    val dist = cols.getOrNull(0)?.ifBlank { defaultDistrict } ?: defaultDistrict
                    val upz = cols.getOrNull(1)?.ifBlank { defaultUpazila } ?: defaultUpazila
                    val mouzaBn = cols.getOrNull(2)?.ifBlank { "মৌজা-${index + 1}" } ?: "মৌজা-${index + 1}"
                    val jl = cols.getOrNull(3)?.ifBlank { "JL ${index + 1}" } ?: "JL ${index + 1}"
                    val survey = cols.getOrNull(4)?.ifBlank { "RS" } ?: "RS"
                    val recType = cols.getOrNull(5)?.ifBlank { "KHATIAN" } ?: "KHATIAN"
                    val sourceRecId = "${dist}_${upz}_${jl.replace(" ", "_")}_${survey}_${recType}"
                    DownloadJobEntity(
                        jobId = "CSV_${now}_$index",
                        sourceRecordId = sourceRecId,
                        division = defaultDivision,
                        district = dist,
                        districtBn = if (dist.equals(defaultDistrict, true)) defaultDistrictBn else dist,
                        upazila = upz,
                        upazilaBn = if (upz.equals(defaultUpazila, true)) defaultUpazilaBn else upz,
                        mouzaId = "csv_m_$index",
                        mouzaName = mouzaBn,
                        mouzaNameBn = mouzaBn,
                        jlNo = jl,
                        surveyType = survey,
                        recordType = recType,
                        status = JobStatus.PENDING.name,
                        userSelectedArchiveUri = archiveUriStr,
                        createdAt = now + index
                    )
                }
                else -> {
                    val recId = cols.firstOrNull()?.ifBlank { "REC_${index + 1}" } ?: "REC_${index + 1}"
                    DownloadJobEntity(
                        jobId = "CSV_${now}_${recId}_$index",
                        sourceRecordId = recId,
                        division = defaultDivision,
                        district = defaultDistrict,
                        districtBn = defaultDistrictBn,
                        upazila = defaultUpazila,
                        upazilaBn = defaultUpazilaBn,
                        mouzaId = recId,
                        mouzaName = recId,
                        mouzaNameBn = "রেকর্ড $recId",
                        jlNo = "JL-${index + 1}",
                        surveyType = "RS",
                        recordType = "KHATIAN",
                        status = JobStatus.PENDING.name,
                        userSelectedArchiveUri = archiveUriStr,
                        createdAt = now + index
                    )
                }
            }
            jobsToInsert.add(jobEntity)
        }

        if (jobsToInsert.isNotEmpty()) {
            dao.insertJobs(jobsToInsert)
            dao.insertAuditLog(
                AuditLogEntity(
                    jobId = "CSV_BATCH_$now",
                    eventType = "CSV_IMPORTED",
                    messageBn = "CSV ফাইল হতে ${jobsToInsert.size} টি রেকর্ড ডাউনলোড কিউতে যুক্ত হয়েছে",
                    details = "Count=${jobsToInsert.size}"
                )
            )
            _lastActivityMessageBn.value = "CSV হতে ${jobsToInsert.size} টি রেকর্ড কিউতে যোগ করা হয়েছে"
        }
        jobsToInsert.size
    }

    fun startQueue() {
        _isPaused.value = false
        if (_isRunning.value && runnerJob?.isActive == true) return
        _isRunning.value = true

        runnerJob = scope.launch {
            try {
                while (!_isPaused.value) {
                    val pendingList = dao.getPendingOrIncompleteJobs()
                    if (pendingList.isEmpty()) break

                    // Check storage space safety
                    if (storageManager.isLowStorageSpace(5L * 1024L * 1024L)) {
                        _lastActivityMessageBn.value = "সতর্কবার্তা: ফোনে পর্যাপ্ত মেমোরি নেই! কিউ স্থগিত করা হয়েছে।"
                        _isPaused.value = true
                        break
                    }

                    // Launch conservative batch up to MAX_CONCURRENT_WORKERS (2)
                    val batch = pendingList.take(MAX_CONCURRENT_WORKERS)
                    val workerJobs = batch.map { job ->
                        launch {
                            workerSemaphore.withPermit {
                                if (!_isPaused.value) {
                                    _activeWorkerCount.value += 1
                                    try {
                                        processSingleJob(job)
                                    } finally {
                                        _activeWorkerCount.value = (_activeWorkerCount.value - 1).coerceAtLeast(0)
                                    }
                                }
                            }
                        }
                    }
                    workerJobs.forEach { it.join() }

                    // Respectful rate limit delay between conservative batches
                    delay(200L)
                }

                // Write updated manifest and reports after queue pass
                val allSnapshot = dao.getAllJobsSnapshot()
                storageManager.writeManifestAndReports(allSnapshot)
                val validated = allSnapshot.count { it.status == JobStatus.VALIDATED.name || it.status == JobStatus.VERIFIED.name }
                val dup = allSnapshot.count { it.status == JobStatus.DUPLICATE.name }
                val failed = allSnapshot.count { it.status == JobStatus.FAILED.name }
                if (!_isPaused.value && allSnapshot.isNotEmpty()) {
                    notificationHelper.notifyBatchCompleted(
                        validatedCount = validated,
                        duplicateCount = dup,
                        failedCount = failed,
                        savePath = storageManager.getCustomFolderLabel()
                    )
                    _lastActivityMessageBn.value = "সংগ্রহ সম্পন্ন: $validated টি যাচাইকৃত ফাইল সংরক্ষিত"
                }
            } finally {
                _isRunning.value = false
            }
        }
    }

    /**
     * Executes the 10-step real download & validation pipeline for a single job.
     * Can also be invoked directly in unit/Robolectric tests for deterministic verification.
     */
    suspend fun processSingleJob(
        job: DownloadJobEntity,
        customAdapter: LandRecordSourceAdapter? = null
    ): DownloadJobEntity = withContext(Dispatchers.IO) {
        val adapter = customAdapter ?: availableAdapters.find { it.descriptor.id == job.sourceName } ?: verifiedAdapter

        // Step 1: Check if source adapter is verified/enabled
        if (!adapter.descriptor.isEnabled) {
            val unverified = job.copy(
                status = JobStatus.SOURCE_UNVERIFIED.name,
                safeErrorCode = "SOURCE_DISABLED",
                validationResult = adapter.descriptor.disabledReasonBn,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateJob(unverified)
            return@withContext unverified
        }

        // Step 2: Check duplicate by sourceRecordId before network/stream work
        val existingValidated = dao.findValidatedBySourceRecordId(job.sourceRecordId)
        if (existingValidated != null && existingValidated.jobId != job.jobId) {
            val dupJob = job.copy(
                status = JobStatus.DUPLICATE.name,
                progress = 100,
                sha256Checksum = existingValidated.sha256Checksum,
                fileSizeBytes = existingValidated.fileSizeBytes,
                archiveRelativePath = existingValidated.archiveRelativePath,
                validationResult = "পূর্বেই সংরক্ষিত রেকর্ডের সাথে মিলে গেছে (${existingValidated.jobId})",
                updatedAt = System.currentTimeMillis()
            )
            dao.updateJob(dupJob)
            return@withContext dupJob
        }

        // Step 3: Transition to DOWNLOADING
        val attempt = job.attemptCount + 1
        val downloadingJob = job.copy(
            status = JobStatus.DOWNLOADING.name,
            progress = 15,
            attemptCount = attempt,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateJob(downloadingJob)
        _lastActivityMessageBn.value = "ডাউনলোড চলছে: ${job.mouzaNameBn} (${job.jlNo}) [${job.surveyType}]"

        val stagingDir = File(context.cacheDir, "lrgetbd_staging").apply { if (!exists()) mkdirs() }
        val partFile = File(stagingDir, "${PdfValidator.sanitizeFileNameSegment(job.jobId)}.part")

        try {
            // Step 4: Retrieve stream from authorized source adapter
            when (val result = adapter.retrieveRecord(downloadingJob, stagingDir)) {
                is SourceRetrievalResult.StreamReady -> {
                    // Stream response to temporary .part file
                    result.inputStream.use { input ->
                        FileOutputStream(partFile).use { output ->
                            val buffer = ByteArray(4096)
                            var bytesRead = input.read(buffer)
                            while (bytesRead != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesRead = input.read(buffer)
                            }
                            output.fd.sync()
                        }
                    }

                    // Mark DOWNLOADED_UNVALIDATED prior to PDF inspection
                    dao.updateJob(
                        downloadingJob.copy(
                            status = JobStatus.DOWNLOADED_UNVALIDATED.name,
                            progress = 65,
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    // Step 5 & 6 & 7: Validate MIME type, PDF signature (%PDF-), reject HTML login pages, compute SHA-256
                    when (val validation = PdfValidator.validatePartFile(partFile, result.mimeType)) {
                        is PdfValidationOutcome.Valid -> {
                            // Check content checksum duplicate
                            val checksumMatch = dao.findByChecksum(validation.sha256Checksum)
                            if (checksumMatch != null && checksumMatch.jobId != job.jobId) {
                                partFile.delete()
                                val duplicateByHash = downloadingJob.copy(
                                    status = JobStatus.DUPLICATE.name,
                                    progress = 100,
                                    sha256Checksum = validation.sha256Checksum,
                                    fileSizeBytes = validation.fileSizeBytes,
                                    archiveRelativePath = checksumMatch.archiveRelativePath,
                                    validationResult = "SHA-256 চেকসাম মিলে গেছে (ডুপ্লিকেট ফাইল)",
                                    updatedAt = System.currentTimeMillis()
                                )
                                dao.updateJob(duplicateByHash)
                                return@withContext duplicateByHash
                            }

                            // Step 8: Atomically move validated .part file to final archive location
                            val writeResult = storageManager.moveValidatedPartToArchive(
                                partFile = partFile,
                                job = downloadingJob,
                                sha256Checksum = validation.sha256Checksum
                            )

                            if (writeResult.success) {
                                // Step 9 & 10: Write metadata, audit entry, and mark VALIDATED
                                val finalJob = downloadingJob.copy(
                                    status = JobStatus.VALIDATED.name,
                                    progress = 100,
                                    fileSizeBytes = validation.fileSizeBytes,
                                    sha256Checksum = validation.sha256Checksum,
                                    validationResult = validation.detailsBn,
                                    archiveRelativePath = writeResult.finalRelativePath,
                                    userSelectedArchiveUri = writeResult.absoluteOrUriPath,
                                    safeErrorCode = "",
                                    updatedAt = System.currentTimeMillis()
                                )
                                dao.updateJob(finalJob)
                                dao.insertAuditLog(
                                    AuditLogEntity(
                                        jobId = finalJob.jobId,
                                        eventType = "FILE_VALIDATED_AND_ARCHIVED",
                                        messageBn = "${finalJob.mouzaNameBn} (${finalJob.jlNo}) সফলভাবে যাচাই ও সংরক্ষণ করা হয়েছে",
                                        details = "SHA256=${validation.sha256Checksum.take(16)}..., Path=${writeResult.finalRelativePath}"
                                    )
                                )
                                return@withContext finalJob
                            } else {
                                partFile.delete()
                                val failedWrite = downloadingJob.copy(
                                    status = JobStatus.FAILED.name,
                                    progress = 0,
                                    safeErrorCode = "ARCHIVE_WRITE_ERROR",
                                    validationResult = writeResult.errorBn,
                                    updatedAt = System.currentTimeMillis()
                                )
                                dao.updateJob(failedWrite)
                                return@withContext failedWrite
                            }
                        }

                        is PdfValidationOutcome.Invalid -> {
                            partFile.delete()
                            val invalidJob = downloadingJob.copy(
                                status = JobStatus.FAILED.name,
                                progress = 0,
                                safeErrorCode = validation.safeErrorCode,
                                validationResult = validation.reasonBn,
                                updatedAt = System.currentTimeMillis()
                            )
                            dao.updateJob(invalidJob)
                            dao.insertAuditLog(
                                AuditLogEntity(
                                    jobId = invalidJob.jobId,
                                    eventType = "VALIDATION_REJECTED",
                                    messageBn = "${invalidJob.mouzaNameBn}: ${validation.reasonBn}",
                                    details = "ErrorCode=${validation.safeErrorCode}"
                                )
                            )
                            return@withContext invalidJob
                        }
                    }
                }

                is SourceRetrievalResult.AuthRequired -> {
                    val authJob = downloadingJob.copy(
                        status = JobStatus.AUTH_REQUIRED.name,
                        progress = 0,
                        safeErrorCode = result.safeCode,
                        validationResult = result.reasonBn,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.updateJob(authJob)
                    return@withContext authJob
                }

                is SourceRetrievalResult.AccessRestricted -> {
                    val restrictedJob = downloadingJob.copy(
                        status = JobStatus.ACCESS_RESTRICTED.name,
                        progress = 0,
                        safeErrorCode = result.safeCode,
                        validationResult = result.reasonBn,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.updateJob(restrictedJob)
                    return@withContext restrictedJob
                }

                is SourceRetrievalResult.NotFound -> {
                    val notFoundJob = downloadingJob.copy(
                        status = JobStatus.NOT_FOUND.name,
                        progress = 0,
                        safeErrorCode = result.safeCode,
                        validationResult = result.reasonBn,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.updateJob(notFoundJob)
                    return@withContext notFoundJob
                }

                is SourceRetrievalResult.UnverifiedSource -> {
                    val unverifiedJob = downloadingJob.copy(
                        status = JobStatus.SOURCE_UNVERIFIED.name,
                        progress = 0,
                        safeErrorCode = result.safeCode,
                        validationResult = result.reasonBn,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.updateJob(unverifiedJob)
                    return@withContext unverifiedJob
                }

                is SourceRetrievalResult.Failure -> {
                    val nextStatus = if (attempt < MAX_RETRY_ATTEMPTS) JobStatus.INCOMPLETE.name else JobStatus.FAILED.name
                    val failedJob = downloadingJob.copy(
                        status = nextStatus,
                        progress = 0,
                        safeErrorCode = result.safeCode,
                        validationResult = result.reasonBn,
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.updateJob(failedJob)
                    return@withContext failedJob
                }
            }
        } catch (e: Exception) {
            partFile.delete()
            val failedJob = downloadingJob.copy(
                status = JobStatus.FAILED.name,
                progress = 0,
                safeErrorCode = "EXCEPTION_DURING_STREAM",
                validationResult = e.localizedMessage ?: "নেটওয়ার্ক বা ফাইল স্ট্রিম ত্রুটি",
                updatedAt = System.currentTimeMillis()
            )
            dao.updateJob(failedJob)
            return@withContext failedJob
        }
    }

    fun pauseQueue() {
        _isPaused.value = true
        _isRunning.value = false
        _lastActivityMessageBn.value = "কার্যসারি সাময়িক বিরতিতে আছে (Paused)"
    }

    fun resumeQueue() {
        _isPaused.value = false
        startQueue()
    }

    suspend fun cancelAndClearQueue() = withContext(Dispatchers.IO) {
        _isPaused.value = true
        _isRunning.value = false
        runnerJob?.cancel()
        dao.clearAllJobs()
        _lastActivityMessageBn.value = "কার্যসারি পরিষ্কার করা হয়েছে"
    }

    suspend fun retryFailedAndIncompleteJobs() = withContext(Dispatchers.IO) {
        dao.transitionStatuses(JobStatus.FAILED.name, JobStatus.PENDING.name)
        dao.transitionStatuses(JobStatus.INCOMPLETE.name, JobStatus.PENDING.name)
        _lastActivityMessageBn.value = "ব্যর্থ ও অসম্পূর্ণ কাজগুলো পুনরায় প্রচেষ্টার জন্য প্রস্তুত"
        startQueue()
    }

    suspend fun retryAuthRequiredJobs() = withContext(Dispatchers.IO) {
        dao.transitionStatuses(JobStatus.AUTH_REQUIRED.name, JobStatus.PENDING.name)
        dao.transitionStatuses(JobStatus.ACCESS_RESTRICTED.name, JobStatus.PENDING.name)
        _lastActivityMessageBn.value = "অনুমোদন-প্রয়োজনীয় কাজগুলো পুনরায় প্রচেষ্টার জন্য প্রস্তুত"
        startQueue()
    }
}
