package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DocumentType
import com.example.data.LocationRepository
import com.example.storage.ImportCandidate
import com.example.storage.StorageManager
import com.example.storage.ZipManager
import com.example.ui.components.CascadingLocationSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    locationRepo: LocationRepository,
    storageManager: StorageManager,
    zipManager: ZipManager,
    defaultDivision: String,
    defaultDistrict: String,
    defaultUpazila: String,
    defaultMouza: String,
    onDivisionSelected: (String) -> Unit,
    onDistrictSelected: (String) -> Unit,
    onUpazilaSelected: (String) -> Unit,
    onMouzaChanged: (String) -> Unit,
    onImportComplete: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ImportViewModel = remember { ImportViewModel(storageManager, zipManager) }
) {
    val candidates by viewModel.candidates.collectAsState()
    val categorizedFolders by viewModel.categorizedFolders.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val currentFileProcessing by viewModel.currentFileProcessing.collectAsState()
    val isFolderView by viewModel.isFolderView.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseZipForImport(
                zipUri = uri,
                defaultDivision = defaultDivision,
                defaultDistrict = defaultDistrict,
                defaultUpazila = defaultUpazila,
                defaultMouza = defaultMouza
            )
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseFolderForImport(
                folderUri = uri,
                defaultDivision = defaultDivision,
                defaultDistrict = defaultDistrict,
                defaultUpazila = defaultUpazila,
                defaultMouza = defaultMouza
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pick an archive or folder. Records are automatically categorized into offline folders by District and Mouza.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_zip_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_pick_zip))
                    }

                    OutlinedButton(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_folder_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_pick_folder))
                    }
                }
            }
        },
        bottomBar = {
            if (candidates.isNotEmpty()) {
                val selectedCount = candidates.count { it.isSelected }
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$selectedCount of ${candidates.size} selected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${categorizedFolders.size} District & ${categorizedFolders.sumOf { it.mouzaFolders.size }} Mouza folders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.saveAllSelected { saved, _ ->
                                    if (saved > 0) {
                                        onImportComplete()
                                    }
                                }
                            },
                            enabled = selectedCount > 0 && !isProcessing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("confirm_import_button")
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Organize ($selectedCount)")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = currentFileProcessing,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (importProgress > 0f) {
                            LinearProgressIndicator(
                                progress = { importProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else if (candidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No Files Loaded Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose an external ZIP archive or a folder on your phone storage to inspect, categorize into District & Mouza folders, and organize into your offline archive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Batch Hierarchy Assigner Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Apply Default Location to All Items",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            CascadingLocationSelector(
                                locationRepo = locationRepo,
                                selectedDivision = defaultDivision,
                                selectedDistrict = defaultDistrict,
                                selectedUpazila = defaultUpazila,
                                mouza = defaultMouza,
                                onDivisionSelected = { div ->
                                    onDivisionSelected(div)
                                    viewModel.applyDefaultLocationToAll(div, defaultDistrict, defaultUpazila, defaultMouza)
                                },
                                onDistrictSelected = { dist ->
                                    onDistrictSelected(dist)
                                    viewModel.applyDefaultLocationToAll(defaultDivision, dist, defaultUpazila, defaultMouza)
                                },
                                onUpazilaSelected = { upz ->
                                    onUpazilaSelected(upz)
                                    val allMouzas = locationRepo.getMouzasForUpazila(upz)
                                    val allLabel = "All Mouzas (${allMouzas.size} Mouzas)"
                                    onMouzaChanged(allLabel)
                                    viewModel.applyDefaultLocationToAll(defaultDivision, defaultDistrict, upz, allLabel)
                                },
                                onMouzaChanged = { mz ->
                                    onMouzaChanged(mz)
                                    viewModel.applyDefaultLocationToAll(defaultDivision, defaultDistrict, defaultUpazila, mz)
                                }
                            )
                        }
                    }
                }

                // View Mode Switcher Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isFolderView,
                                onClick = { viewModel.setFolderView(true) },
                                label = { Text("Folder View (${categorizedFolders.size} Districts)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_folder_view")
                            )

                            FilterChip(
                                selected = !isFolderView,
                                onClick = { viewModel.setFolderView(false) },
                                label = { Text("Flat List (${candidates.size})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_list_view")
                            )
                        }
                    }
                }

                if (isFolderView) {
                    // Categorized View: District Folders containing Mouza Folders
                    items(categorizedFolders, key = { it.districtName }) { districtFolder ->
                        DistrictFolderCard(
                            districtFolder = districtFolder,
                            onToggleDistrictExpanded = { viewModel.toggleDistrictExpanded(districtFolder.districtName) },
                            onToggleDistrictSelection = { isChecked ->
                                viewModel.toggleFolderSelection(districtFolder.districtName, null, isChecked)
                            },
                            onToggleMouzaExpanded = { mouzaName ->
                                viewModel.toggleMouzaExpanded(districtFolder.districtName, mouzaName)
                            },
                            onToggleMouzaSelection = { mouzaName, isChecked ->
                                viewModel.toggleFolderSelection(districtFolder.districtName, mouzaName, isChecked)
                            },
                            onToggleCandidateSelect = { candidateId, isChecked ->
                                viewModel.toggleCandidateSelection(candidateId, isChecked)
                            },
                            onCandidateDocTypeChange = { candidateId, newType ->
                                viewModel.updateCandidateDocType(candidateId, newType)
                            },
                            onCandidateKhatianChange = { candidateId, newKhatian ->
                                viewModel.updateCandidateKhatian(candidateId, newKhatian)
                            }
                        )
                    }
                } else {
                    // Flat List View
                    items(candidates, key = { it.id }) { candidate ->
                        ImportCandidateCard(
                            candidate = candidate,
                            onToggleSelect = { isChecked ->
                                viewModel.toggleCandidateSelection(candidate.id, isChecked)
                            },
                            onDocTypeChange = { newType ->
                                viewModel.updateCandidateDocType(candidate.id, newType)
                            },
                            onKhatianChange = { newKhatian ->
                                viewModel.updateCandidateKhatian(candidate.id, newKhatian)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card representing a District folder containing Mouza subfolders.
 */
@Composable
fun DistrictFolderCard(
    districtFolder: DistrictFolder,
    onToggleDistrictExpanded: () -> Unit,
    onToggleDistrictSelection: (Boolean) -> Unit,
    onToggleMouzaExpanded: (String) -> Unit,
    onToggleMouzaSelection: (String, Boolean) -> Unit,
    onToggleCandidateSelect: (String, Boolean) -> Unit,
    onCandidateDocTypeChange: (String, DocumentType) -> Unit,
    onCandidateKhatianChange: (String, String) -> Unit
) {
    val allSelectedInDistrict = districtFolder.mouzaFolders.all { m -> m.records.all { it.isSelected } }
    val anySelectedInDistrict = districtFolder.mouzaFolders.any { m -> m.records.any { it.isSelected } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("district_folder_${districtFolder.districtName}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // District Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = allSelectedInDistrict,
                        onCheckedChange = { onToggleDistrictSelection(it) },
                        modifier = Modifier.testTag("checkbox_district_${districtFolder.districtName}")
                    )

                    Icon(
                        imageVector = if (districtFolder.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        Text(
                            text = "District: ${districtFolder.districtName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${districtFolder.totalRecordsCount} records in ${districtFolder.mouzaFolders.size} Mouza folders • ${districtFolder.totalSizeBytes / 1024} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleDistrictExpanded,
                    modifier = Modifier.testTag("expand_district_${districtFolder.districtName}")
                ) {
                    Icon(
                        imageVector = if (districtFolder.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (districtFolder.isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // Mouza Subfolders
            if (districtFolder.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    districtFolder.mouzaFolders.forEach { mouzaFolder ->
                        MouzaFolderCard(
                            mouzaFolder = mouzaFolder,
                            onToggleMouzaExpanded = { onToggleMouzaExpanded(mouzaFolder.mouzaName) },
                            onToggleMouzaSelection = { isChecked ->
                                onToggleMouzaSelection(mouzaFolder.mouzaName, isChecked)
                            },
                            onToggleCandidateSelect = onToggleCandidateSelect,
                            onCandidateDocTypeChange = onCandidateDocTypeChange,
                            onCandidateKhatianChange = onCandidateKhatianChange
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card representing a Mouza folder containing individual land records.
 */
@Composable
fun MouzaFolderCard(
    mouzaFolder: MouzaFolder,
    onToggleMouzaExpanded: () -> Unit,
    onToggleMouzaSelection: (Boolean) -> Unit,
    onToggleCandidateSelect: (String, Boolean) -> Unit,
    onCandidateDocTypeChange: (String, DocumentType) -> Unit,
    onCandidateKhatianChange: (String, String) -> Unit
) {
    val allSelectedInMouza = mouzaFolder.records.all { it.isSelected }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mouza_folder_${mouzaFolder.mouzaName}"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mouza Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = allSelectedInMouza,
                        onCheckedChange = onToggleMouzaSelection,
                        modifier = Modifier.testTag("checkbox_mouza_${mouzaFolder.mouzaName}")
                    )

                    Icon(
                        imageVector = if (mouzaFolder.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )

                    Column {
                        Text(
                            text = "Mouza: ${mouzaFolder.mouzaName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${mouzaFolder.records.size} files • ${mouzaFolder.totalSizeBytes / 1024} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleMouzaExpanded,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (mouzaFolder.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (mouzaFolder.isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Documents in this Mouza
            if (mouzaFolder.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mouzaFolder.records.forEach { candidate ->
                        ImportCandidateCard(
                            candidate = candidate,
                            onToggleSelect = { isChecked ->
                                onToggleCandidateSelect(candidate.id, isChecked)
                            },
                            onDocTypeChange = { newType ->
                                onCandidateDocTypeChange(candidate.id, newType)
                            },
                            onKhatianChange = { newKhatian ->
                                onCandidateKhatianChange(candidate.id, newKhatian)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCandidateCard(
    candidate: ImportCandidate,
    onToggleSelect: (Boolean) -> Unit,
    onDocTypeChange: (DocumentType) -> Unit,
    onKhatianChange: (String) -> Unit
) {
    var expandedTypeMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (candidate.isDuplicate)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = candidate.isSelected,
                onCheckedChange = onToggleSelect,
                modifier = Modifier.testTag("import_checkbox_${candidate.id}")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = candidate.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${candidate.size / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Show current District & Mouza mapping badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "District: ${candidate.district.ifBlank { "Unassigned" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Mouza: ${candidate.mouza.ifBlank { "Unassigned" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (candidate.isDuplicate) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Duplicate (SHA-256 match)",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DocType Dropdown button
                    Box {
                        OutlinedButton(
                            onClick = { expandedTypeMenu = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("${candidate.docType.code} (${candidate.docType.bnLabel})")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = expandedTypeMenu,
                            onDismissRequest = { expandedTypeMenu = false }
                        ) {
                            DocumentType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text("${type.code} - ${type.enLabel} (${type.bnLabel})") },
                                    onClick = {
                                        onDocTypeChange(type)
                                        expandedTypeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = candidate.khatianOrPlotNo,
                        onValueChange = onKhatianChange,
                        placeholder = { Text("Khatian / Plot #") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }
    }
}
