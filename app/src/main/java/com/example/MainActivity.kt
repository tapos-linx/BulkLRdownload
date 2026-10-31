package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LRgetBDSection
import com.example.ui.LRgetBDViewModel
import com.example.ui.components.DownloadConfirmationDialog
import com.example.ui.screens.AreaSelectionSection
import com.example.ui.screens.ArchiveAndReportsSection
import com.example.ui.screens.QueueAndTelemetrySection
import com.example.ui.theme.LRgetBDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LRgetBDTheme {
                val context = LocalContext.current
                val vm: LRgetBDViewModel = viewModel(factory = LRgetBDViewModel.Factory(context))
                LRgetBDAppRoot(viewModel = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LRgetBDAppRoot(viewModel: LRgetBDViewModel) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val selectedDivision by viewModel.selectedDivision.collectAsStateWithLifecycle()
    val selectedDistrict by viewModel.selectedDistrict.collectAsStateWithLifecycle()
    val selectedUpazila by viewModel.selectedUpazila.collectAsStateWithLifecycle()
    val mouzas by viewModel.mouzas.collectAsStateWithLifecycle()
    val mouzaSearchQuery by viewModel.mouzaSearchQuery.collectAsStateWithLifecycle()
    val selectedSurveys by viewModel.selectedSurveys.collectAsStateWithLifecycle()
    val selectedRecordTypes by viewModel.selectedRecordTypes.collectAsStateWithLifecycle()
    val selectedFolderLabel by viewModel.selectedFolderLabel.collectAsStateWithLifecycle()
    val showConfirmDialog by viewModel.showDownloadConfirmDialog.collectAsStateWithLifecycle()
    val statusBannerMessageBn by viewModel.statusBannerMessageBn.collectAsStateWithLifecycle()

    val jobs by viewModel.jobsFlow.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogsFlow.collectAsStateWithLifecycle()
    val stats by viewModel.summaryStats.collectAsStateWithLifecycle()
    val isRunning by viewModel.queueEngine.isRunning.collectAsStateWithLifecycle()
    val isPaused by viewModel.queueEngine.isPaused.collectAsStateWithLifecycle()
    val lastActivityMessageBn by viewModel.queueEngine.lastActivityMessageBn.collectAsStateWithLifecycle()

    // Runtime permission launcher for Android 13+ POST_NOTIFICATIONS
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {
            }
        }
    }

    // SAF Directory Picker Launcher so user can save files anywhere in phone memory
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhoneMemoryFolderChosen(uri)
        }
    }

    // SAF CreateDocument Launcher for JSON Queue Export
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        viewModel.exportQueueAsJson(uri)
    }

    // SAF GetContent Launcher for CSV Batch Upload
    val csvUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importCsvBatchFromUri(uri)
        }
    }

    // SAF CreateDocument Launcher for Master ZIP Export
    val masterZipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        viewModel.exportMasterZip(uri)
    }

    if (currentSection != LRgetBDSection.AREA_SELECTION) {
        BackHandler {
            viewModel.navigateToSection(LRgetBDSection.AREA_SELECTION)
        }
    }

    val districts = viewModel.getAvailableDistricts()
    val upazilas = viewModel.getAvailableUpazilas()
    val currentDistrictObj = districts.find { it.name.equals(selectedDistrict, ignoreCase = true) }
    val districtBn = currentDistrictObj?.bnName ?: selectedDistrict
    val upazilaBn = if (selectedUpazila.isNotBlank()) {
        viewModel.locationRepo.getUpazilaDisplayName(selectedUpazila)
    } else {
        "নির্বাচিত উপজেলা"
    }

    if (showConfirmDialog) {
        val selectedMouzaCount = mouzas.count { it.isSelected }
        val totalJobs = selectedMouzaCount * selectedSurveys.size * selectedRecordTypes.size
        DownloadConfirmationDialog(
            districtBn = districtBn,
            upazilaBn = upazilaBn,
            selectedMouzaCount = selectedMouzaCount,
            totalJobsToRun = totalJobs,
            currentFolderLabel = selectedFolderLabel,
            onChooseFolderClick = {
                try {
                    folderPickerLauncher.launch(null)
                } catch (_: Exception) {
                }
            },
            onConfirmDownload = {
                viewModel.confirmAndStartDownloadNow()
            },
            onDismiss = {
                viewModel.dismissDownloadConfirmation()
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "LRgetBD Logo",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LRgetBD — ভূমি রেকর্ড সংগ্রাহক",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "কুমিল্লা ও ব্রাহ্মণবাড়িয়া • অটো-মৌজা কিউ • ফোন মেমোরি আর্কাইভ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentSection == LRgetBDSection.HOME,
                    onClick = { viewModel.navigateToSection(LRgetBDSection.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "মূল পাতা") },
                    label = { Text("মূল পাতা", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentSection == LRgetBDSection.AREA_SELECTION,
                    onClick = { viewModel.navigateToSection(LRgetBDSection.AREA_SELECTION) },
                    icon = { Icon(Icons.Default.Map, contentDescription = "এলাকা নির্বাচন") },
                    label = { Text("এলাকা নির্বাচন", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_area")
                )
                NavigationBarItem(
                    selected = currentSection == LRgetBDSection.QUEUE || currentSection == LRgetBDSection.ACTIVE_WORK,
                    onClick = { viewModel.navigateToSection(LRgetBDSection.QUEUE) },
                    icon = { Icon(Icons.Default.QueuePlayNext, contentDescription = "কার্যসারি") },
                    label = { Text("কার্যসারি (${stats.totalDiscovered})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_queue")
                )
                NavigationBarItem(
                    selected = currentSection == LRgetBDSection.ARCHIVE || currentSection == LRgetBDSection.HISTORY,
                    onClick = { viewModel.navigateToSection(LRgetBDSection.ARCHIVE) },
                    icon = { Icon(Icons.Default.FolderSpecial, contentDescription = "ফাইল ভাণ্ডার") },
                    label = { Text("ফাইল ভাণ্ডার", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_archive")
                )
                NavigationBarItem(
                    selected = currentSection == LRgetBDSection.REPORTS || currentSection == LRgetBDSection.SETTINGS,
                    onClick = { viewModel.navigateToSection(LRgetBDSection.REPORTS) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "প্রতিবেদন ও সেটিংস") },
                    label = { Text("প্রতিবেদন", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_reports")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // All 8 Bangla-first sections accessible via scrollable pill bar as well
            ScrollableTabRow(
                selectedTabIndex = LRgetBDSection.entries.indexOf(currentSection),
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                LRgetBDSection.entries.forEach { section ->
                    Tab(
                        selected = currentSection == section,
                        onClick = { viewModel.navigateToSection(section) },
                        text = {
                            Text(
                                text = section.titleBn,
                                fontWeight = if (currentSection == section) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("top_tab_${section.name.lowercase()}")
                    )
                }
            }

            // Live Notification/Status Banner
            AnimatedVisibility(visible = statusBannerMessageBn.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusBannerMessageBn,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("status_banner_text")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentSection) {
                    LRgetBDSection.HOME -> {
                        HomeOverviewBanner(
                            districtBn = districtBn,
                            upazilaBn = upazilaBn,
                            mouzaCount = mouzas.count { it.isSelected },
                            stats = stats,
                            selectedFolderLabel = selectedFolderLabel,
                            onGoToAreaSelection = { viewModel.navigateToSection(LRgetBDSection.AREA_SELECTION) },
                            onStartCollection = { viewModel.requestStartCollectionWithConfirmation() }
                        )
                        AreaSelectionSection(
                            divisions = viewModel.divisions,
                            selectedDivision = selectedDivision,
                            districts = districts,
                            selectedDistrict = selectedDistrict,
                            upazilas = upazilas,
                            selectedUpazila = selectedUpazila,
                            getUpazilaDisplayName = { viewModel.locationRepo.getUpazilaDisplayName(it) },
                            mouzas = mouzas,
                            mouzaSearchQuery = mouzaSearchQuery,
                            selectedSurveys = selectedSurveys,
                            selectedRecordTypes = selectedRecordTypes,
                            selectedFolderLabel = selectedFolderLabel,
                            catalogueSourceBadge = viewModel.locationRepo.currentSourceType.labelBn,
                            onDivisionSelected = viewModel::onDivisionSelected,
                            onDistrictSelected = viewModel::onDistrictSelected,
                            onUpazilaSelected = { viewModel.onUpazilaSelected(it, triggerAutoQueueAndPrompt = true) },
                            onSearchQueryChanged = viewModel::updateMouzaSearchQuery,
                            onToggleMouza = viewModel::toggleMouzaSelection,
                            onSelectAllMouzas = viewModel::selectAllMouzas,
                            onToggleSurvey = viewModel::toggleSurveyCategory,
                            onToggleRecordType = viewModel::toggleRecordCategory,
                            onPickPhoneFolder = {
                                try {
                                    folderPickerLauncher.launch(null)
                                } catch (_: Exception) {
                                    viewModel.onPhoneMemoryFolderChosen(null)
                                }
                            },
                            onStartCollectionClick = viewModel::requestStartCollectionWithConfirmation,
                            onExportQueueJsonClick = {
                                try {
                                    jsonExportLauncher.launch("lrgetbd_queue_${System.currentTimeMillis()}.json")
                                } catch (_: Exception) {
                                    viewModel.exportQueueAsJson(null)
                                }
                            },
                            onUploadCsvBatchClick = {
                                try {
                                    csvUploadLauncher.launch("text/*")
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }

                    LRgetBDSection.AREA_SELECTION -> {
                        AreaSelectionSection(
                            divisions = viewModel.divisions,
                            selectedDivision = selectedDivision,
                            districts = districts,
                            selectedDistrict = selectedDistrict,
                            upazilas = upazilas,
                            selectedUpazila = selectedUpazila,
                            getUpazilaDisplayName = { viewModel.locationRepo.getUpazilaDisplayName(it) },
                            mouzas = mouzas,
                            mouzaSearchQuery = mouzaSearchQuery,
                            selectedSurveys = selectedSurveys,
                            selectedRecordTypes = selectedRecordTypes,
                            selectedFolderLabel = selectedFolderLabel,
                            catalogueSourceBadge = viewModel.locationRepo.currentSourceType.labelBn,
                            onDivisionSelected = viewModel::onDivisionSelected,
                            onDistrictSelected = viewModel::onDistrictSelected,
                            onUpazilaSelected = { viewModel.onUpazilaSelected(it, triggerAutoQueueAndPrompt = true) },
                            onSearchQueryChanged = viewModel::updateMouzaSearchQuery,
                            onToggleMouza = viewModel::toggleMouzaSelection,
                            onSelectAllMouzas = viewModel::selectAllMouzas,
                            onToggleSurvey = viewModel::toggleSurveyCategory,
                            onToggleRecordType = viewModel::toggleRecordCategory,
                            onPickPhoneFolder = {
                                try {
                                    folderPickerLauncher.launch(null)
                                } catch (_: Exception) {
                                    viewModel.onPhoneMemoryFolderChosen(null)
                                }
                            },
                            onStartCollectionClick = viewModel::requestStartCollectionWithConfirmation,
                            onExportQueueJsonClick = {
                                try {
                                    jsonExportLauncher.launch("lrgetbd_queue_${System.currentTimeMillis()}.json")
                                } catch (_: Exception) {
                                    viewModel.exportQueueAsJson(null)
                                }
                            },
                            onUploadCsvBatchClick = {
                                try {
                                    csvUploadLauncher.launch("text/*")
                                } catch (_: Exception) {
                                    viewModel.navigateToSection(LRgetBDSection.QUEUE)
                                }
                            }
                        )
                        QueueAndTelemetrySection(
                            jobs = jobs,
                            stats = stats,
                            isRunning = isRunning,
                            isPaused = isPaused,
                            lastActivityMessageBn = lastActivityMessageBn,
                            selectedFolderLabel = selectedFolderLabel,
                            isLowStorage = viewModel.storageManager.isLowStorageSpace(),
                            onStartQueue = { viewModel.queueEngine.startQueue() },
                            onPauseQueue = { viewModel.queueEngine.pauseQueue() },
                            onResumeQueue = { viewModel.queueEngine.resumeQueue() },
                            onCancelClearQueue = viewModel::cancelAndClearQueue,
                            onRetryFailedJobs = viewModel::retryFailedAndIncompleteJobs,
                            onRetryAuthJobs = viewModel::retryAuthRequiredJobs,
                            onExportQueueJson = { viewModel.exportQueueAsJson(null) },
                            onUploadCsvBatch = {
                                try {
                                    csvUploadLauncher.launch("text/*")
                                } catch (_: Exception) {
                                }
                            },
                            onPasteCsvSubmit = viewModel::importCsvBatchFromString,
                            onExportMasterZip = { viewModel.exportMasterZip(null) }
                        )
                    }

                    LRgetBDSection.QUEUE, LRgetBDSection.ACTIVE_WORK -> {
                        QueueAndTelemetrySection(
                            jobs = jobs,
                            stats = stats,
                            isRunning = isRunning,
                            isPaused = isPaused,
                            lastActivityMessageBn = lastActivityMessageBn,
                            selectedFolderLabel = selectedFolderLabel,
                            isLowStorage = viewModel.storageManager.isLowStorageSpace(),
                            onStartQueue = { viewModel.queueEngine.startQueue() },
                            onPauseQueue = { viewModel.queueEngine.pauseQueue() },
                            onResumeQueue = { viewModel.queueEngine.resumeQueue() },
                            onCancelClearQueue = viewModel::cancelAndClearQueue,
                            onRetryFailedJobs = viewModel::retryFailedAndIncompleteJobs,
                            onRetryAuthJobs = viewModel::retryAuthRequiredJobs,
                            onExportQueueJson = { viewModel.exportQueueAsJson(null) },
                            onUploadCsvBatch = {
                                try {
                                    csvUploadLauncher.launch("text/*")
                                } catch (_: Exception) {
                                }
                            },
                            onPasteCsvSubmit = viewModel::importCsvBatchFromString,
                            onExportMasterZip = { viewModel.exportMasterZip(null) }
                        )
                    }

                    LRgetBDSection.HISTORY, LRgetBDSection.ARCHIVE, LRgetBDSection.REPORTS, LRgetBDSection.SETTINGS -> {
                        ArchiveAndReportsSection(
                            jobs = jobs,
                            auditLogs = auditLogs,
                            stats = stats,
                            selectedFolderLabel = selectedFolderLabel,
                            adapters = viewModel.queueEngine.availableAdapters,
                            onPickPhoneFolder = {
                                try {
                                    folderPickerLauncher.launch(null)
                                } catch (_: Exception) {
                                    viewModel.onPhoneMemoryFolderChosen(null)
                                }
                            },
                            onExportQueueJson = { viewModel.exportQueueAsJson(null) },
                            onExportMasterZip = {
                                try {
                                    masterZipExportLauncher.launch("LRgetBD_Master_${selectedDistrict}_${selectedUpazila}.zip")
                                } catch (_: Exception) {
                                    viewModel.exportMasterZip(null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeOverviewBanner(
    districtBn: String,
    upazilaBn: String,
    mouzaCount: Int,
    stats: com.example.storage.download.QueueSummaryStats,
    selectedFolderLabel: String,
    onGoToAreaSelection: () -> Unit,
    onStartCollection: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF064E3B), Color(0xFF047857))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LRgetBD — ফ্রি-ফার্স্ট নেটিভ ল্যান্ড রেকর্ড ডাউনলোডার",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "নির্বাচিত এলাকা: $districtBn → $upazilaBn ($mouzaCount টি মৌজা প্রস্তুত)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD1FAE5)
                )
                Text(
                    text = "মোট কিউ: ${stats.totalDiscovered} | যাচাইকৃত: ${stats.validatedCount} | ডুপ্লিকেট: ${stats.duplicateCount} | ব্যর্থ: ${stats.failedCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFEF3C7),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "সংরক্ষণ স্থান: $selectedFolderLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
