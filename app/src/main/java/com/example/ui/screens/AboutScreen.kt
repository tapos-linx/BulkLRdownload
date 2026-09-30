package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun AboutScreen() {
    var decimalInput by remember { mutableStateOf("1") }

    val decimalVal = decimalInput.toDoubleOrNull() ?: 0.0
    val katha = decimalVal / 1.65
    val bigha = decimalVal / 33.0
    val acre = decimalVal / 100.0
    val sqFeet = decimalVal * 435.6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Land Document & Survey Archive Utility",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Official Disclaimer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Legal Disclaimer & Notice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = stringResource(R.string.about_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Bangladesh Land Unit Converter
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Bangladesh Land Unit Converter / পরিমাপ রূপান্তর",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = decimalInput,
                    onValueChange = { decimalInput = it },
                    label = { Text("Decimal / শতাংশ (Shotok)") },
                    modifier = Modifier.fillMaxWidth().testTag("converter_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    UnitConversionRow("কাঠা (Katha)", "%.3f কাঠা (1 Katha ≈ 1.65 Decimal)".format(katha))
                    UnitConversionRow("বিঘা (Bigha)", "%.4f বিঘা (1 Bigha = 20 Katha = 33 Decimal)".format(bigha))
                    UnitConversionRow("একর (Acre)", "%.4f একর (1 Acre = 100 Decimal = 3 Bigha 8 Chatak)".format(acre))
                    UnitConversionRow("বর্গফুট (Sq. Feet)", "%.1f বর্গফুট (1 Decimal ≈ 435.6 Sq Ft)".format(sqFeet))
                }
            }
        }

        // Survey Types Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Guide to Bangladesh Land Surveys (জরিপ পরিচিতি)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SurveyTypeItem(
                    title = "CS (Cadastral Survey) / সিএস জরিপ",
                    subtitle = "১৮৮৮ – ১৯৪০ খ্রিস্টাব্দ",
                    description = "ব্রিটিশ আমলে প্রণীত ভারতীয় উপমহাদেশের প্রথম বিজ্ঞানসম্মত খতিয়ান ও মৌজা নকশা। জমিসংক্রান্ত বিরোধে এটি আদি ভিত্তি হিসেবে বিবেচিত।"
                )

                SurveyTypeItem(
                    title = "SA (State Acquisition Survey) / এসএ জরিপ",
                    subtitle = "১৯৫৬ – ১৯৬৩ খ্রিস্টাব্দ",
                    description = "১৯৫০ সালের জমিদারী অধিগ্রহণ ও প্রজাস্বত্ব আইনের আওতায় জমিদারী প্রথা বিলুপ্তির পর দ্রুত প্রস্তুতকৃত খতিয়ান।"
                )

                SurveyTypeItem(
                    title = "RS (Revisional Survey) / আরএস জরিপ",
                    subtitle = "১৯৬৬ – চলমান",
                    description = "এসএ জরিপের ভুলভ্রান্তি সংশোধনে সরেজমিনে পরিমাপ করে তৈরি প্রামাণ্য ও সর্বাধিক গ্রহণযোগ্য আধুনিক জরিপ।"
                )

                SurveyTypeItem(
                    title = "BS / City Survey / বিএস ও সিটি জরিপ",
                    subtitle = "১৯৯৮ – বর্তমান",
                    description = "বাংলাদেশ জরিপ ও ঢাকা সিটি জরিপ। এটি সরকারিভাবে মুদ্রিত বা হালনাগাদ ডিজিটাল ডাটাবেজের সর্বশেষ রেকর্ড।"
                )

                SurveyTypeItem(
                    title = "ই-নামজারি (Mutation) ও খাজনা দাখিলা",
                    subtitle = "হালনাগাদ মালিকানা ও কর",
                    description = "জমি ক্রয়, হেবা বা ওয়ারিশসূত্রে প্রাপ্তির পর এসি ল্যান্ড অফিসে নামজারির মাধ্যমে নতুন খতিয়ান সৃষ্টি ও বার্ষিক এলডি ট্যাক্স প্রদানের রশিদ।"
                )
            }
        }
    }
}

@Composable
fun SurveyTypeItem(title: String, subtitle: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun UnitConversionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}
