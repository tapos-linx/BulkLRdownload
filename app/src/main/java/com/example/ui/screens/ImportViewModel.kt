package com.example.ui.screens

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DocumentType
import com.example.data.LandRecord
import com.example.storage.DocumentDirectoryService
import com.example.storage.ImportCandidate
import com.example.storage.SaveResult
import com.example.storage.StorageManager
import com.example.storage.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Represents a Mouza folder containing land records belonging to a specific Mouza.
 */
data class MouzaFolder(
    val mouzaName: String,
    val records: List<ImportCandidate>,
    val totalSizeBytes: Long = records.sumOf { it.size },
    val isExpanded: Boolean = true
)

/**
 * Represents a District folder containing Mouza subfolders.
 */
data class DistrictFolder(
    val districtName: String,
    val mouzaFolders: List<MouzaFolder>,
    val totalRecordsCount: Int = mouzaFolders.sumOf { it.records.size },
    val totalSizeBytes: Long = mouzaFolders.sumOf { it.totalSizeBytes },
    val isExpanded: Boolean = true
)

/**
 * Summary of physical offline archive organization.
 */
data class OfflineOrganizationSummary(
    val totalDistricts: Int,
    val totalMouzas: Int,
    val savedCount: Int,
    val duplicateCount: Int,
    val rootFolderPath: String,
    val createdFolderPaths: List<String>
)

/**
 * Represents a categorized offline archive folder destination based on
 * District and Mouza attributes.
 */
data class OfflineArchiveFolder(
    val district: String,
    val mouza: String,
    val relativeFolderPath: String,
    val records: List<ImportCandidate>,
    val totalFiles: Int = records.size,
    val totalSizeBytes: Long = records.sumOf { it.size }
)

/**
 * Represents a categorized offline archive folder for saved LandRecords based on
 * District and Mouza attributes.
 */
data class SavedOfflineArchiveFolder(
    val district: String,
    val mouza: String,
    val relativeFolderPath: String,
    val records: List<LandRecord>,
    val totalFiles: Int = records.size,
    val totalSizeBytes: Long = records.sumOf { it.fileSize }
)

/**
 * ViewModel responsible for managing import candidates, parsing archives,
 * and categorizing imported records into structured offline archive folders based on
 * 'District' and 'Mouza' attributes.
 */
