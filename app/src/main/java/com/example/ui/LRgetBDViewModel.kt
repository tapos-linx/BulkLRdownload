package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.District
import com.example.data.Division
import com.example.data.JobStatus
import com.example.data.LocationRepository
import com.example.data.MouzaInfo
import com.example.data.RecordCategory
import com.example.data.SurveyCategory
import com.example.data.db.AuditLogEntity
import com.example.data.db.DownloadJobEntity
import com.example.data.db.LRgetBDDatabase
import com.example.storage.ArchiveStorageManager
import com.example.storage.DownloadNotificationHelper
import com.example.storage.download.QueueSummaryStats
import com.example.storage.download.RealDownloadQueueEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LRgetBDSection(
    val titleBn: String,
    val subtitleBn: String
) {
    HOME("মূল পাতা", "ড্যাশবোর্ড ও সারসংক্ষেপ"),
    AREA_SELECTION("এলাকা নির্বাচন", "জেলা, উপজেলা ও অটো-সিলেক্ট মৌজা"),
    QUEUE("কার্যসারি", "ডাউনলোড কিউ ও JSON এক্সপোর্ট"),
    ACTIVE_WORK("চলমান কাজ", "কনকারেন্ট স্ট্রিম ও যাচাইকরণ"),
    HISTORY("সংগ্রহ ইতিহাস", "অডিট লগ ও ট্রেসিবিলিটি"),
    ARCHIVE("ফাইল ভাণ্ডার", "ফোন মেমোরি ও ফোল্ডার ব্যবস্থাপনা"),
    REPORTS("প্রতিবেদন", "CSV, JSON ও মাস্টার জিপ রিপোর্ট"),
    SETTINGS("সেটিংস", "উৎস অ্যাডাপ্টার ও নিরাপত্তা নীতিমালা")
}

