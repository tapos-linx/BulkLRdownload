package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JobStatus
import com.example.data.db.AuditLogEntity
import com.example.data.db.DownloadJobEntity
import com.example.storage.download.QueueSummaryStats
import com.example.storage.source.LandRecordSourceAdapter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QueueAndTelemetrySection(
    jobs: List<DownloadJobEntity>,
    stats: QueueSummaryStats,
    isRunning: Boolean,
    isPaused: Boolean,
    lastActivityMessageBn: String,
    selectedFolderLabel: String,
    isLowStorage: Boolean,
    onStartQueue: () -> Unit,
    onPauseQueue: () -> Unit,
    onResumeQueue: () -> Unit,
    onCancelClearQueue: () -> Unit,
    onRetryFailedJobs: () -> Unit,
    onRetryAuthJobs: () -> Unit,
    onExportQueueJson: () -> Unit,
    onUploadCsvBatch: () -> Unit,
    onPasteCsvSubmit: (String) -> Unit,
    onExportMasterZip: () -> Unit
) {
    var csvTextInput by remember { mutableStateOf("") }
    var showCsvDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Telemetry Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
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
                        text = "কার্যসারি ও চলমান কাজ (Persistent Download Queue)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "থ্রেড: ${stats.activeWorkers} / 2",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "সর্বশেষ কার্যক্রম: $lastActivityMessageBn",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (isLowStorage) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "সতর্কবার্তা: ডিভাইসে স্টোরেজ স্পেস কম! অনুগ্রহ করে মেমোরি খালি করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Telemetry Grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatBadge("মোট কাজ", "${stats.totalDiscovered}", Color(0xFF0F172A))
                    StatBadge("যাচাইকৃত (VALIDATED)", "${stats.validatedCount}", Color(0xFF059669))
                    StatBadge("ডাউনলোড হচ্ছে", "${stats.downloadingCount}", Color(0xFF0284C7))
                    StatBadge("অপেক্ষমাণ", "${stats.pendingCount}", Color(0xFF64748B))
                    StatBadge("ডুপ্লিকেট", "${stats.duplicateCount}", Color(0xFF7C3AED))
                    StatBadge("পাওয়া যায়নি", "${stats.missingNotFoundCount}", Color(0xFF94A3B8))
                    StatBadge("ব্যর্থ/অসম্পূর্ণ", "${stats.failedCount + stats.incompleteCount}", Color(0xFFDC2626))
                    StatBadge("লগইন প্রয়োজন", "${stats.authRequiredCount}", Color(0xFFE11D48))
                }

                // Queue Control Buttons: Start, Pause, Resume, Retry, Cancel
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isRunning && !isPaused) {
                        Button(
                            onClick = onStartQueue,
                            enabled = stats.pendingCount > 0 || stats.incompleteCount > 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("queue_start_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("শুরু করুন (Start)", fontSize = 12.sp)
                        }
                    }

                    if (isRunning) {
                        OutlinedButton(
                            onClick = onPauseQueue,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("queue_pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("বিরতি (Pause)", fontSize = 12.sp)
                        }
                    }

                    if (isPaused) {
                        Button(
                            onClick = onResumeQueue,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("queue_resume_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পুনরায় শুরু (Resume)", fontSize = 12.sp)
                        }
                    }

                    if (stats.failedCount > 0 || stats.incompleteCount > 0) {
                        OutlinedButton(
                            onClick = onRetryFailedJobs,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("queue_retry_failed_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ব্যর্থ কাজ পুনরায় চেষ্টা", fontSize = 12.sp)
                        }
                    }

                    if (stats.authRequiredCount > 0 || stats.accessRestrictedCount > 0) {
                        OutlinedButton(
                            onClick = onRetryAuthJobs,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("queue_retry_auth_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("লগইন-প্রয়োজনীয় কাজ পুনরায়", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onCancelClearQueue,
                        enabled = jobs.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("queue_clear_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বাতিল ও মুছুন", fontSize = 12.sp)
                    }
                }

                HorizontalDivider()

                // Export Queue as JSON, CSV Batch Upload, and Save Master ZIP
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onExportQueueJson,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("queue_panel_export_json_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কিউ JSON ফাইল এক্সপোর্ট", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showCsvDialog = !showCsvDialog },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("toggle_csv_paste_box_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV ব্যাচ ইনপুট / ফাইল আপলোড", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onExportMasterZip,
                        enabled = stats.validatedCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_master_zip_button")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("মাস্টার জিপ সংরক্ষণ (${stats.validatedCount})", fontSize = 12.sp)
                    }
                }

                if (showCsvDialog) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CSV ব্যাচ প্রসেসিং ইউটিলিটি (জেলা,উপজেলা,মৌজা,জেএল,সার্ভে,রেকর্ড অথবা Record ID):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = csvTextInput,
                                onValueChange = { csvTextInput = it },
                                placeholder = {
                                    Text("Cumilla,Titas,কড়িকান্দি,JL 220,RS,KHATIAN\nBrahmanbaria,Sarail,সরাইল,JL 10,BRS,KHATIAN")
                                },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("csv_batch_text_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (csvTextInput.isNotBlank()) {
                                            onPasteCsvSubmit(csvTextInput)
                                            csvTextInput = ""
                                            showCsvDialog = false
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("submit_csv_batch_button")
                                ) {
                                    Text("CSV কিউতে যোগ করুন", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = onUploadCsvBatch,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("ফোন থেকে CSV ফাইল বাছুন", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Job List Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "কিউভুক্ত কাজের তালিকা ও প্রমাণ ট্রেসিবিলিটি (${jobs.size} টি):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (jobs.isEmpty()) {
                    Text(
                        text = "বর্তমানে কার্যসারিতে কোনো কাজ নেই। এলাকা নির্বাচন থেকে উপজেলা সিলেক্ট করলেই স্বয়ংক্রিয়ভাবে মৌজা যুক্ত হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 380.dp)
                            .testTag("queue_jobs_lazy_list")
                    ) {
                        items(jobs, key = { it.jobId }) { job ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${job.mouzaNameBn} (${job.jlNo}) • ${job.surveyType} ${job.recordType}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = job.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${job.districtBn} → ${job.upazilaBn} | প্রচেষ্টা: ${job.attemptCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (job.sha256Checksum.isNotBlank()) {
                                        Text(
                                            text = "SHA-256: ${job.sha256Checksum.take(24)}... (${job.fileSizeBytes} B)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (job.archiveRelativePath.isNotBlank()) {
                                        Text(
                                            text = "ফাইল পাথ: ${job.archiveRelativePath}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (job.validationResult.isNotBlank()) {
                                        Text(
                                            text = job.validationResult,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (job.status == JobStatus.FAILED.name) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (job.status == JobStatus.DOWNLOADING.name || job.status == JobStatus.DOWNLOADED_UNVALIDATED.name) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(labelBn: String, countText: String, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$labelBn: ",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

@Composable
fun ArchiveAndReportsSection(
    jobs: List<DownloadJobEntity>,
    auditLogs: List<AuditLogEntity>,
    stats: QueueSummaryStats,
    selectedFolderLabel: String,
    adapters: List<LandRecordSourceAdapter>,
    onPickPhoneFolder: () -> Unit,
    onExportQueueJson: () -> Unit,
    onExportMasterZip: () -> Unit
) {
    val validatedJobs = jobs.filter { it.status == JobStatus.VALIDATED.name || it.status == JobStatus.VERIFIED.name }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Storage & Archive Layout Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ফাইল ভাণ্ডার ও ফোন মেমোরি কাঠামো (Local Archive Layout)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "নির্বাচিত সংরক্ষণ স্থান: $selectedFolderLabel",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "LRgetBD/\n" +
                            "  Bangladesh/\n" +
                            "    Division/District/Upazila/Mouza/SurveyType/RecordType/\n" +
                            "      validated-record.pdf\n" +
                            "  manifest.json\n" +
                            "  Reports/ (collection_report.csv, collection_report.json)\n" +
                            "  Logs/",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPickPhoneFolder,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ফোন মেমোরি ফোল্ডার বাছুন", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onExportMasterZip,
                        enabled = validatedJobs.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("মাস্টার জিপ (${validatedJobs.size})", fontSize = 12.sp)
                    }
                }
            }
        }

        // Source Adapters & Security Status Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "উৎস অ্যাডাপ্টার ও নিরাপত্তা যাচাইকরণ (Source Adapters)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                adapters.forEach { adapter ->
                    val desc = adapter.descriptor
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = desc.nameBn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (desc.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = if (desc.isEnabled) "সক্রিয় (ENABLED)" else "নিষ্ক্রিয় (DISABLED)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "পদ্ধতি: ${desc.permittedDownloadMethod}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "রেট লিমিট: ${desc.rateLimitPolicy}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (!desc.isEnabled && desc.disabledReasonBn.isNotBlank()) {
                                Text(
                                    text = "কারণ: ${desc.disabledReasonBn}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audit Trail / History Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সংগ্রহ ইতিহাস ও অডিট লগ (${auditLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (auditLogs.isEmpty()) {
                    Text(
                        text = "এখনও কোনো অডিট লগ তৈরি হয়নি।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    auditLogs.take(12).forEach { log ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "[${log.eventType}] ${log.messageBn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
