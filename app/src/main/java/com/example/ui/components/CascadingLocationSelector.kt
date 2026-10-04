package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.District
import com.example.data.Division
import com.example.data.LocationRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CascadingLocationSelector(
    locationRepo: LocationRepository,
    selectedDivision: String,
    selectedDistrict: String,
    selectedUpazila: String,
    mouza: String,
    onDivisionSelected: (String) -> Unit,
    onDistrictSelected: (String) -> Unit,
    onUpazilaSelected: (String) -> Unit,
    onMouzaChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMouzasLoaded: ((List<com.example.data.MouzaInfo>) -> Unit)? = null
) {
    val divisions = remember { locationRepo.getDivisions() }
    val districts = remember(selectedDivision) { locationRepo.getDistricts(selectedDivision) }
    val upazilas = remember(selectedDivision, selectedDistrict) {
        locationRepo.getUpazilas(selectedDivision, selectedDistrict)
    }

    var divisionExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var upazilaExpanded by remember { mutableStateOf(false) }
    var mouzaExpanded by remember { mutableStateOf(false) }

    // Fully query without truncation or hardcoded limits directly from dataset
    val mouzasForUpazila = remember(selectedDivision, selectedDistrict, selectedUpazila) {
        if (selectedUpazila.isNotBlank()) {
            locationRepo.getMouzasForUpazila(selectedUpazila, selectedDistrict, selectedDivision)
        } else {
            emptyList()
        }
    }

    // Automatically notify loaded mouzas whenever list changes
    LaunchedEffect(mouzasForUpazila) {
        onMouzasLoaded?.invoke(mouzasForUpazila)
    }

    // Automatically select all mouzas when upazila is selected or changed
    LaunchedEffect(selectedUpazila, selectedDistrict) {
        if (selectedUpazila.isNotBlank()) {
            val allMouzas = locationRepo.getMouzasForUpazila(selectedUpazila, selectedDistrict, selectedDivision)
            if (allMouzas.isNotEmpty()) {
                val allMouzasLabel = "All Mouzas (${allMouzas.size} Mouzas)"
                onMouzaChanged(allMouzasLabel)
                onMouzasLoaded?.invoke(allMouzas)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Archive Location Hierarchy / ভৌগোলিক স্তর",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Division & District in Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Division Selector
                ExposedDropdownMenuBox(
                    expanded = divisionExpanded,
                    onExpandedChange = { divisionExpanded = !divisionExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDivision,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.division)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("division_dropdown"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = divisionExpanded,
                        onDismissRequest = { divisionExpanded = false }
                    ) {
                        divisions.forEach { div ->
                            DropdownMenuItem(
                                text = { Text("${div.name} (${div.bnName})") },
                                onClick = {
                                    onDivisionSelected(div.name)
                                    divisionExpanded = false
                                    val newDistricts = locationRepo.getDistricts(div.name)
                                    val firstDist = newDistricts.firstOrNull()?.name ?: ""
                                    if (firstDist.isNotBlank()) {
                                        onDistrictSelected(firstDist)
                                        val newUpzs = locationRepo.getUpazilas(div.name, firstDist)
                                        val firstUpz = newUpzs.firstOrNull() ?: ""
                                        if (firstUpz.isNotBlank()) {
                                            onUpazilaSelected(firstUpz)
                                            val newMouzas = locationRepo.getMouzasForUpazila(firstUpz, firstDist, div.name)
                                            onMouzaChanged("All Mouzas (${newMouzas.size} Mouzas)")
                                            onMouzasLoaded?.invoke(newMouzas)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // District Selector
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDistrict,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.district)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("district_dropdown"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        districts.forEach { dist ->
                            DropdownMenuItem(
                                text = { Text("${dist.name} (${dist.bnName})") },
                                onClick = {
                                    onDistrictSelected(dist.name)
                                    districtExpanded = false
                                    val newUpazilas = locationRepo.getUpazilas(selectedDivision, dist.name)
                                    val firstUpz = newUpazilas.firstOrNull() ?: ""
                                    if (firstUpz.isNotBlank()) {
                                        onUpazilaSelected(firstUpz)
                                        val newMouzas = locationRepo.getMouzasForUpazila(firstUpz, dist.name, selectedDivision)
                                        onMouzaChanged("All Mouzas (${newMouzas.size} Mouzas)")
                                        onMouzasLoaded?.invoke(newMouzas)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Upazila Selector
            ExposedDropdownMenuBox(
                expanded = upazilaExpanded,
                onExpandedChange = { upazilaExpanded = !upazilaExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedUpazila,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.upazila)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("upazila_dropdown"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = upazilaExpanded,
                    onDismissRequest = { upazilaExpanded = false }
                ) {
                    upazilas.forEach { upz ->
                        DropdownMenuItem(
                            text = { Text(upz) },
                            onClick = {
                                onUpazilaSelected(upz)
                                upazilaExpanded = false
                                val newMouzas = locationRepo.getMouzasForUpazila(upz, selectedDistrict, selectedDivision)
                                onMouzaChanged("All Mouzas (${newMouzas.size} Mouzas)")
                                onMouzasLoaded?.invoke(newMouzas)
                            }
                        )
                    }
                }
            }

            // Mouza Quick Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mouza Selection (${mouzasForUpazila.size} in $selectedUpazila)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val allMouzas = locationRepo.getMouzasForUpazila(selectedUpazila, selectedDistrict, selectedDivision)
                            onMouzaChanged("All Mouzas (${allMouzas.size} Mouzas)")
                            onMouzasLoaded?.invoke(allMouzas)
                        },
                        modifier = Modifier.testTag("btn_select_all_mouzas_selector")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select All (${mouzasForUpazila.size})", style = MaterialTheme.typography.labelSmall)
                    }
                    if (mouza.isNotBlank()) {
                        TextButton(
                            onClick = { onMouzaChanged("") }
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Mouza Dropdown & Autocomplete Selector
            ExposedDropdownMenuBox(
                expanded = mouzaExpanded,
                onExpandedChange = { mouzaExpanded = !mouzaExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = mouza,
                    onValueChange = {
                        onMouzaChanged(it)
                        mouzaExpanded = true
                    },
                    label = { Text("${stringResource(R.string.mouza)} (${mouzasForUpazila.size} available)") },
                    placeholder = { Text("Select from list or type... / মৌজা নির্বাচন") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mouzaExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("mouza_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                if (mouzasForUpazila.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = mouzaExpanded,
                        onDismissRequest = { mouzaExpanded = false }
                    ) {
                        // Option 1: Select All Mouzas automatically
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "★ All Mouzas / সকল মৌজা (${mouzasForUpazila.size} Mouzas)",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Automatically select all ${mouzasForUpazila.size} mouzas in $selectedUpazila",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onMouzaChanged("All Mouzas (${mouzasForUpazila.size} Mouzas)")
                                onMouzasLoaded?.invoke(mouzasForUpazila)
                                mouzaExpanded = false
                            },
                            modifier = Modifier.testTag("item_select_all_mouzas")
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        val isAllSelected = mouza.isBlank() || mouza.startsWith("All Mouzas", ignoreCase = true)
                        val filtered = if (isAllSelected) mouzasForUpazila
                        else mouzasForUpazila.filter {
                            it.name.contains(mouza, ignoreCase = true) ||
                            it.bnName.contains(mouza, ignoreCase = true) ||
                            it.jlNo.contains(mouza, ignoreCase = true)
                        }

                        filtered.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.name} (${m.bnName}) - ${m.jlNo}") },
                                onClick = {
                                    onMouzaChanged("${m.name} (${m.bnName})")
                                    mouzaExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