class LRgetBDViewModel(
    private val appContext: Context,
    val locationRepo: LocationRepository,
    val storageManager: ArchiveStorageManager,
    val notificationHelper: DownloadNotificationHelper,
    val queueEngine: RealDownloadQueueEngine
) : ViewModel() {

    private val _currentSection = MutableStateFlow(LRgetBDSection.AREA_SELECTION)
    val currentSection: StateFlow<LRgetBDSection> = _currentSection.asStateFlow()

    val divisions: List<Division> = locationRepo.getDivisions()

    private val _selectedDivision = MutableStateFlow(divisions.firstOrNull()?.name ?: "Chattogram")
    val selectedDivision: StateFlow<String> = _selectedDivision.asStateFlow()

    // Default to Cumilla first (with Brahmanbaria right beside it in dropdown)
    private val _selectedDistrict = MutableStateFlow("Cumilla")
    val selectedDistrict: StateFlow<String> = _selectedDistrict.asStateFlow()

    private val _selectedUpazila = MutableStateFlow("")
    val selectedUpazila: StateFlow<String> = _selectedUpazila.asStateFlow()

    private val _mouzas = MutableStateFlow<List<MouzaInfo>>(emptyList())
    val mouzas: StateFlow<List<MouzaInfo>> = _mouzas.asStateFlow()

    private val _mouzaSearchQuery = MutableStateFlow("")
    val mouzaSearchQuery: StateFlow<String> = _mouzaSearchQuery.asStateFlow()

    private val _selectedSurveys = MutableStateFlow(setOf(SurveyCategory.RS.code, SurveyCategory.BS_BRS.code))
    val selectedSurveys: StateFlow<Set<String>> = _selectedSurveys.asStateFlow()

    private val _selectedRecordTypes = MutableStateFlow(setOf(RecordCategory.KHATIAN.code))
    val selectedRecordTypes: StateFlow<Set<String>> = _selectedRecordTypes.asStateFlow()

    private val _selectedFolderLabel = MutableStateFlow(storageManager.getCustomFolderLabel())
    val selectedFolderLabel: StateFlow<String> = _selectedFolderLabel.asStateFlow()

    // Download Confirmation Prompt State
    private val _showDownloadConfirmDialog = MutableStateFlow(false)
    val showDownloadConfirmDialog: StateFlow<Boolean> = _showDownloadConfirmDialog.asStateFlow()

    private val _pendingConfirmAutoQueued = MutableStateFlow(false)
    val pendingConfirmAutoQueued: StateFlow<Boolean> = _pendingConfirmAutoQueued.asStateFlow()

    private val _statusBannerMessageBn = MutableStateFlow("")
    val statusBannerMessageBn: StateFlow<String> = _statusBannerMessageBn.asStateFlow()

    val jobsFlow: StateFlow<List<DownloadJobEntity>> = queueEngine.allJobsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogsFlow: StateFlow<List<AuditLogEntity>> = queueEngine.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryStats: StateFlow<QueueSummaryStats> = combine(
        jobsFlow,
        queueEngine.activeWorkerCount
    ) { jobs, workers ->
        QueueSummaryStats(
            totalDiscovered = jobs.size,
            pendingCount = jobs.count { it.status == JobStatus.PENDING.name },
            downloadingCount = jobs.count { it.status == JobStatus.DOWNLOADING.name || it.status == JobStatus.DOWNLOADED_UNVALIDATED.name },
            validatedCount = jobs.count { it.status == JobStatus.VALIDATED.name || it.status == JobStatus.VERIFIED.name },
            duplicateCount = jobs.count { it.status == JobStatus.DUPLICATE.name },
            missingNotFoundCount = jobs.count { it.status == JobStatus.NOT_FOUND.name },
            failedCount = jobs.count { it.status == JobStatus.FAILED.name },
            incompleteCount = jobs.count { it.status == JobStatus.INCOMPLETE.name },
            authRequiredCount = jobs.count { it.status == JobStatus.AUTH_REQUIRED.name },
            accessRestrictedCount = jobs.count { it.status == JobStatus.ACCESS_RESTRICTED.name },
            activeWorkers = workers
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QueueSummaryStats())

    init {
        viewModelScope.launch {
            queueEngine.recoverInterruptedQueueOnStartup()
        }
        // Initialize default Cumilla upazilas and auto-select first upazila's mouzas
        val initialUpazilas = locationRepo.getUpazilas("Chattogram", "Cumilla")
        val firstUpazila = initialUpazilas.firstOrNull() ?: "Cumilla Adarsha Sadar"
        onUpazilaSelected(firstUpazila, triggerAutoQueueAndPrompt = false)
    }

    fun navigateToSection(section: LRgetBDSection) {
        _currentSection.value = section
    }

    fun getAvailableDistricts(): List<District> {
        return locationRepo.getPriorityDistrictsFirst(_selectedDivision.value)
    }

    fun getAvailableUpazilas(): List<String> {
        return locationRepo.getUpazilas(_selectedDivision.value, _selectedDistrict.value)
    }

    /**
     * Cascade Rule 1: Changing Division resets District, Upazila, and Mouzas.
     */
    fun onDivisionSelected(divisionName: String) {
        _selectedDivision.value = divisionName
        val districts = locationRepo.getPriorityDistrictsFirst(divisionName)
        val firstDistrict = districts.firstOrNull()?.name ?: ""
        _selectedDistrict.value = firstDistrict
        _selectedUpazila.value = ""
        _mouzas.value = emptyList()
        _mouzaSearchQuery.value = ""
    }

    /**
     * Cascade Rule 2: Selecting a District (e.g., Cumilla or Brahmanbaria)
     * loads all related Upazilas for that District and resets invalid child selection.
     */
    fun onDistrictSelected(districtName: String) {
        _selectedDistrict.value = districtName
        _selectedUpazila.value = ""
        _mouzas.value = emptyList()
        _mouzaSearchQuery.value = ""
        val upazilas = locationRepo.getUpazilas(_selectedDivision.value, districtName)
        _statusBannerMessageBn.value = "$districtName জেলার ${upazilas.size} টি উপজেলা লোড হয়েছে। উপজেলা নির্বাচন করুন।"
    }

    /**
     * Cascade Rule 3 (Crucial Requirement):
     * As soon as the user selects any Upazila, ALL Mouzas under that Upazila are
     * automatically selected (100% auto-selected), added to the download queue for the next
     * process, and a notification + confirmation dialog asks the user if they want to
     * start downloading now or change the phone memory folder first.
     */
    fun onUpazilaSelected(upazilaName: String, triggerAutoQueueAndPrompt: Boolean = true) {
        _selectedUpazila.value = upazilaName
        _mouzaSearchQuery.value = ""
        if (upazilaName.isBlank()) {
            _mouzas.value = emptyList()
            return
        }

        val loadedMouzas = locationRepo.getMouzasForUpazila(
            upazilaName = upazilaName,
            districtName = _selectedDistrict.value,
            divisionName = _selectedDivision.value
        ).map { it.copy(isSelected = true) } // 100% Auto-Selected

        _mouzas.value = loadedMouzas

        if (triggerAutoQueueAndPrompt && loadedMouzas.isNotEmpty()) {
            val distObj = getAvailableDistricts().find { it.name.equals(_selectedDistrict.value, true) }
            val distBn = distObj?.bnName ?: _selectedDistrict.value
            val upzBn = locationRepo.getUpazilaDisplayName(upazilaName)

            _pendingConfirmAutoQueued.value = true
            _showDownloadConfirmDialog.value = true

            // Send notification to user asking if they want to download the files
            notificationHelper.notifyDownloadPrompt(
                upazilaBn = upzBn,
                mouzaCount = loadedMouzas.size,
                folderLabel = _selectedFolderLabel.value
            )

            viewModelScope.launch {
                val enqueued = queueEngine.enqueueMouzaBatch(
                    division = _selectedDivision.value,
                    district = _selectedDistrict.value,
                    districtBn = distBn,
                    upazila = upazilaName,
                    upazilaBn = upzBn,
                    mouzas = loadedMouzas,
                    surveyTypes = _selectedSurveys.value.toList(),
                    recordTypes = _selectedRecordTypes.value.toList()
                )
                _statusBannerMessageBn.value = "$upzBn এর সব মৌজা (${loadedMouzas.size} টি) স্বয়ংক্রিয়ভাবে সিলেক্ট ও ডাউনলোড কিউতে (${enqueued.size} টি কাজ) প্রস্তুত করা হয়েছে।"
            }
        }
    }

    fun updateMouzaSearchQuery(query: String) {
        _mouzaSearchQuery.value = query
    }

    fun toggleMouzaSelection(mouzaId: String) {
        _mouzas.value = _mouzas.value.map { m ->
            if (m.id == mouzaId) m.copy(isSelected = !m.isSelected) else m
        }
    }

    fun selectAllMouzas(selectAll: Boolean) {
        val query = _mouzaSearchQuery.value.trim()
        _mouzas.value = _mouzas.value.map { m ->
            if (query.isEmpty() ||
                m.name.contains(query, ignoreCase = true) ||
                m.bnName.contains(query, ignoreCase = true) ||
                m.jlNo.contains(query, ignoreCase = true)
            ) {
                m.copy(isSelected = selectAll)
            } else {
                m
            }
        }
    }

    fun toggleSurveyCategory(code: String) {
        val current = _selectedSurveys.value.toMutableSet()
        if (current.contains(code)) {
            if (current.size > 1) current.remove(code)
        } else {
            current.add(code)
        }
        _selectedSurveys.value = current
    }

    fun toggleRecordCategory(code: String) {
        val current = _selectedRecordTypes.value.toMutableSet()
        if (current.contains(code)) {
            if (current.size > 1) current.remove(code)
        } else {
            current.add(code)
        }
        _selectedRecordTypes.value = current
    }

    fun onPhoneMemoryFolderChosen(uri: Uri?) {
        storageManager.setSelectedFolderTreeUri(uri)
        _selectedFolderLabel.value = storageManager.getCustomFolderLabel()
        _statusBannerMessageBn.value = "সংরক্ষণ ফোল্ডার আপডেট হয়েছে: ${_selectedFolderLabel.value}"
    }

    /**
     * Triggered when user clicks primary action "সংগ্রহ শুরু করুন" or "ডাউনলোড শুরু করুন".
     * Shows the confirmation notification & dialog asking user whether to download now and
     * allowing them to verify or change the phone memory folder.
     */
    fun requestStartCollectionWithConfirmation() {
        val selectedCount = _mouzas.value.count { it.isSelected }
        if (selectedCount == 0 && jobsFlow.value.isEmpty()) {
            _statusBannerMessageBn.value = "অনুগ্রহ করে অন্তত ১টি মৌজা নির্বাচন করুন"
            return
        }
        val upzBn = locationRepo.getUpazilaDisplayName(_selectedUpazila.value.ifBlank { "নির্বাচিত উপজেলা" })
        notificationHelper.notifyDownloadPrompt(
            upazilaBn = upzBn,
            mouzaCount = selectedCount,
            folderLabel = _selectedFolderLabel.value
        )
        _pendingConfirmAutoQueued.value = false
        _showDownloadConfirmDialog.value = true
    }

    fun confirmAndStartDownloadNow() {
        _showDownloadConfirmDialog.value = false
        viewModelScope.launch {
            if (!_pendingConfirmAutoQueued.value && _selectedUpazila.value.isNotBlank()) {
                val distObj = getAvailableDistricts().find { it.name.equals(_selectedDistrict.value, true) }
                val distBn = distObj?.bnName ?: _selectedDistrict.value
                val upzBn = locationRepo.getUpazilaDisplayName(_selectedUpazila.value)
                queueEngine.enqueueMouzaBatch(
                    division = _selectedDivision.value,
                    district = _selectedDistrict.value,
                    districtBn = distBn,
                    upazila = _selectedUpazila.value,
                    upazilaBn = upzBn,
                    mouzas = _mouzas.value,
                    surveyTypes = _selectedSurveys.value.toList(),
                    recordTypes = _selectedRecordTypes.value.toList()
                )
            }
            _pendingConfirmAutoQueued.value = false
            queueEngine.startQueue()
            _statusBannerMessageBn.value = "ডাউনলোড ও SHA-256 যাচাইকরণ শুরু হয়েছে (${_selectedFolderLabel.value})"
        }
    }

    fun dismissDownloadConfirmation() {
        _showDownloadConfirmDialog.value = false
        _statusBannerMessageBn.value = "ফাইলগুলো কার্যসারিতে সংরক্ষিত আছে। যেকোনো সময় 'সংগ্রহ শুরু করুন' চেপে শুরু করতে পারবেন।"
    }

    fun exportQueueAsJson(destinationUri: Uri? = null) {
        viewModelScope.launch {
            val file = storageManager.exportQueueToJsonFile(jobsFlow.value, destinationUri)
            _statusBannerMessageBn.value = "ডাউনলোড কিউ JSON সংরক্ষিত হয়েছে: ${file.name}"
        }
    }

    fun importCsvBatchFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val csvText = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                importCsvBatchFromString(csvText)
            } catch (e: Exception) {
                _statusBannerMessageBn.value = "CSV ফাইল পড়তে ত্রুটি: ${e.localizedMessage}"
            }
        }
    }

    fun importCsvBatchFromString(csvText: String) {
        viewModelScope.launch {
            val distObj = getAvailableDistricts().find { it.name.equals(_selectedDistrict.value, true) }
            val distBn = distObj?.bnName ?: _selectedDistrict.value
            val upz = _selectedUpazila.value.ifBlank { "Cumilla Adarsha Sadar" }
            val upzBn = locationRepo.getUpazilaDisplayName(upz)

            val count = queueEngine.enqueueCsvBatch(
                csvContent = csvText,
                defaultDivision = _selectedDivision.value,
                defaultDistrict = _selectedDistrict.value,
                defaultDistrictBn = distBn,
                defaultUpazila = upz,
                defaultUpazilaBn = upzBn
            )
            _statusBannerMessageBn.value = "CSV ব্যাচ হতে $count টি রেকর্ড স্বয়ংক্রিয়ভাবে ডাউনলোড কিউতে যুক্ত হয়েছে"
        }
    }

    fun exportMasterZip(destinationUri: Uri? = null) {
        viewModelScope.launch {
            val zipFile = storageManager.createMasterZipArchive(
                jobs = jobsFlow.value,
                districtName = _selectedDistrict.value,
                upazilaName = _selectedUpazila.value,
                destinationUri = destinationUri
            )
            _statusBannerMessageBn.value = "মাস্টার জিপ প্রস্তুত ও ফোন মেমোরিতে সংরক্ষিত: ${zipFile.name}"
        }
    }

    fun cancelAndClearQueue() {
        viewModelScope.launch {
            queueEngine.cancelAndClearQueue()
            _statusBannerMessageBn.value = "সব কাজ বাতিল ও কার্যসারি পরিষ্কার করা হয়েছে"
        }
    }

    fun retryFailedAndIncompleteJobs() {
        viewModelScope.launch {
            queueEngine.retryFailedAndIncompleteJobs()
            _statusBannerMessageBn.value = "ব্যর্থ ও অসম্পূর্ণ কাজগুলো পুনরায় শুরু করা হয়েছে"
        }
    }

    fun retryAuthRequiredJobs() {
        viewModelScope.launch {
            queueEngine.retryAuthRequiredJobs()
            _statusBannerMessageBn.value = "অনুমোদন প্রয়োজন এমন কাজগুলো পুনরায় কিউতে পাঠানো হয়েছে"
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = LRgetBDDatabase.getInstance(appCtx)
            val locRepo = LocationRepository(appCtx)
            val storage = ArchiveStorageManager(appCtx)
            val notif = DownloadNotificationHelper(appCtx)
            val engine = RealDownloadQueueEngine(appCtx, db.downloadJobDao(), storage, notif)
            return LRgetBDViewModel(appCtx, locRepo, storage, notif, engine) as T
        }
    }
}
