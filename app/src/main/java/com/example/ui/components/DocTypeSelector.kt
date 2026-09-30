package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DocumentType

@Composable
fun DocTypeSelector(
    selectedDocType: DocumentType,
    onDocTypeSelected: (DocumentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Document Survey / Type",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DocumentType.entries.forEach { type ->
                val isSelected = type == selectedDocType
                FilterChip(
                    selected = isSelected,
                    onClick = { onDocTypeSelected(type) },
                    label = {
                        Column {
                            Text(
                                text = type.code,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = type.bnLabel,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.testTag("doc_type_chip_${type.code.lowercase()}")
                )
            }
        }
    }
}
