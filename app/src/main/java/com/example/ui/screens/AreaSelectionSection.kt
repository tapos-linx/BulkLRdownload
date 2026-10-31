package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.District
import com.example.data.Division
import com.example.data.MouzaInfo
import com.example.data.RecordCategory
import com.example.data.SurveyCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AreaSelectionSection(
    divisions: List<Division>,
    selectedDivision: String,
    districts: List<District>,
    selectedDistrict: String,
    upazilas: List<String>,
    selectedUpazila: String,
    getUpazilaDisplayName: (String) -> String,
    mouzas: List<MouzaInfo>,
    mouzaSearchQuery: String,
    selectedSurveys: Set<String>,
    selectedRecordTypes: Set<String>,
    selectedFolderLabel: String,
    catalogueSourceBadge: String,
    onDivisionSelected: (String) -> Unit,
    onDistrictSelected: (String) -> Unit,
    onUpazilaSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleMouza: (String) -> Unit,
    onSelectAllMouzas: (Boolean) -> Unit,
    onToggleSurvey: (String) -> Unit,
    onToggleRecordType: (String) -> Unit,
    onPickPhoneFolder: () -> Unit,
    onStartCollectionClick: () -> Unit,
    onExportQueueJsonClick: () -> Unit,
    onUploadCsvBatchClick: () -> Unit
) {
    var divisionExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var upazilaExpanded by remember { mutableStateOf(false) }

    val selectedDistrictObj = districts.find { it.name.equals(selectedDistrict, ignoreCase = true) }
    val selectedDistrictDisplay = if (selectedDistrictObj != null) {
        "${selectedDistrictObj.bnName} (${selectedDistrictObj.name})"
    } else {
        selectedDistrict
    }

    val selectedUpazilaDisplay = if (selectedUpazila.isNotBlank()) {
        getUpazilaDisplayName(selectedUpazila)
    } else {
        "-- উপজেলা নির্বাচন করুন (Select Upazila) --"
    }

    val filteredMouzas = remember(mouzas, mouzaSearchQuery) {
        if (mouzaSearchQuery.isBlank()) {
            mouzas
        } else {
            val q = mouzaSearchQuery.trim()
            mouzas.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.bnName.contains(q, ignoreCase = true) ||
                    it.jlNo.contains(q, ignoreCase = true)
            }
        }
    }

    val selectedMouzaCount = mouzas.count { it.isSelected }
    val totalDiscoveredJobsForSelection = selectedMouzaCount * selectedSurveys.size * selectedRecordTypes.size

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Phone Memory Folder Picker & Catalogue Source Verification Banner
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = catalogueSourceBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "অটো-সিলেক্ট সক্রিয় (Auto-Select)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ফোনের মেমোরি সংরক্ষণ ফোল্ডার (Save Location in Phone Memory):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedFolderLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("selected_folder_path_text")
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onPickPhoneFolder,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("choose_phone_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Choose Folder",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফোল্ডার পাল্টান", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Card 2: Cascading Area Selector (Division -> District [Cumilla & Brahmanbaria] -> Upazila)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationCity,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "প্রশাসনিক এলাকা নির্বাচন (Division → District → Upazila)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "জেলা নির্বাচন করলে সংশ্লিষ্ট সব উপজেলা লোড হবে এবং উপজেলা নির্বাচন করলেই সব মৌজা স্বয়ংক্রিয়ভাবে সিলেক্ট হবে",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Priority District Buttons for Cumilla & Brahmanbaria
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isCumilla = selectedDistrict.equals("Cumilla", ignoreCase = true)
                    val isBrahmanbaria = selectedDistrict.equals("Brahmanbaria", ignoreCase = true)

                    FilterChip(
                        selected = isCumilla,
                        onClick = { onDistrictSelected("Cumilla") },
                        label = { Text("কুমিল্লা (Cumilla - 17 Upazilas)", fontWeight = FontWeight.Bold) },
                        leadingIcon = if (isCumilla) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_chip_cumilla")
                    )

                    FilterChip(
                        selected = isBrahmanbaria,
                        onClick = { onDistrictSelected("Brahmanbaria") },
                        label = { Text("ব্রাহ্মণবাড়িয়া (Brahmanbaria - 9)", fontWeight = FontWeight.Bold) },
                        leadingIcon = if (isBrahmanbaria) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_chip_brahmanbaria")
                    )
                }

                // Division Dropdown
                ExposedDropdownMenuBox(
                    expanded = divisionExpanded,
                    onExpandedChange = { divisionExpanded = !divisionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val divObj = divisions.find { it.name.equals(selectedDivision, true) }
                    OutlinedTextField(
                        value = if (divObj != null) "${divObj.bnName} (${divObj.name})" else selectedDivision,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("১. বিভাগ (Division)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("division_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = divisionExpanded,
                        onDismissRequest = { divisionExpanded = false }
                    ) {
                        divisions.forEach { div ->
                            DropdownMenuItem(
                                text = { Text("${div.bnName} (${div.name})") },
                                onClick = {
                                    divisionExpanded = false
                                    onDivisionSelected(div.name)
                                }
                            )
                        }
                    }
                }

                // District Dropdown (Cumilla & Brahmanbaria featured prominently at top)
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDistrictDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("২. জেলা নির্বাচন করুন (District - কুমিল্লা ও ব্রাহ্মণবাড়িয়া)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("district_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        districts.forEach { dist ->
                            val badge = if (dist.name.equals("Cumilla", true) || dist.name.equals("Brahmanbaria", true)) " ★" else ""
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${dist.bnName} (${dist.name}) - ${dist.upazilas.size} টি উপজেলা$badge",
                                        fontWeight = if (badge.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    districtExpanded = false
                                    onDistrictSelected(dist.name)
                                },
                                modifier = Modifier.testTag("district_option_${dist.name.lowercase()}")
                            )
                        }
                    }
                }

                // Upazila Dropdown (Shows all Upazilas related to selected District)
                ExposedDropdownMenuBox(
                    expanded = upazilaExpanded,
                    onExpandedChange = { upazilaExpanded = !upazilaExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedUpazilaDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("৩. উপজেলা নির্বাচন করুন (Upazila - মোট ${upazilas.size} টি)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("upazila_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = upazilaExpanded,
                        onDismissRequest = { upazilaExpanded = false }
                    ) {
                        upazilas.forEach { upz ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = getUpazilaDisplayName(upz),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    upazilaExpanded = false
                                    onUpazilaSelected(upz)
                                },
                                modifier = Modifier.testTag("upazila_option_${upz.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }

                // Survey Type & Record Type Multi-Selectors
                Text(
                    text = "৪. জরিপ ও রেকর্ডের ধরন (Survey & Record Categories):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SurveyCategory.entries.take(4).forEach { survey ->
                        val isSelected = selectedSurveys.contains(survey.code)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleSurvey(survey.code) },
                            label = { Text(survey.nameBn, fontSize = 12.sp) },
                            modifier = Modifier.testTag("survey_chip_${survey.code}")
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecordCategory.entries.forEach { rec ->
                        val isSelected = selectedRecordTypes.contains(rec.code)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleRecordType(rec.code) },
                            label = { Text(rec.nameBn, fontSize = 12.sp) },
                            modifier = Modifier.testTag("record_chip_${rec.code}")
                        )
                    }
                }
            }
        }

        // Card 3: Auto-Selected Mouzas Grid + Search Filter + Primary Action
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedUpazila.isNotBlank()) {
                                    "${getUpazilaDisplayName(selectedUpazila)} এর মৌজা তালিকা"
                                } else {
                                    "মৌজা তালিকা (উপজেলা নির্বাচন করুন)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "নির্বাচিত মৌজা: $selectedMouzaCount / ${mouzas.size} টি | মোট কাজ: $totalDiscoveredJobsForSelection টি",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("mouza_selection_counter")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val allSelected = mouzas.isNotEmpty() && selectedMouzaCount == mouzas.size
                        OutlinedButton(
                            onClick = { onSelectAllMouzas(!allSelected) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_select_all_mouzas_button")
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (allSelected) "সব বাদ দিন" else "সব মৌজা নির্বাচন", fontSize = 12.sp)
                        }
                    }
                }

                // Search bar to filter Mouzas by Bengali/English name or JL number
                OutlinedTextField(
                    value = mouzaSearchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("মৌজার নাম বা জেএল নম্বর (যেমন: JL 01, কালিকাপুর) দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Mouzas") },
                    trailingIcon = if (mouzaSearchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onSearchQueryChanged("") },
                                modifier = Modifier.testTag("clear_mouza_search_button")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mouza_search_bar")
                )

                if (filteredMouzas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mouzas.isEmpty()) {
                                "উপরে থেকে একটি উপজেলা নির্বাচন করলে সব মৌজা স্বয়ংক্রিয়ভাবে এখানে লোড ও সিলেক্ট হবে।"
                            } else {
                                "\"$mouzaSearchQuery\" এর সাথে মিলে এমন কোনো মৌজা পাওয়া যায়নি।"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Efficient LazyVerticalGrid supporting 200+ Mouzas smoothly
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 148.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 300.dp)
                            .testTag("mouza_lazy_grid")
                    ) {
                        items(filteredMouzas, key = { it.id.ifBlank { "${it.name}_${it.jlNo}" } }) { mouza ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (mouza.isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (mouza.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleMouza(mouza.id) }
                                    .testTag("mouza_item_${mouza.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mouza.bnName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "জেএল: ${mouza.jlNo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = if (mouza.isSelected) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (mouza.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Action Button: “সংগ্রহ শুরু করুন”
                Button(
                    onClick = onStartCollectionClick,
                    enabled = selectedMouzaCount > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_collection_primary_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সংগ্রহ শুরু করুন ($totalDiscoveredJobsForSelection টি রেকর্ড ফাইল)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Batch Utility Row: Export Queue JSON & Upload CSV Batch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportQueueJsonClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_queue_json_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কিউ JSON এক্সপোর্ট", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onUploadCsvBatchClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_csv_batch_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV ব্যাচ আপলোড", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
