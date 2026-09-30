package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.DocumentType
import com.example.data.LocationRepository
import com.example.storage.MasterZipResult
import com.example.storage.StorageManager
import com.example.storage.ZipManager
import com.example.ui.components.CascadingLocationSelector
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun MasterZipScreen(
    locationRepo: LocationRepository,
    storageManager: StorageManager,
    zipManager: ZipManager,
    selectedDivision: String,
    selectedDistrict: String,
    selectedUpazila: String,
    mouza: String,
    onDivisionSelected: (String) -> Unit,
    onDistrictSelected: (String) -> Unit,
    onUpazilaSelected: (String) -> Unit,
    onMouzaChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isGenerating by remember { mutableStateOf(false) }
    var progressCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }
    var currentItemName by remember { mutableStateOf("") }
    var zipResult by remember { mutableStateOf<MasterZipResult?>(null) }

    // Fetch records for currently selected upazila
    val upazilaRecords = remember(selectedDivision, selectedDistrict, selectedUpazila, storageManager.getAllRecords()) {
        storageManager.getRecordsForUpazila(selectedDivision, selectedDistrict, selectedUpazila)
    }

    // Export to chosen file launcher
    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && zipResult != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        FileInputStream(zipResult!!.zipFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    snackbarHostState.showSnackbar("Master ZIP saved successfully!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error saving ZIP: ${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.master_zip_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.master_zip_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Upazila Location Selector
            CascadingLocationSelector(
                locationRepo = locationRepo,
                selectedDivision = selectedDivision,
                selectedDistrict = selectedDistrict,
                selectedUpazila = selectedUpazila,
                mouza = mouza,
                onDivisionSelected = onDivisionSelected,
                onDistrictSelected = onDistrictSelected,
                onUpazilaSelected = onUpazilaSelected,
                onMouzaChanged = onMouzaChanged
            )

            // Statistics Card for Selected Upazila
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
                        Text(
                            text = "Upazila Archive Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Badge {
                            Text("${upazilaRecords.size} files")
                        }
                    }

                    if (upazilaRecords.isEmpty()) {
                        Text(
                            text = "No records currently in internal database for $selectedUpazila ($selectedDistrict).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isGenerating = true
                                    zipResult = null
                                    try {
                                        val valService = com.example.storage.ArchiveValidationService(
                                            context,
                                            storageManager,
                                            com.example.storage.DocumentDirectoryService(context)
                                        )
                                        val mouzas = locationRepo.getMouzasForUpazila(selectedUpazila)
                                        val report = valService.validateAndPrepareArchive(
                                            division = selectedDivision,
                                            district = selectedDistrict,
                                            upazila = selectedUpazila,
                                            activeMouzas = mouzas,
                                            activeDocTypes = DocumentType.entries.toSet(),
                                            onProgress = { cur, tot, status ->
                                                progressCount = cur
                                                totalCount = tot
                                                currentItemName = status
                                            }
                                        )
                                        val res = zipManager.createMasterZip(
                                            division = selectedDivision,
                                            district = selectedDistrict,
                                            upazila = selectedUpazila,
                                            records = report.verifiedRecords,
                                            onProgress = { cur, tot, name ->
                                                progressCount = cur
                                                totalCount = tot
                                                currentItemName = name
                                            }
                                        )
                                        val exportRes = zipManager.exportMasterPackageToPhoneMemory(
                                            upazila = selectedUpazila,
                                            records = report.verifiedRecords,
                                            zipFile = res.zipFile,
                                            docDirService = com.example.storage.DocumentDirectoryService(context)
                                        )
                                        zipResult = res
                                        snackbarHostState.showSnackbar("Master Folder (${exportRes.exportedFilesCount} files in ${exportRes.exportedSubfoldersCount} subfolders) & ZIP downloaded to ${exportRes.primaryDisplayPath}!")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Packaging error: ${e.localizedMessage}")
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_auto_archive_and_generate"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Archive All Mouzas & Download to Phone Memory")
                        }
                    } else {
                        val totalBytes = upazilaRecords.sumOf { it.fileSize }
                        Text(
                            text = "Total Raw Size: ${totalBytes / 1024} KB (${"%.2f".format(totalBytes / (1024f * 1024f))} MB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        Text(
                            text = "Breakdown by Survey / Document Type:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        DocumentType.entries.forEach { type ->
                            val count = upazilaRecords.count { it.docType == type }
                            if (count > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${type.code} (${type.bnLabel})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "$count records",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Generate Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isGenerating = true
                        zipResult = null
                        try {
                            val res = zipManager.createMasterZip(
                                division = selectedDivision,
                                district = selectedDistrict,
                                upazila = selectedUpazila,
                                records = upazilaRecords,
                                onProgress = { cur, tot, name ->
                                    progressCount = cur
                                    totalCount = tot
                                    currentItemName = name
                                }
                            )
                            zipResult = res
                            snackbarHostState.showSnackbar("Master ZIP created: ${res.entryCount} entries packaging completed!")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Packaging error: ${e.localizedMessage}")
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = upazilaRecords.isNotEmpty() && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_master_zip_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_generate_master_zip))
            }

            // Progress Display
            if (isGenerating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Packaging: $currentItemName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (totalCount > 0) {
                            LinearProgressIndicator(
                                progress = { progressCount.toFloat() / totalCount },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            text = "$progressCount / $totalCount items processed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Result Card
            zipResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Master ZIP Package Created!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "File: ${result.zipFile.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "Package Size: ${result.totalSize / 1024} KB | Files: ${result.entryCount} + manifest.tsv",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "SHA-256 Checksum:\n${result.sha256}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val docDirService = com.example.storage.DocumentDirectoryService(context)
                                    val effectiveRecords = if (upazilaRecords.isNotEmpty()) {
                                        upazilaRecords
                                    } else {
                                        val valService = com.example.storage.ArchiveValidationService(
                                            context,
                                            storageManager,
                                            docDirService
                                        )
                                        val mouzas = locationRepo.getMouzasForUpazila(selectedUpazila)
                                        val rep = valService.validateAndPrepareArchive(
                                            division = selectedDivision,
                                            district = selectedDistrict,
                                            upazila = selectedUpazila,
                                            activeMouzas = mouzas,
                                            activeDocTypes = DocumentType.entries.toSet()
                                        )
                                        rep.verifiedRecords
                                    }
                                    val exportRes = zipManager.exportMasterPackageToPhoneMemory(
                                        upazila = selectedUpazila,
                                        records = effectiveRecords,
                                        zipFile = result.zipFile,
                                        docDirService = docDirService
                                    )
                                    snackbarHostState.showSnackbar("Saved Master Folder (${exportRes.exportedFilesCount} documents in ${exportRes.exportedSubfoldersCount} subfolders) & ZIP to ${exportRes.primaryDisplayPath}!")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_download_master_to_phone_memory"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Master Folder & ZIP to Phone Memory")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    shareMasterZip(context, result.zipFile)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share ZIP")
                            }

                            OutlinedButton(
                                onClick = {
                                    saveZipLauncher.launch(result.zipFile.name)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export to Storage")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareMasterZip(context: Context, zipFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LandArchive BD Master Archive: ${zipFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Master ZIP Package of Bangladesh Land Records with manifest.tsv checksums.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Master ZIP"))
    } catch (e: Exception) {
        // Fallback
    }
}
