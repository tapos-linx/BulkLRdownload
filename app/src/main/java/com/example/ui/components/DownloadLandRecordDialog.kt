package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DocumentType
import com.example.data.LocationRepository
import com.example.storage.download.DownloadProgress
import com.example.storage.download.DownloadRequest
import com.example.storage.download.DownloadStatus
import com.example.storage.download.LandRecordDownloadService
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadLandRecordDialog(
    locationRepo: LocationRepository,
    downloadService: LandRecordDownloadService,
    onDismiss: () -> Unit,
    onDownloadStarted: (UUID) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = New Download, 1 = Active Tasks

    // Form State
    var downloadUrl by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var selectedDocType by remember { mutableStateOf(DocumentType.RS) }
    var khatianNo by remember { mutableStateOf("") }
    var requiresWifi by remember { mutableStateOf(false) }

    // Location State
    var division by remember { mutableStateOf("Dhaka") }
    var district by remember { mutableStateOf("Dhaka") }
    var upazila by remember { mutableStateOf("Savar") }
    var mouza by remember {
        val initialCount = locationRepo.getMouzasForUpazila("Savar", "Dhaka", "Dhaka").size
        mutableStateOf("All Mouzas ($initialCount Mouzas)")
    }

    // Observe active tasks
    val activeDownloads by downloadService.observeAllDownloads().collectAsState(initial = emptyList())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("download_land_record_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Land Record Downloader",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "OkHttp & WorkManager Background Service",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: New Download vs Active Tasks
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("New Download") },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            val activeCount = activeDownloads.count { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.ENQUEUED }
                            Text(if (activeCount > 0) "Active ($activeCount)" else "Tasks (${activeDownloads.size})")
                        },
                        icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // New Download Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Presets
                        Text(
                            text = "Sample Record Presets / নমুনা রেকর্ড:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    downloadUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
                                    fileName = "${upazila}_${selectedDocType.code}_Khatian_${khatianNo.ifBlank { "101" }}.pdf"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sample PDF (Khatian)", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    downloadUrl = "https://picsum.photos/1200/800"
                                    fileName = "${upazila}_${selectedDocType.code}_MouzaMap_Sheet1.jpg"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sample Map (Image)", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // URL Input
                        OutlinedTextField(
                            value = downloadUrl,
                            onValueChange = {
                                downloadUrl = it
                                if (fileName.isBlank() && it.isNotBlank()) {
                                    val guessed = it.substringAfterLast("/").substringBefore("?")
                                    if (guessed.isNotBlank()) fileName = guessed
                                }
                            },
                            label = { Text("Remote Document URL (PDF / Image)") },
                            placeholder = { Text("https://example.gov.bd/records/cs_123.pdf") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_download_url"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // File Name Input
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            label = { Text("Save File Name") },
                            placeholder = { Text("Savar_RS_Khatian_45.pdf") },
                            leadingIcon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_download_filename"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Khatian / Plot Number
                        OutlinedTextField(
                            value = khatianNo,
                            onValueChange = { khatianNo = it },
                            label = { Text("Khatian / Plot Number (Optional)") },
                            placeholder = { Text("e.g. 102/A") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Document Type Selector
                        Text(
                            text = "Document Survey Type:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        DocTypeSelector(
                            selectedDocType = selectedDocType,
                            onDocTypeSelected = { selectedDocType = it }
                        )

                        // Cascading Location Selector
                        CascadingLocationSelector(
                            locationRepo = locationRepo,
                            selectedDivision = division,
                            selectedDistrict = district,
                            selectedUpazila = upazila,
                            mouza = mouza,
                            onDivisionSelected = { division = it },
                            onDistrictSelected = { district = it },
                            onUpazilaSelected = { upazila = it },
                            onMouzaChanged = { mouza = it }
                        )

                        // Wi-Fi constraint toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Require Wi-Fi (Unmetered)", fontWeight = FontWeight.SemiBold)
                                Text("Only download when connected to unmetered Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = requiresWifi,
                                onCheckedChange = { requiresWifi = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (downloadUrl.isNotBlank()) {
                                    val safeFileName = if (fileName.isNotBlank()) {
                                        fileName
                                    } else {
                                        "${upazila}_${selectedDocType.code}_Record_${System.currentTimeMillis()}.pdf"
                                    }

                                    val request = DownloadRequest(
                                        url = downloadUrl.trim(),
                                        fileName = safeFileName,
                                        division = division,
                                        district = district,
                                        upazila = upazila,
                                        mouza = mouza,
                                        docType = selectedDocType,
                                        khatianOrPlotNo = khatianNo.trim(),
                                        requiresWifi = requiresWifi,
                                        isForeground = true
                                    )

                                    val workId = downloadService.enqueueDownload(request)
                                    onDownloadStarted(workId)
                                    selectedTab = 1 // Switch to active tasks tab
                                }
                            },
                            enabled = downloadUrl.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_start_background_download"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Background Download (WorkManager)")
                        }
                    }
                } else {
                    // Active Downloads List
                    if (activeDownloads.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No active download tasks",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Start a new download from the 'New Download' tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(activeDownloads, key = { it.workId }) { item ->
                                DownloadItemCard(
                                    progress = item,
                                    onCancel = { downloadService.cancelDownload(item.workId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    progress: DownloadProgress,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusIcon = when (progress.status) {
                        DownloadStatus.RUNNING -> Icons.Default.Sync
                        DownloadStatus.SUCCEEDED -> Icons.Default.CheckCircle
                        DownloadStatus.FAILED -> Icons.Default.Error
                        DownloadStatus.CANCELLED -> Icons.Default.Cancel
                        DownloadStatus.ENQUEUED -> Icons.Default.Schedule
                        DownloadStatus.BLOCKED -> Icons.Default.HourglassEmpty
                    }
                    val statusColor = when (progress.status) {
                        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        DownloadStatus.SUCCEEDED -> MaterialTheme.colorScheme.tertiary
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                        DownloadStatus.ENQUEUED -> MaterialTheme.colorScheme.secondary
                        DownloadStatus.BLOCKED -> MaterialTheme.colorScheme.outline
                    }

                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = progress.fileName.ifBlank { "Land Record File" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(status = progress.status)
            }

            // Progress Bar
            if (progress.status == DownloadStatus.RUNNING) {
                if (progress.progressPercentage >= 0) {
                    LinearProgressIndicator(
                        progress = { progress.progressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                }
            } else if (progress.status == DownloadStatus.SUCCEEDED) {
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val detailText = when (progress.status) {
                    DownloadStatus.RUNNING -> {
                        val downloaded = formatBytes(progress.bytesDownloaded)
                        val total = if (progress.totalBytes > 0) formatBytes(progress.totalBytes) else "..."
                        "${progress.progressPercentage}% • $downloaded / $total"
                    }
                    DownloadStatus.SUCCEEDED -> {
                        "Complete • ${formatBytes(progress.bytesDownloaded)}"
                    }
                    DownloadStatus.FAILED -> {
                        progress.errorMessage ?: "Failed to download"
                    }
                    DownloadStatus.ENQUEUED -> "Waiting in WorkManager queue..."
                    DownloadStatus.CANCELLED -> "Download cancelled"
                    DownloadStatus.BLOCKED -> "Waiting for network requirements"
                }

                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (progress.status == DownloadStatus.RUNNING || progress.status == DownloadStatus.ENQUEUED) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DownloadStatus) {
    val (label, containerColor, contentColor) = when (status) {
        DownloadStatus.RUNNING -> Triple("Downloading", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        DownloadStatus.SUCCEEDED -> Triple("Saved", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        DownloadStatus.FAILED -> Triple("Failed", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        DownloadStatus.CANCELLED -> Triple("Cancelled", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        DownloadStatus.ENQUEUED -> Triple("Queued", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        DownloadStatus.BLOCKED -> Triple("Blocked", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
