package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
    modifier: Modifier = Modifier
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

    val mouzasForUpazila = remember(selectedUpazila) {
        locationRepo.getMouzasForUpazila(selectedUpazila)
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
                            }
                        )
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
                        val filtered = if (mouza.isBlank()) mouzasForUpazila
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
