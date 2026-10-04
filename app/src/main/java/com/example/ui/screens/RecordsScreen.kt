package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.DocumentType
import com.example.data.LandRecord
import com.example.data.LocationRepository
import com.example.storage.StorageManager
import com.example.storage.download.DownloadStatus
import com.example.storage.download.LandRecordDownloadService
import com.example.ui.components.DownloadLandRecordDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    storageManager: StorageManager,
    locationRepo: LocationRepository? = null,
    onNavigateToCapture: () -> Unit,
    onNavigateToImport: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = locationRepo ?: remember { LocationRepository(context) }
    val downloadService = remember { LandRecordDownloadService.getInstance(context) }

    var records by remember { mutableStateOf(storageManager.getAllRecords()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<DocumentType?>(null) }
    var recordToDelete by remember { mutableStateOf<LandRecord?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    // Observe active background downloads
    val allDownloads by downloadService.observeAllDownloads().collectAsState(initial = emptyList())
    val activeDownloads = remember(allDownloads) {
        allDownloads.filter { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.ENQUEUED }
    }

    fun refreshRecords() {
        records = storageManager.getAllRecords()
    }

    // Auto refresh when a download succeeds
    LaunchedEffect(allDownloads) {
        if (allDownloads.any { it.status == DownloadStatus.SUCCEEDED }) {
            refreshRecords()
        }
    }

    val filteredRecords = remember(records, searchQuery, selectedFilterType) {
        records.filter { rec ->
            val matchesType = selectedFilterType == null || rec.docType == selectedFilterType
            val matchesQuery = searchQuery.isBlank() ||
                    rec.fileName.contains(searchQuery, ignoreCase = true) ||
                    rec.mouza.contains(searchQuery, ignoreCase = true) ||
                    rec.khatianOrPlotNo.contains(searchQuery, ignoreCase = true) ||
                    rec.upazila.contains(searchQuery, ignoreCase = true) ||
                    rec.district.contains(searchQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
    }

    val totalBytes = remember(records) { records.sumOf { it.fileSize } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDownloadDialog = true },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                text = { Text("Download Record") },
                modifier = Modifier.testTag("fab_download_record")
            )
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Active Download Status Banner
                if (activeDownloads.isNotEmpty()) {
                    val current = activeDownloads.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { showDownloadDialog = true }
                            .testTag("active_download_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Downloading (${current.progressPercentage}%): ${current.fileName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "View",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (current.progressPercentage >= 0) {
                                LinearProgressIndicator(
                                    progress = { current.progressPercentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                )
                            }
                        }
                    }
                }

                // Header & Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.tab_records),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${records.size} documents archived (${totalBytes / (1024 * 1024)} MB)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(onClick = { showDownloadDialog = true }) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Download Record")
                        }
                        IconButton(onClick = { refreshRecords() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Mouza, Khatian #, Upazila...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_records_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter by Doc Type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = { selectedFilterType = null },
                        label = { Text("All (${records.size})") }
                    )
                    DocumentType.entries.forEach { type ->
                        val count = records.count { it.docType == type }
                        if (count > 0 || records.isEmpty()) {
                            FilterChip(
                                selected = selectedFilterType == type,
                                onClick = { selectedFilterType = if (selectedFilterType == type) null else type },
                                label = { Text("${type.code} ($count)") }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (filteredRecords.isEmpty()) {
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
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (records.isEmpty()) "No Land Records in Archive Yet" else "No matching records found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (records.isEmpty())
                            "Capture khatians via the built-in browser or import local records from ZIP/folder."
                        else "Try a different search term or filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (records.isEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onNavigateToCapture) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Browser")
                            }
                            OutlinedButton(onClick = onNavigateToImport) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Files")
                            }
                        }
                    }
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
                items(filteredRecords, key = { it.id }) { record ->
                    RecordItemCard(
                        record = record,
                        onOpen = { openFile(context, record) },
                        onShare = { shareFile(context, record) },
                        onDelete = { recordToDelete = record }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        val rec = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Document?") },
            text = {
                Text("Are you sure you want to delete '${rec.fileName}' from the local archive? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        storageManager.deleteRecord(rec.id)
                        recordToDelete = null
                        refreshRecords()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remote Land Record Download Dialog (OkHttp & WorkManager)
    if (showDownloadDialog) {
        DownloadLandRecordDialog(
            locationRepo = repo,
            downloadService = downloadService,
            onDismiss = {
                showDownloadDialog = false
                refreshRecords()
            },
            onDownloadStarted = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Background download started with WorkManager!")
                }
            }
        )
    }
}

@Composable
fun RecordItemCard(
    record: LandRecord,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Doc Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.docType.code,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• ${record.docType.bnLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = "${record.fileSize / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // File Name & Khatian
            Text(
                text = record.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (record.khatianOrPlotNo.isNotBlank()) {
                Text(
                    text = "Khatian / Plot: ${record.khatianOrPlotNo}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Location Hierarchy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${record.division} > ${record.district} > ${record.upazila} (Mouza: ${record.mouza})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // SHA-256 Hash & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SHA: ${record.sha256.take(12)}...",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = dateFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View")
                }
            }
        }
    }
}

private fun openFile(context: Context, record: LandRecord) {
    try {
        val file = File(record.filePath)
        if (!file.exists()) return

        val mime = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true) -> "text/html"
            file.name.endsWith(".png", ignoreCase = true) -> "image/png"
            file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            else -> "*/*"
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open Record"))
    } catch (e: Exception) {
        // Fallback: toast or snackbar
    }
}

private fun shareFile(context: Context, record: LandRecord) {
    try {
        val file = File(record.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Land Record: ${record.fileName}")
            putExtra(Intent.EXTRA_TEXT, "Bangladesh Land Archive Record: ${record.docType.enLabel} - ${record.upazila}, Mouza: ${record.mouza}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Land Record"))
    } catch (e: Exception) {
        // Fallback
    }
}
