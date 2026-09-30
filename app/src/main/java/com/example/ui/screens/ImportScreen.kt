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
import com.example.storage.SaveResult
import com.example.storage.StorageManager
import com.example.storage.ZipManager
import com.example.ui.components.CascadingLocationSelector
import kotlinx.coroutines.launch

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
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<ImportCandidate>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var importProgress by remember { mutableFloatStateOf(0f) }
    var currentFileProcessing by remember { mutableStateOf("") }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                currentFileProcessing = "Analyzing ZIP archive..."
                try {
                    val parsed = zipManager.parseZipForImport(
                        zipUri = uri,
                        defaultDivision = defaultDivision,
                        defaultDistrict = defaultDistrict,
                        defaultUpazila = defaultUpazila,
                        defaultMouza = defaultMouza
                    )
                    candidates = parsed
                    snackbarHostState.showSnackbar("Found ${parsed.size} documents in ZIP")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error parsing ZIP: ${e.localizedMessage}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                currentFileProcessing = "Scanning folder..."
                try {
                    val parsed = zipManager.parseFolderForImport(
                        folderUri = uri,
                        defaultDivision = defaultDivision,
                        defaultDistrict = defaultDistrict,
                        defaultUpazila = defaultUpazila,
                        defaultMouza = defaultMouza
                    )
                    candidates = parsed
                    snackbarHostState.showSnackbar("Found ${parsed.size} documents in folder")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error scanning folder: ${e.localizedMessage}")
                } finally {
                    isProcessing = false
                }
            }
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
                    text = stringResource(R.string.import_instructions),
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
                                text = "Ready to organize into LandArchive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isProcessing = true
                                    var importedCount = 0
                                    var dupCount = 0
                                    val toImport = candidates.filter { it.isSelected }
                                    val total = toImport.size

                                    toImport.forEachIndexed { idx, candidate ->
                                        currentFileProcessing = "Saving ${candidate.fileName}..."
                                        importProgress = (idx + 1).toFloat() / total.coerceAtLeast(1)

                                        val res = storageManager.saveDocument(
                                            division = candidate.division,
                                            district = candidate.district,
                                            upazila = candidate.upazila,
                                            mouza = candidate.mouza,
                                            docType = candidate.docType,
                                            khatianOrPlotNo = candidate.khatianOrPlotNo,
                                            rawFileName = candidate.fileName,
                                            data = candidate.data
                                        )
                                        when (res) {
                                            is SaveResult.Success -> importedCount++
                                            is SaveResult.Duplicate -> dupCount++
                                            is SaveResult.Error -> {}
                                        }
                                    }

                                    isProcessing = false
                                    candidates = emptyList()
                                    onImportComplete()
                                    snackbarHostState.showSnackbar("Import complete: $importedCount saved, $dupCount duplicates skipped.")
                                }
                            },
                            enabled = selectedCount > 0 && !isProcessing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("confirm_import_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save All ($selectedCount)")
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
                        text = "Choose an external ZIP archive or a folder of PDFs/images on your phone storage to inspect, categorize, and archive them.",
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
                                    candidates = candidates.map { it.copy(division = div) }
                                },
                                onDistrictSelected = { dist ->
                                    onDistrictSelected(dist)
                                    candidates = candidates.map { it.copy(district = dist) }
                                },
                                onUpazilaSelected = { upz ->
                                    onUpazilaSelected(upz)
                                    candidates = candidates.map { it.copy(upazila = upz) }
                                },
                                onMouzaChanged = { mz ->
                                    onMouzaChanged(mz)
                                    candidates = candidates.map { it.copy(mouza = mz) }
                                }
                            )
                        }
                    }
                }

                items(candidates, key = { it.id }) { candidate ->
                    ImportCandidateCard(
                        candidate = candidate,
                        onToggleSelect = { isChecked ->
                            candidates = candidates.map {
                                if (it.id == candidate.id) it.copy(isSelected = isChecked) else it
                            }
                        },
                        onDocTypeChange = { newType ->
                            candidates = candidates.map {
                                if (it.id == candidate.id) it.copy(docType = newType) else it
                            }
                        },
                        onKhatianChange = { newKhatian ->
                            candidates = candidates.map {
                                if (it.id == candidate.id) it.copy(khatianOrPlotNo = newKhatian) else it
                            }
                        }
                    )
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
