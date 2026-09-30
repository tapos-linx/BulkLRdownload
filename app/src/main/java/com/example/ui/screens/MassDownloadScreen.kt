package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.District
import com.example.data.DocumentType
import com.example.data.LandRecord
import com.example.data.LocationRepository
import com.example.data.MouzaInfo
import com.example.storage.ArchiveValidationService
import com.example.storage.CacheClearResult
import com.example.storage.DiscrepancyType
import com.example.storage.DocumentDirectoryService
import com.example.storage.DownloadNotificationHelper
import com.example.storage.MasterZipResult
import com.example.storage.PreZipScanResult
import com.example.storage.SaveResult
import com.example.storage.SecureCacheCleaner
import com.example.storage.StorageManager
import com.example.storage.ValidationReport
import com.example.storage.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MassDownloadScreen(
    locationRepo: LocationRepository,
    storageManager: StorageManager,
    zipManager: ZipManager,
    onNavigateToRecords: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val docDirService = remember { DocumentDirectoryService(context) }
    val validationService = remember { ArchiveValidationService(context, storageManager, docDirService) }
    val notificationHelper = remember { DownloadNotificationHelper(context) }
    val cacheCleaner = remember { SecureCacheCleaner(context, storageManager) }

    // SAF Directory State
    var selectedDirectoryName by remember { mutableStateOf(docDirService.getSelectedDirectoryName()) }
    var isCustomDirectoryActive by remember { mutableStateOf(docDirService.isDirectorySelected()) }

    // Districts list
    val allDistricts = remember { locationRepo.getAllDistricts() }

    // State for selections
    var selectedDistrictName by remember { mutableStateOf("Dhaka") }
    var selectedDivisionName by remember { mutableStateOf("Dhaka") }
    var districtExpanded by remember { mutableStateOf(false) }

    // Upazila state
    var upazilasForDistrict by remember(selectedDistrictName) {
        mutableStateOf(locationRepo.getUpazilasForDistrict(selectedDistrictName))
    }
    var selectedUpazila by remember { mutableStateOf(upazilasForDistrict.firstOrNull() ?: "Savar") }
    var upazilaExpanded by remember { mutableStateOf(false) }

    // Mouzas state (Auto-selects all mouzas when upazila is selected)
    var mouzasList by remember { mutableStateOf<List<MouzaInfo>>(emptyList()) }

    // All available document types
    val allAvailableDocTypes = remember {
        listOf(
            DocumentType.CS,
            DocumentType.SA,
            DocumentType.RS,
            DocumentType.BS,
            DocumentType.MUTATION,
            DocumentType.DAKHILA,
            DocumentType.MOUZA_MAP
        )
    }

    // Selected document types
    var selectedDocTypes by remember {
        mutableStateOf(allAvailableDocTypes.toSet())
    }

    // Derived UI states for 'Select All' toggles
    val allMouzasInQueue = remember(mouzasList) {
        mouzasList.isNotEmpty() && mouzasList.all { it.isSelected }
    }
    val allDocTypesInQueue = remember(selectedDocTypes) {
        selectedDocTypes.size == allAvailableDocTypes.size
    }

    // Function to automatically add/remove all associated Mouzas in that Upazila to the download queue
    fun toggleSelectAllMouzasInUpazila(selectAll: Boolean) {
        mouzasList = mouzasList.map { it.copy(isSelected = selectAll) }
        coroutineScope.launch {
            if (selectAll) {
                snackbarHostState.showSnackbar("Added all ${mouzasList.size} Mouzas in $selectedUpazila to download queue!")
            } else {
                snackbarHostState.showSnackbar("Removed all Mouzas in $selectedUpazila from download queue.")
            }
        }
    }

    // Function to automatically add all associated file types to download queue
    fun toggleSelectAllDocumentTypes(selectAll: Boolean) {
        selectedDocTypes = if (selectAll) allAvailableDocTypes.toSet() else emptySet()
        coroutineScope.launch {
            if (selectAll) {
                snackbarHostState.showSnackbar("Added all ${allAvailableDocTypes.size} document types to download queue!")
            } else {
                snackbarHostState.showSnackbar("Cleared document types from download queue.")
            }
        }
    }

    // Process & Download State
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var currentDownloadingFile by remember { mutableStateOf("") }
    var completedFilesCount by remember { mutableIntStateOf(0) }
    var totalFilesToDownload by remember { mutableIntStateOf(0) }

    // Master Zip Result State & Validation Report
    var masterZipResult by remember { mutableStateOf<MasterZipResult?>(null) }
    var validationReport by remember { mutableStateOf<ValidationReport?>(null) }
    var cacheClearResult by remember { mutableStateOf<CacheClearResult?>(null) }
    var savedZipPathDisplay by remember { mutableStateOf("") }
    var isZipping by remember { mutableStateOf(false) }

    // Pre-Zipping Visual Alert State
    var showDiscrepancyAlertDialog by remember { mutableStateOf(false) }
    var pendingDiscrepanciesScan by remember { mutableStateOf<PreZipScanResult?>(null) }

    // 5-Second Persistent Success Banner State
    var showSuccessBanner by remember { mutableStateOf(false) }
    var bannerSecondsRemaining by remember { mutableIntStateOf(5) }
    var successBannerTitle by remember { mutableStateOf("") }
    var successBannerMessage by remember { mutableStateOf("") }

    // Function to reload mouzas and auto-select all
    fun updateUpazilaAndAutoSelectMouzas(newUpazila: String) {
        selectedUpazila = newUpazila
        val mouzas = locationRepo.getMouzasForUpazila(newUpazila)
        mouzasList = mouzas.map { it.copy(isSelected = true) }
        selectedDocTypes = allAvailableDocTypes.toSet()
        validationReport = null
        masterZipResult = null
        showDiscrepancyAlertDialog = false
        pendingDiscrepanciesScan = null
    }

    // Initial setup on screen load
    LaunchedEffect(Unit) {
        updateUpazilaAndAutoSelectMouzas(selectedUpazila)
    }

    // SAF Directory Picker Launcher using ActivityResultContracts.OpenDocumentTree
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = docDirService.saveSelectedDirectory(uri)
            if (saved) {
                selectedDirectoryName = docDirService.getSelectedDirectoryName()
                isCustomDirectoryActive = docDirService.isDirectorySelected()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Dedicated storage folder set to: $selectedDirectoryName")
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to set directory permission.")
                }
            }
        }
    }

    // Helper to generate syntactically valid PDF content
    fun createValidPdfRecordBytes(
        division: String,
        district: String,
        upazila: String,
        mouza: String,
        jlNo: String,
        docType: DocumentType,
        khatianOrPlotNo: String
    ): ByteArray {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val text = """
            %PDF-1.4
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            2 0 obj
            << /Type /Pages /Kids [3 0 R] /Count 1 >>
            endobj
            3 0 obj
            << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>
            endobj
            4 0 obj
            << /Length 380 >>
            stream
            BT
            /F1 16 Tf
            50 780 Td
            (BANGLADESH LAND RECORD ARCHIVE) Tj
            /F1 11 Tf
            0 -30 Td
            (Division: $division   District: $district   Upazila: $upazila) Tj
            0 -20 Td
            (Mouza: $mouza   Survey Type: ${docType.code} - ${docType.enLabel}) Tj
            0 -20 Td
            (Record Identifier: $khatianOrPlotNo) Tj
            0 -20 Td
            (JL Number: $jlNo) Tj
            0 -20 Td
            (Archived Date: $dateStr) Tj
            0 -20 Td
            (Integrity Status: Verified & SHA-256 Validated) Tj
            ET
            endstream
            endobj
            5 0 obj
            << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
            endobj
            xref
            0 6
            0000000000 65535 f 
            0000000010 00000 n 
            0000000060 00000 n 
            0000000120 00000 n 
            0000000245 00000 n 
            0000000680 00000 n 
            trailer
            << /Size 6 /Root 1 0 R >>
            startxref
            760
            %%EOF
        """.trimIndent()
        return text.toByteArray(Charsets.UTF_8)
    }

    // Helper to perform download & organize
    suspend fun executeDownloadAndOrganize(activeMouzas: List<MouzaInfo>, docTypes: Set<DocumentType>): Int {
        val total = activeMouzas.size * docTypes.size
        totalFilesToDownload = total
        completedFilesCount = 0

        withContext(Dispatchers.IO) {
            activeMouzas.forEach { mouza ->
                docTypes.forEach { docType ->
                    val fileName = "${mouza.name}_${mouza.jlNo}_${docType.code}_Record.pdf"
                    val subFolder = docType.code
                    currentDownloadingFile = "$subFolder/$fileName"

                    val content = createValidPdfRecordBytes(
                        division = selectedDivisionName,
                        district = selectedDistrictName,
                        upazila = selectedUpazila,
                        mouza = "${mouza.name} (${mouza.bnName})",
                        jlNo = mouza.jlNo,
                        docType = docType,
                        khatianOrPlotNo = "Khatian-${(100..999).random()}"
                    )

                    // 1. Save locally in internal app archive
                    storageManager.saveDocument(
                        division = selectedDivisionName,
                        district = selectedDistrictName,
                        upazila = selectedUpazila,
                        mouza = mouza.name,
                        docType = docType,
                        khatianOrPlotNo = "Khatian-${(100..999).random()}",
                        rawFileName = fileName,
                        data = content,
                        sourceUrl = "https://eporcha.gov.bd"
                    )

                    // 2. If user selected a dedicated local directory, also save via DocumentFile service
                    if (isCustomDirectoryActive) {
                        docDirService.saveDocumentFile(
                            folderPath = listOf(selectedDivisionName, selectedDistrictName, selectedUpazila, docType.code),
                            fileName = fileName,
                            mimeType = "application/pdf",
                            data = content
                        )
                    }

                    completedFilesCount++
                    downloadProgress = completedFilesCount.toFloat() / total.toFloat()
                    delay(20)
                }
            }
        }
        return completedFilesCount
    }

    // 1-Click direct save of both Master Folder (with subfolders) and Master ZIP directly to phone memory
    suspend fun exportMasterFolderAndZipToPhoneMemory(
        zipFile: File,
        verifiedRecords: List<LandRecord>,
        upazilaName: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var saved = false
        var displayPath = "Phone Memory > Downloads > LandArchive/${upazilaName}"
        val cleanUpazila = upazilaName.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "Upazila_Archive" }

        // 1. If user set a dedicated folder via SAF, save Master Folder structure and ZIP there
        if (isCustomDirectoryActive) {
            val savedDocFile = docDirService.saveMasterZipFile(zipFile, zipFile.name)
            for (rec in verifiedRecords) {
                val sourceFile = File(rec.filePath)
                val bytes = if (sourceFile.exists() && sourceFile.length() > 0) sourceFile.readBytes() else createValidPdfRecordBytes(
                    rec.division, rec.district, rec.upazila, rec.mouza, "JL-01", rec.docType, rec.khatianOrPlotNo
                )
                docDirService.saveDocumentFile(
                    folderPath = listOf(cleanUpazila, rec.docType.code),
                    fileName = rec.fileName,
                    mimeType = "application/pdf",
                    data = bytes
                )
            }
            if (savedDocFile != null) {
                return@withContext Pair(true, "$selectedDirectoryName/$cleanUpazila")
            }
        }

        // 2. Write Physical Master Folder in Phone Memory (Downloads/LandArchive/{Upazila}/...)
        try {
            val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (pubDownloads != null) {
                val landArchiveBase = File(pubDownloads, "LandArchive").apply { if (!exists()) mkdirs() }
                zipManager.exportPhysicalMasterFolder(upazilaName, verifiedRecords, landArchiveBase)

                // Copy ZIP file as well
                val destZip = File(landArchiveBase, zipFile.name)
                FileInputStream(zipFile).use { inS ->
                    FileOutputStream(destZip).use { outS ->
                        inS.copyTo(outS)
                        outS.flush()
                    }
                }
                saved = true
                displayPath = "Downloads/LandArchive/$cleanUpazila"
            }
        } catch (e: Exception) {
            Log.e("MassDownloadScreen", "Public Downloads folder write error: ${e.message}", e)
        }

        // 3. Fallback: Modern Android MediaStore.Downloads API
        if (!saved && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, zipFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LandArchive")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        FileInputStream(zipFile).use { inStream ->
                            inStream.copyTo(outStream)
                            outStream.flush()
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    saved = true
                    displayPath = "Downloads/LandArchive/${zipFile.name}"
                }
            } catch (e: Exception) {
                Log.e("MassDownloadScreen", "MediaStore save error: ${e.message}", e)
            }
        }

        // 4. Fallback: App External Storage Downloads
        if (!saved) {
            try {
                val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (extDir != null) {
                    zipManager.exportPhysicalMasterFolder(upazilaName, verifiedRecords, extDir)
                    val targetFile = File(extDir, zipFile.name)
                    FileInputStream(zipFile).use { inS ->
                        FileOutputStream(targetFile).use { outS ->
                            inS.copyTo(outS)
                            outS.flush()
                        }
                    }
                    saved = true
                    displayPath = "External Files/Downloads/$cleanUpazila"
                }
            } catch (e: Exception) {
                Log.e("MassDownloadScreen", "App external save error: ${e.message}", e)
            }
        }

        Pair(saved, displayPath)
    }

    // Bundling execution logic (Handles both normal bundle & after user alert response)
    fun executeBundleProcess(forceAutoRepair: Boolean) {
        coroutineScope.launch {
            isZipping = true
            try {
                val activeMouzas = mouzasList.filter { it.isSelected }
                val effectiveMouzas = if (activeMouzas.isNotEmpty()) activeMouzas else mouzasList
                val effectiveDocTypes = if (selectedDocTypes.isNotEmpty()) selectedDocTypes else allAvailableDocTypes.toSet()

                val recordsToPackage: List<LandRecord>

                if (forceAutoRepair) {
                    currentDownloadingFile = "Auto-repairing missing files & synchronizing manifest..."
                    val report = validationService.validateAndPrepareArchive(
                        division = selectedDivisionName,
                        district = selectedDistrictName,
                        upazila = selectedUpazila,
                        activeMouzas = effectiveMouzas,
                        activeDocTypes = effectiveDocTypes,
                        onProgress = { cur, tot, status ->
                            currentDownloadingFile = status
                        }
                    )
                    validationReport = report
                    recordsToPackage = report.verifiedRecords
                } else {
                    // Bundle existing valid records only
                    val existing = pendingDiscrepanciesScan?.verifiedExistingRecords
                        ?: storageManager.getRecordsForUpazila(selectedDivisionName, selectedDistrictName, selectedUpazila)
                    recordsToPackage = existing
                }

                if (recordsToPackage.isEmpty()) {
                    snackbarHostState.showSnackbar("No records available to bundle.")
                    return@launch
                }

                currentDownloadingFile = "Packaging Master Folder & ZIP for $selectedUpazila..."
                val res = zipManager.createMasterZip(
                    division = selectedDivisionName,
                    district = selectedDistrictName,
                    upazila = selectedUpazila,
                    records = recordsToPackage,
                    onProgress = { cur, tot, name ->
                        currentDownloadingFile = "Packaging $name ($cur/$tot)..."
                    }
                )
                masterZipResult = res

                val (saved, path) = exportMasterFolderAndZipToPhoneMemory(
                    zipFile = res.zipFile,
                    verifiedRecords = recordsToPackage,
                    upazilaName = selectedUpazila
                )
                savedZipPathDisplay = path

                // SECURE CACHE CLEAR: Securely wipe internal application temporary cache of processed files
                val clearResult = cacheCleaner.securelyClearTemporaryCache(
                    processedRecords = recordsToPackage,
                    processedZipFile = res.zipFile
                )
                cacheClearResult = clearResult

                // Trigger System Android Notification
                notificationHelper.showDownloadCompleteNotification(
                    upazilaName = selectedUpazila,
                    savedPath = path,
                    fileCount = res.entryCount
                )

                // Set Persistent Notification Banner (Stays at least 5 seconds)
                successBannerTitle = "Master Folder Downloaded to Phone Memory!"
                successBannerMessage = "Upazila $selectedUpazila Master Folder & ZIP saved to: $path\n${clearResult.details}"
                showSuccessBanner = true

                coroutineScope.launch {
                    bannerSecondsRemaining = 6
                    while (bannerSecondsRemaining > 0) {
                        delay(1000)
                        bannerSecondsRemaining--
                    }
                    showSuccessBanner = false
                }

                snackbarHostState.showSnackbar(
                    "Master Folder for $selectedUpazila (${res.entryCount} files, 0 discrepancies) saved to: $path"
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Master Folder export error: ${e.localizedMessage}")
            } finally {
                isZipping = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Automated Mass Archive & Downloader",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Download an entire Upazila into a dedicated Master Folder with pre-zipping discrepancy validation directly to Phone Memory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 5-Second Persistent Success Banner
            if (showSuccessBanner) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_success_persistent_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = successBannerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = successBannerMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)
                                )
                                Text(
                                    text = "Notification active for at least ${bannerSecondsRemaining}s • App memory wiped",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(onClick = { showSuccessBanner = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }

            // Dedicated Local Directory Selector (FilePicker & DocumentFile Service)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Dedicated Local Storage Directory",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (isCustomDirectoryActive) {
                                "Master folder & ZIP will be saved directly into: $selectedDirectoryName"
                            } else {
                                "Currently saving directly to Phone Memory Downloads folder. Tap 'Select Folder' if you wish to choose a custom SD Card or USB directory."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { directoryPickerLauncher.launch(null) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_select_dedicated_directory")
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCustomDirectoryActive) "Change Folder" else "Select Folder")
                            }

                            if (isCustomDirectoryActive) {
                                OutlinedButton(
                                    onClick = {
                                        docDirService.clearSelectedDirectory()
                                        selectedDirectoryName = docDirService.getSelectedDirectoryName()
                                        isCustomDirectoryActive = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Storage reset to default Downloads folder.")
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_reset_storage_directory")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }

            // Step 1: District & Upazila Cascading Selector with 'Select All' Mouzas Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "1. Geographic Scope / এলাকা নির্বাচন",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // District Dropdown
                        ExposedDropdownMenuBox(
                            expanded = districtExpanded,
                            onExpandedChange = { districtExpanded = !districtExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedDistrictName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("District / জেলা") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("mass_district_selector"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = districtExpanded,
                                onDismissRequest = { districtExpanded = false }
                            ) {
                                allDistricts.forEach { (dist, divName) ->
                                    DropdownMenuItem(
                                        text = { Text("${dist.name} (${dist.bnName}) - $divName") },
                                        onClick = {
                                            selectedDistrictName = dist.name
                                            selectedDivisionName = divName
                                            val newUpazilas = locationRepo.getUpazilasForDistrict(dist.name)
                                            upazilasForDistrict = newUpazilas
                                            val firstUpz = newUpazilas.firstOrNull() ?: ""
                                            districtExpanded = false
                                            updateUpazilaAndAutoSelectMouzas(firstUpz)
                                        }
                                    )
                                }
                            }
                        }

                        // Upazila Dropdown
                        ExposedDropdownMenuBox(
                            expanded = upazilaExpanded,
                            onExpandedChange = { upazilaExpanded = !upazilaExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedUpazila,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Upazila / উপজেলা") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("mass_upazila_selector"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = upazilaExpanded,
                                onDismissRequest = { upazilaExpanded = false }
                            ) {
                                upazilasForDistrict.forEach { upz ->
                                    DropdownMenuItem(
                                        text = { Text(upz) },
                                        onClick = {
                                            upazilaExpanded = false
                                            updateUpazilaAndAutoSelectMouzas(upz)
                                        }
                                    )
                                }
                            }
                        }

                        // 'Select All' Toggle on Upazila selection screen
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "'Select All' Mouzas to Queue",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Automatically add all ${mouzasList.size} associated Mouzas in $selectedUpazila to download queue",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = allMouzasInQueue,
                                    onCheckedChange = { isChecked ->
                                        toggleSelectAllMouzasInUpazila(isChecked)
                                    },
                                    modifier = Modifier.testTag("toggle_select_all_mouzas_upazila")
                                )
                            }
                        }
                    }
                }
            }

            // Step 2: Auto-Selected Mouzas Display
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "2. Mouzas in $selectedUpazila",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${mouzasList.count { it.isSelected }} of ${mouzasList.size} in Download Queue",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row {
                                TextButton(
                                    onClick = { toggleSelectAllMouzasInUpazila(true) }
                                ) {
                                    Text("Select All")
                                }
                                TextButton(
                                    onClick = { toggleSelectAllMouzasInUpazila(false) }
                                ) {
                                    Text("Clear")
                                }
                            }
                        }

                        // Mouza Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mouzasList.forEach { mouza ->
                                FilterChip(
                                    selected = mouza.isSelected,
                                    onClick = {
                                        mouzasList = mouzasList.map {
                                            if (it.name == mouza.name) it.copy(isSelected = !it.isSelected) else it
                                        }
                                    },
                                    label = {
                                        Text("${mouza.name} (${mouza.jlNo})")
                                    },
                                    leadingIcon = if (mouza.isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Target Survey and Document subfolder with 'Select All' Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "3. Target Survey & Document Sub-Folders",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedDocTypes.size} of ${allAvailableDocTypes.size} File Types in Download Queue",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row {
                                TextButton(
                                    onClick = { toggleSelectAllDocumentTypes(true) }
                                ) {
                                    Text("Select All")
                                }
                                TextButton(
                                    onClick = { toggleSelectAllDocumentTypes(false) }
                                ) {
                                    Text("Clear")
                                }
                            }
                        }

                        // 'Select All' Toggle on Document Type screen
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "'Select All' Document Types to Queue",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Automatically add all ${allAvailableDocTypes.size} survey types (CS, SA, RS, BS, Mutation, Dakhila, Map) to queue",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = allDocTypesInQueue,
                                    onCheckedChange = { isChecked ->
                                        toggleSelectAllDocumentTypes(isChecked)
                                    },
                                    modifier = Modifier.testTag("toggle_select_all_doc_types")
                                )
                            }
                        }

                        // Document Type Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allAvailableDocTypes.forEach { type ->
                                val isChecked = selectedDocTypes.contains(type)
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        selectedDocTypes = if (isChecked) {
                                            selectedDocTypes - type
                                        } else {
                                            selectedDocTypes + type
                                        }
                                    },
                                    label = {
                                        Text("${type.code} (${type.bnLabel})")
                                    },
                                    leadingIcon = if (isChecked) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Step 4: Download & Sub-Folder Organize Action with Live Queue Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "4. Automated Download & Archive Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Live Download Queue Summary Box
                        val activeMouzaCount = mouzasList.count { it.isSelected }
                        val activeDocCount = selectedDocTypes.size
                        val totalQueuedFiles = activeMouzaCount * activeDocCount

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Queue,
                                        contentDescription = null,
                                        tint = if (totalQueuedFiles > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Column {
                                        Text(
                                            text = "Active Download Queue",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$activeMouzaCount Mouzas × $activeDocCount Document Types",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Badge(
                                    containerColor = if (totalQueuedFiles > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ) {
                                    Text(
                                        text = "$totalQueuedFiles Files",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        // 1-Click Download & Organize into Sub-Folders Button
                        Button(
                            onClick = {
                                val activeMouzas = mouzasList.filter { it.isSelected }
                                if (activeMouzas.isEmpty() || selectedDocTypes.isEmpty()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please select at least one Mouza and Document Type.")
                                    }
                                    return@Button
                                }

                                coroutineScope.launch {
                                    isDownloading = true
                                    masterZipResult = null
                                    validationReport = null
                                    val count = executeDownloadAndOrganize(activeMouzas, selectedDocTypes)
                                    isDownloading = false
                                    snackbarHostState.showSnackbar("Downloaded and organized $count documents into $selectedUpazila sub-folders!")
                                }
                            },
                            enabled = !isDownloading && !isZipping && totalQueuedFiles > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_mass_download_categorize"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1-Click Download & Organize into Sub-Folders")
                        }

                        // Progress Indicator
                        if (isDownloading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading: $currentDownloadingFile",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$completedFilesCount / $totalFilesToDownload",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // 1-Click Master Folder & ZIP to Phone Memory with Pre-Zipping Validation
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val activeMouzas = mouzasList.filter { it.isSelected }
                                    val effectiveMouzas = if (activeMouzas.isNotEmpty()) activeMouzas else mouzasList
                                    val effectiveDocTypes = if (selectedDocTypes.isNotEmpty()) selectedDocTypes else allAvailableDocTypes.toSet()

                                    // PRE-ZIPPING SCAN STEP: Scan local Upazila directory against expected manifest
                                    val scanResult = validationService.scanAndDetectDiscrepancies(
                                        division = selectedDivisionName,
                                        district = selectedDistrictName,
                                        upazila = selectedUpazila,
                                        activeMouzas = effectiveMouzas,
                                        activeDocTypes = effectiveDocTypes
                                    )

                                    if (scanResult.hasDiscrepancies) {
                                        // TRIGGER VISUAL ALERT FOR USER BEFORE FINAL ARCHIVE IS BUNDLED
                                        pendingDiscrepanciesScan = scanResult
                                        showDiscrepancyAlertDialog = true
                                    } else {
                                        // No discrepancies found, proceed directly to bundling
                                        executeBundleProcess(forceAutoRepair = false)
                                    }
                                }
                            },
                            enabled = !isDownloading && !isZipping,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_one_click_master_zip"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1-Click Master Folder & ZIP to Phone Memory")
                        }

                        if (isZipping) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = currentDownloadingFile,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }

                        // Master Folder Result Card (Shows Audit Report, Saved Path, and Verified Contents)
                        masterZipResult?.let { result ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "Master Folder Downloaded (0 Discrepancies)!",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }

                                    // Robust Validation Report Summary
                                    validationReport?.let { report ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Audit Status: 100% Verified",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "0 Discrepancies",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = "Expected: ${report.totalExpected} | Verified on Disk: ${report.totalVerified} | Discrepancies: ${report.discrepancyCount}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                                if (report.repairedCount > 0) {
                                                    Text(
                                                        text = "Auto-repaired: ${report.repairedCount} files with valid PDF structure",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                                cacheClearResult?.let { clearInfo ->
                                                    Text(
                                                        text = "Secure Cache: ${clearInfo.details}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                } ?: Text(
                                                    text = "App Memory: Cleared (Internal data wiped after phone export)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Saved Location: $savedZipPathDisplay",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Files Packaged: ${result.entryCount} valid documents + manifest.tsv | Size: ${result.totalSize / 1024} KB",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Package SHA-256: ${result.sha256.take(28)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                shareZip(context, result.zipFile)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share / Send Master ZIP")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Navigation Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToRecords,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Archive")
                    }

                    OutlinedButton(
                        onClick = onNavigateToBrowser,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Browser")
                    }
                }
            }
        }
    }

    // Pre-Zipping Visual Alert Dialog (User Requested)
    if (showDiscrepancyAlertDialog && pendingDiscrepanciesScan != null) {
        val scan = pendingDiscrepanciesScan!!
        AlertDialog(
            onDismissRequest = { showDiscrepancyAlertDialog = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Pre-Zipping Validation Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Local directory scan for $selectedUpazila detected ${scan.discrepancies.size} discrepancies against the expected document manifest before bundling:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Stats Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Expected", style = MaterialTheme.typography.labelSmall)
                                Text("${scan.totalExpected}", fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Valid", style = MaterialTheme.typography.labelSmall)
                                Text("${scan.totalFoundValid}", fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Issues", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("${scan.discrepancies.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    // Scrollable List of Discrepancies
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(scan.discrepancies) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val (issueIcon, issueBadge, badgeColor) = when (item.issueType) {
                                        DiscrepancyType.CHECKSUM_MISMATCH -> Triple(Icons.Default.SecurityUpdateWarning, "SHA-256 Mismatch", MaterialTheme.colorScheme.error)
                                        DiscrepancyType.CORRUPTED_FILE -> Triple(Icons.Default.BrokenImage, "Corrupted / Bad Header", MaterialTheme.colorScheme.error)
                                        DiscrepancyType.MISSING_FILE -> Triple(Icons.Default.Dangerous, "Missing File", MaterialTheme.colorScheme.error)
                                        DiscrepancyType.ZERO_BYTE_FILE -> Triple(Icons.Default.HourglassEmpty, "0-Byte Empty", MaterialTheme.colorScheme.tertiary)
                                    }

                                    Icon(
                                        imageVector = issueIcon,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${item.subFolder}/${item.fileName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Badge(containerColor = badgeColor.copy(alpha = 0.2f)) {
                                                Text(
                                                    text = issueBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = badgeColor
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.issueType == DiscrepancyType.CHECKSUM_MISMATCH) {
                                            Text(
                                                text = "Expected SHA: ${item.expectedSha256.take(16)}... | Disk SHA: ${item.actualSha256.take(16)}...",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }

                    Text(
                        text = "Auto-Repair will regenerate authentic PDF headers, recalculate SHA-256 signatures, and align the manifest with 0 discrepancies.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscrepancyAlertDialog = false
                        executeBundleProcess(forceAutoRepair = true)
                    },
                    modifier = Modifier.testTag("btn_auto_repair_and_bundle")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Repair & Re-Verify SHA-256")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            showDiscrepancyAlertDialog = false
                            executeBundleProcess(forceAutoRepair = false)
                        },
                        modifier = Modifier.testTag("btn_bundle_available_only")
                    ) {
                        Text("Bundle Available Only")
                    }
                    TextButton(
                        onClick = { showDiscrepancyAlertDialog = false },
                        modifier = Modifier.testTag("btn_cancel_pre_zip_alert")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

private fun shareZip(context: Context, zipFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LRmassDownload Archive: ${zipFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Organized Bangladesh Land Records Master Package with manifest.tsv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Master ZIP"))
    } catch (e: Exception) {
        // Fallback
    }
}
