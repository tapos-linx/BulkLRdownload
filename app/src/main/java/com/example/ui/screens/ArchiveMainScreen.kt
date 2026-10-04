package com.example.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.LocationRepository
import com.example.storage.StorageManager
import com.example.storage.ZipManager

enum class ArchiveTab(val labelRes: Int, val icon: ImageVector, val tag: String) {
    MASS(R.string.tab_mass, Icons.Default.CloudDownload, "tab_mass"),
    CAPTURE(R.string.tab_capture, Icons.Default.Public, "tab_capture"),
    RECORDS(R.string.tab_records, Icons.Default.FolderSpecial, "tab_records"),
    IMPORT(R.string.tab_import, Icons.Default.CloudUpload, "tab_import"),
    ABOUT(R.string.tab_about, Icons.Default.HelpOutline, "tab_about")
}

@Composable
fun ArchiveMainScreen() {
    val context = LocalContext.current
    val locationRepo = remember { LocationRepository(context) }
    val storageManager = remember { StorageManager(context) }
    val zipManager = remember { ZipManager(context, storageManager) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(ArchiveTab.MASS) }

    // Shared location state
    var selectedDivision by remember { mutableStateOf("Dhaka") }
    var selectedDistrict by remember { mutableStateOf("Dhaka") }
    var selectedUpazila by remember { mutableStateOf("Savar") }
    val initialMouzas = remember { locationRepo.getMouzasForUpazila("Savar") }
    var mouza by remember { mutableStateOf("All Mouzas (${initialMouzas.size} Mouzas)") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                ArchiveTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                ArchiveTab.MASS -> {
                    MassDownloadScreen(
                        locationRepo = locationRepo,
                        storageManager = storageManager,
                        zipManager = zipManager,
                        onNavigateToRecords = { currentTab = ArchiveTab.RECORDS },
                        onNavigateToBrowser = { currentTab = ArchiveTab.CAPTURE },
                        snackbarHostState = snackbarHostState
                    )
                }
                ArchiveTab.CAPTURE -> {
                    WebViewCaptureScreen(
                        locationRepo = locationRepo,
                        storageManager = storageManager,
                        selectedDivision = selectedDivision,
                        selectedDistrict = selectedDistrict,
                        selectedUpazila = selectedUpazila,
                        mouza = mouza,
                        onDivisionSelected = { div ->
                            selectedDivision = div
                            val districts = locationRepo.getDistricts(div)
                            val firstDist = districts.firstOrNull()?.name ?: ""
                            selectedDistrict = firstDist
                            val firstUpz = locationRepo.getUpazilas(div, firstDist).firstOrNull() ?: ""
                            selectedUpazila = firstUpz
                            val newMouzas = locationRepo.getMouzasForUpazila(firstUpz)
                            mouza = "All Mouzas (${newMouzas.size} Mouzas)"
                        },
                        onDistrictSelected = { dist ->
                            selectedDistrict = dist
                            val firstUpz = locationRepo.getUpazilas(selectedDivision, dist).firstOrNull() ?: ""
                            selectedUpazila = firstUpz
                            val newMouzas = locationRepo.getMouzasForUpazila(firstUpz)
                            mouza = "All Mouzas (${newMouzas.size} Mouzas)"
                        },
                        onUpazilaSelected = { upz ->
                            selectedUpazila = upz
                            val newMouzas = locationRepo.getMouzasForUpazila(upz)
                            mouza = "All Mouzas (${newMouzas.size} Mouzas)"
                        },
                        onMouzaChanged = { mz -> mouza = mz },
                        onRecordSaved = { /* Trigger refresh */ },
                        snackbarHostState = snackbarHostState
                    )
                }
                ArchiveTab.RECORDS -> {
                    RecordsScreen(
                        storageManager = storageManager,
                        locationRepo = locationRepo,
                        onNavigateToCapture = { currentTab = ArchiveTab.CAPTURE },
                        onNavigateToImport = { currentTab = ArchiveTab.IMPORT },
                        snackbarHostState = snackbarHostState
                    )
                }
                ArchiveTab.IMPORT -> {
                    ImportScreen(
                        locationRepo = locationRepo,
                        storageManager = storageManager,
                        zipManager = zipManager,
                        defaultDivision = selectedDivision,
                        defaultDistrict = selectedDistrict,
                        defaultUpazila = selectedUpazila,
                        defaultMouza = mouza,
                        onDivisionSelected = { div ->
                            selectedDivision = div
                            val districts = locationRepo.getDistricts(div)
                            val firstDist = districts.firstOrNull()?.name ?: ""
                            selectedDistrict = firstDist
                            val firstUpz = locationRepo.getUpazilas(div, firstDist).firstOrNull() ?: ""
                            selectedUpazila = firstUpz
                            val newMouzas = locationRepo.getMouzasForUpazila(firstUpz)
                            mouza = "All Mouzas (${newMouzas.size} Mouzas)"
                        },
                        onDistrictSelected = { dist ->
                            selectedDistrict = dist
                            val firstUpz = locationRepo.getUpazilas(selectedDivision, dist).firstOrNull() ?: ""
                            selectedUpazila = firstUpz
                            val newMouzas = locationRepo.getMouzasForUpazila(firstUpz)
                            mouza = "All Mouzas (${newMouzas.size} Mouzas)"
                        },
                        onUpazilaSelected = { upz ->
                            selectedUpazila = upz
                            val newMouzas = locationRepo.getMouzasForUpazila(upz)
                            mouza = "All Mouzas (${newMouzas.size} Mouzas)"
                        },
                        onMouzaChanged = { mz -> mouza = mz },
                        onImportComplete = { currentTab = ArchiveTab.RECORDS },
                        snackbarHostState = snackbarHostState
                    )
                }
                ArchiveTab.ABOUT -> {
                    AboutScreen()
                }
            }
        }
    }
}