class ImportViewModel(
    private val storageManager: StorageManager,
    private val zipManager: ZipManager,
    private val docDirService: DocumentDirectoryService? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ImportViewModel"
        const val DEFAULT_UNASSIGNED_DISTRICT = "Unassigned District"
        const val DEFAULT_UNASSIGNED_MOUZA = "Unassigned Mouza"
    }

    private val _candidates = MutableStateFlow<List<ImportCandidate>>(emptyList())
    val candidates: StateFlow<List<ImportCandidate>> = _candidates.asStateFlow()

    private val _categorizedFolders = MutableStateFlow<List<DistrictFolder>>(emptyList())
    val categorizedFolders: StateFlow<List<DistrictFolder>> = _categorizedFolders.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _currentFileProcessing = MutableStateFlow("")
    val currentFileProcessing: StateFlow<String> = _currentFileProcessing.asStateFlow()

    private val _isFolderView = MutableStateFlow(true)
    val isFolderView: StateFlow<Boolean> = _isFolderView.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun setFolderView(enabled: Boolean) {
        _isFolderView.value = enabled
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Primary ViewModel function to categorize imported records into folders based on
     * 'District' and 'Mouza' attributes to help users organize their offline archive.
     *
     * Returns a nested Map: District -> Mouza -> List of ImportCandidate.
     * Empty or blank values are sanitized and grouped under standard labels.
     * Districts and Mouzas are sorted alphabetically.
     */
    fun categorizeImportedRecordsIntoFolders(
        records: List<ImportCandidate> = _candidates.value
    ): Map<String, Map<String, List<ImportCandidate>>> {
        if (records.isEmpty()) return emptyMap()

        return records
            .groupBy { candidate ->
                cleanLocationAttribute(candidate.district, DEFAULT_UNASSIGNED_DISTRICT)
            }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .mapValues { (_, districtRecords) ->
                districtRecords
                    .groupBy { candidate ->
                        cleanLocationAttribute(candidate.mouza, DEFAULT_UNASSIGNED_MOUZA)
                    }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            }
    }

    /**
     * Categorizes imported records into high-level DistrictFolder and MouzaFolder
     * models for intuitive UI tree rendering and folder manipulation.
     */
    fun categorizeRecordsByDistrictAndMouza(
        records: List<ImportCandidate> = _candidates.value
    ): List<DistrictFolder> {
        val groupedMap = categorizeImportedRecordsIntoFolders(records)

        // Preserve previous expand states if available
        val currentDistrictStates = _categorizedFolders.value.associate { it.districtName to it.isExpanded }
        val currentMouzaStates = _categorizedFolders.value.flatMap { it.mouzaFolders }
            .associate { it.mouzaName to it.isExpanded }

        return groupedMap.map { (districtName, mouzasMap) ->
            val mouzaFolders = mouzasMap.map { (mouzaName, mouzaRecords) ->
                MouzaFolder(
                    mouzaName = mouzaName,
                    records = mouzaRecords,
                    totalSizeBytes = mouzaRecords.sumOf { it.size },
                    isExpanded = currentMouzaStates[mouzaName] ?: true
                )
            }
            DistrictFolder(
                districtName = districtName,
                mouzaFolders = mouzaFolders,
                totalRecordsCount = mouzaFolders.sumOf { it.records.size },
                totalSizeBytes = mouzaFolders.sumOf { it.totalSizeBytes },
                isExpanded = currentDistrictStates[districtName] ?: true
            )
        }
    }

    /**
     * Categorizes already saved LandRecords into District and Mouza folders.
     */
    fun categorizeSavedRecordsIntoFolders(
        records: List<LandRecord>
    ): Map<String, Map<String, List<LandRecord>>> {
        if (records.isEmpty()) return emptyMap()

        return records
            .groupBy { rec -> cleanLocationAttribute(rec.district, DEFAULT_UNASSIGNED_DISTRICT) }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .mapValues { (_, districtRecords) ->
                districtRecords
                    .groupBy { rec -> cleanLocationAttribute(rec.mouza, DEFAULT_UNASSIGNED_MOUZA) }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            }
    }

    /**
     * ViewModel function to categorize imported records into a flat list of distinct
     * offline archive folders organized by 'District' and 'Mouza' attributes.
     *
     * @param records List of imported candidate records to categorize (defaults to current _candidates)
     * @return List of [OfflineArchiveFolder] sorted alphabetically by District and Mouza
     */
    fun categorizeImportedRecordsIntoOfflineArchiveFolders(
        records: List<ImportCandidate> = _candidates.value
    ): List<OfflineArchiveFolder> {
        if (records.isEmpty()) return emptyList()

        return records
            .groupBy { candidate ->
                Pair(
                    cleanLocationAttribute(candidate.district, DEFAULT_UNASSIGNED_DISTRICT),
                    cleanLocationAttribute(candidate.mouza, DEFAULT_UNASSIGNED_MOUZA)
                )
            }
            .toSortedMap(compareBy({ it.first.lowercase(Locale.ROOT) }, { it.second.lowercase(Locale.ROOT) }))
            .map { (locationPair, candidateList) ->
                val (district, mouza) = locationPair
                OfflineArchiveFolder(
                    district = district,
                    mouza = mouza,
                    relativeFolderPath = "${sanitizePath(district)}/${sanitizePath(mouza)}",
                    records = candidateList,
                    totalFiles = candidateList.size,
                    totalSizeBytes = candidateList.sumOf { it.size }
                )
            }
    }

    /**
     * ViewModel function to categorize saved land records into offline archive folder
     * structures based on 'District' and 'Mouza' attributes.
     *
     * @param records List of saved LandRecord items
     * @return List of [SavedOfflineArchiveFolder] sorted alphabetically by District and Mouza
     */
    fun categorizeSavedRecordsIntoOfflineArchiveFolders(
        records: List<LandRecord>
    ): List<SavedOfflineArchiveFolder> {
        if (records.isEmpty()) return emptyList()

        return records
            .groupBy { rec ->
                Pair(
                    cleanLocationAttribute(rec.district, DEFAULT_UNASSIGNED_DISTRICT),
                    cleanLocationAttribute(rec.mouza, DEFAULT_UNASSIGNED_MOUZA)
                )
            }
            .toSortedMap(compareBy({ it.first.lowercase(Locale.ROOT) }, { it.second.lowercase(Locale.ROOT) }))
            .map { (locationPair, recordList) ->
                val (district, mouza) = locationPair
                SavedOfflineArchiveFolder(
                    district = district,
                    mouza = mouza,
                    relativeFolderPath = "${sanitizePath(district)}/${sanitizePath(mouza)}",
                    records = recordList,
                    totalFiles = recordList.size,
                    totalSizeBytes = recordList.sumOf { it.fileSize }
                )
            }
    }

    /**
     * Physically organizes and saves imported records into structured offline archive
     * folders on device storage based on 'District' and 'Mouza' attributes:
     *
     * Structure: <OfflineArchiveBaseDir>/<District>/<Mouza>/<DocType>/<FileName>
     */
    suspend fun organizeAndSaveImportedRecordsIntoFolders(
        records: List<ImportCandidate> = _candidates.value.filter { it.isSelected },
        customDestinationDir: File? = null
    ): OfflineOrganizationSummary = withContext(Dispatchers.IO) {
        val categorized = categorizeImportedRecordsIntoFolders(records)
        val baseDir = customDestinationDir ?: File(storageManager.baseArchiveDir, "Organized_Archive")
        if (!baseDir.exists()) baseDir.mkdirs()

        val createdPaths = mutableListOf<String>()
        var savedCount = 0
        var dupCount = 0
        val total = records.size
        var currentIndex = 0

        categorized.forEach { (district, mouzasMap) ->
            mouzasMap.forEach { (mouza, candidateList) ->
                candidateList.forEach { candidate ->
                    currentIndex++
                    _currentFileProcessing.value = "Organizing ${candidate.fileName} in $district / $mouza..."
                    _importProgress.value = currentIndex.toFloat() / total.coerceAtLeast(1)

                    // 1. Physical folder creation based on District and Mouza
                    val targetFolder = File(baseDir, "${sanitizePath(district)}/${sanitizePath(mouza)}/${candidate.docType.code}").apply {
                        if (!exists()) mkdirs()
                    }
                    val targetFile = File(targetFolder, candidate.fileName)
                    try {
                        FileOutputStream(targetFile).use { fos ->
                            fos.write(candidate.data)
                            fos.flush()
                        }
                        if (!createdPaths.contains(targetFolder.absolutePath)) {
                            createdPaths.add(targetFolder.absolutePath)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed writing physical record: ${e.message}", e)
                    }

                    // 2. Also register in internal app storage manifest with District & Mouza attributes
                    val saveRes = storageManager.saveDocument(
                        division = candidate.division,
                        district = candidate.district.ifBlank { district },
                        upazila = candidate.upazila,
                        mouza = candidate.mouza.ifBlank { mouza },
                        docType = candidate.docType,
                        khatianOrPlotNo = candidate.khatianOrPlotNo,
                        rawFileName = candidate.fileName,
                        data = candidate.data
                    )
                    when (saveRes) {
                        is SaveResult.Success -> savedCount++
                        is SaveResult.Duplicate -> dupCount++
                        is SaveResult.Error -> {}
                    }

                    // 3. Optional SAF export if user configured custom tree
                    if (docDirService != null && docDirService.isDirectorySelected()) {
                        docDirService.saveDocumentFile(
                            folderPath = listOf(district, mouza, candidate.docType.code),
                            fileName = candidate.fileName,
                            mimeType = "application/pdf",
                            data = candidate.data
                        )
                    }
                }
            }
        }

        OfflineOrganizationSummary(
            totalDistricts = categorized.keys.size,
            totalMouzas = categorized.values.sumOf { it.keys.size },
            savedCount = savedCount,
            duplicateCount = dupCount,
            rootFolderPath = baseDir.absolutePath,
            createdFolderPaths = createdPaths
        )
    }

    /**
     * Updates candidate list and automatically recalculates the categorized folder tree.
     */
    fun setCandidates(newCandidates: List<ImportCandidate>) {
        _candidates.value = newCandidates
        refreshCategorizedFolders()
    }

    /**
     * Refreshes the categorized folders state based on current candidates.
     */
    fun refreshCategorizedFolders() {
        _categorizedFolders.value = categorizeRecordsByDistrictAndMouza(_candidates.value)
    }

    /**
     * Toggle selection for all items in an entire District folder, or a specific Mouza subfolder.
     */
    fun toggleFolderSelection(districtName: String, mouzaName: String? = null, isSelected: Boolean) {
        _candidates.update { list ->
            list.map { candidate ->
                val candidateDistrict = cleanLocationAttribute(candidate.district, DEFAULT_UNASSIGNED_DISTRICT)
                val candidateMouza = cleanLocationAttribute(candidate.mouza, DEFAULT_UNASSIGNED_MOUZA)

                val matches = if (mouzaName != null) {
                    candidateDistrict.equals(districtName, ignoreCase = true) &&
                    candidateMouza.equals(mouzaName, ignoreCase = true)
                } else {
                    candidateDistrict.equals(districtName, ignoreCase = true)
                }

                if (matches) candidate.copy(isSelected = isSelected) else candidate
            }
        }
        refreshCategorizedFolders()
    }

    /**
     * Toggle expanded/collapsed state of a District folder.
     */
    fun toggleDistrictExpanded(districtName: String) {
        _categorizedFolders.update { folders ->
            folders.map { folder ->
                if (folder.districtName.equals(districtName, ignoreCase = true)) {
                    folder.copy(isExpanded = !folder.isExpanded)
                } else folder
            }
        }
    }

    /**
     * Toggle expanded/collapsed state of a Mouza folder inside a District.
     */
    fun toggleMouzaExpanded(districtName: String, mouzaName: String) {
        _categorizedFolders.update { folders ->
            folders.map { folder ->
                if (folder.districtName.equals(districtName, ignoreCase = true)) {
                    val updatedMouzas = folder.mouzaFolders.map { mouzaFolder ->
                        if (mouzaFolder.mouzaName.equals(mouzaName, ignoreCase = true)) {
                            mouzaFolder.copy(isExpanded = !mouzaFolder.isExpanded)
                        } else mouzaFolder
                    }
                    folder.copy(mouzaFolders = updatedMouzas)
                } else folder
            }
        }
    }

    /**
     * Applies a default location (Division, District, Upazila, Mouza) to all candidates,
     * re-categorizing all folders immediately.
     */
    fun applyDefaultLocationToAll(division: String, district: String, upazila: String, mouza: String) {
        _candidates.update { list ->
            list.map { candidate ->
                candidate.copy(
                    division = division.ifBlank { candidate.division },
                    district = district.ifBlank { candidate.district },
                    upazila = upazila.ifBlank { candidate.upazila },
                    mouza = mouza.ifBlank { candidate.mouza }
                )
            }
        }
        refreshCategorizedFolders()
    }

    /**
     * Updates an individual candidate's location attributes and re-categorizes folders.
     */
    fun updateCandidateLocation(
        candidateId: String,
        division: String? = null,
        district: String? = null,
        upazila: String? = null,
        mouza: String? = null
    ) {
        _candidates.update { list ->
            list.map { candidate ->
                if (candidate.id == candidateId) {
                    candidate.copy(
                        division = division ?: candidate.division,
                        district = district ?: candidate.district,
                        upazila = upazila ?: candidate.upazila,
                        mouza = mouza ?: candidate.mouza
                    )
                } else candidate
            }
        }
        refreshCategorizedFolders()
    }

    fun toggleCandidateSelection(candidateId: String, isSelected: Boolean) {
        _candidates.update { list ->
            list.map { if (it.id == candidateId) it.copy(isSelected = isSelected) else it }
        }
        refreshCategorizedFolders()
    }

    fun updateCandidateDocType(candidateId: String, newType: DocumentType) {
        _candidates.update { list ->
            list.map { if (it.id == candidateId) it.copy(docType = newType) else it }
        }
        refreshCategorizedFolders()
    }

    fun updateCandidateKhatian(candidateId: String, newKhatian: String) {
        _candidates.update { list ->
            list.map { if (it.id == candidateId) it.copy(khatianOrPlotNo = newKhatian) else it }
        }
        refreshCategorizedFolders()
    }

    /**
     * Parses an external ZIP file, loads candidates, and automatically categorizes them.
     */
    fun parseZipForImport(
        zipUri: Uri,
        defaultDivision: String,
        defaultDistrict: String,
        defaultUpazila: String,
        defaultMouza: String,
        onComplete: (count: Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _currentFileProcessing.value = "Analyzing ZIP archive..."
            try {
                val parsed = zipManager.parseZipForImport(
                    zipUri = zipUri,
                    defaultDivision = defaultDivision,
                    defaultDistrict = defaultDistrict,
                    defaultUpazila = defaultUpazila,
                    defaultMouza = defaultMouza
                )
                setCandidates(parsed)
                _statusMessage.value = "Found ${parsed.size} documents in ZIP"
                onComplete(parsed.size)
            } catch (e: Exception) {
                _statusMessage.value = "Error parsing ZIP: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Parses a local folder, loads candidates, and automatically categorizes them.
     */
    fun parseFolderForImport(
        folderUri: Uri,
        defaultDivision: String,
        defaultDistrict: String,
        defaultUpazila: String,
        defaultMouza: String,
        onComplete: (count: Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _currentFileProcessing.value = "Scanning folder..."
            try {
                val parsed = zipManager.parseFolderForImport(
                    folderUri = folderUri,
                    defaultDivision = defaultDivision,
                    defaultDistrict = defaultDistrict,
                    defaultUpazila = defaultUpazila,
                    defaultMouza = defaultMouza
                )
                setCandidates(parsed)
                _statusMessage.value = "Found ${parsed.size} documents in folder"
                onComplete(parsed.size)
            } catch (e: Exception) {
                _statusMessage.value = "Error scanning folder: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Saves all selected candidates into the offline archive and categorizes them.
     */
    fun saveAllSelected(
        onComplete: (saved: Int, dups: Int) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val toImport = _candidates.value.filter { it.isSelected }
                val summary = organizeAndSaveImportedRecordsIntoFolders(toImport)
                _candidates.value = emptyList()
                refreshCategorizedFolders()
                _statusMessage.value = "Saved ${summary.savedCount} records across ${summary.totalDistricts} Districts & ${summary.totalMouzas} Mouzas (${summary.duplicateCount} duplicates skipped)"
                onComplete(summary.savedCount, summary.duplicateCount)
            } catch (e: Exception) {
                _statusMessage.value = "Import error: ${e.localizedMessage}"
                onComplete(0, 0)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun cleanLocationAttribute(value: String?, fallback: String): String {
        val trimmed = value?.trim() ?: ""
        return if (trimmed.isNotBlank()) trimmed else fallback
    }

    private fun sanitizePath(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "Unknown" }
    }
}
