package com.example.data

import com.google.gson.annotations.SerializedName

data class Division(
    @SerializedName("name") val name: String,
    @SerializedName("bnName") val bnName: String,
    @SerializedName("districts") val districts: List<District> = emptyList()
)

data class District(
    @SerializedName("name") val name: String,
    @SerializedName("bnName") val bnName: String,
    @SerializedName("upazilas") val upazilas: List<String> = emptyList()
)

data class MouzaInfo(
    val name: String,
    val bnName: String,
    val jlNo: String,
    val isSelected: Boolean = true
)

enum class DocumentType(
    val code: String,
    val enLabel: String,
    val bnLabel: String,
    val description: String
) {
    CS(
        code = "CS",
        enLabel = "CS Khatian",
        bnLabel = "সিএস খতিয়ান",
        description = "Cadastral Survey (1888–1940) First survey record"
    ),
    SA(
        code = "SA",
        enLabel = "SA Khatian",
        bnLabel = "এসএ খতিয়ান",
        description = "State Acquisition Survey (1956–1963)"
    ),
    RS(
        code = "RS",
        enLabel = "RS Khatian",
        bnLabel = "আরএস খতিয়ান",
        description = "Revisional Survey (Generally modern & authoritative)"
    ),
    BS(
        code = "BS",
        enLabel = "BS / BRS / City",
        bnLabel = "বিএস / সিটি জরিপ",
        description = "Bangladesh Survey / Dhaka City Survey (Ongoing/Latest)"
    ),
    MUTATION(
        code = "MUTATION",
        enLabel = "Namjari / Mutation",
        bnLabel = "নামজারি খতিয়ান",
        description = "E-Namjari / Mutation Order & Duplicate Khatian"
    ),
    DAKHILA(
        code = "DAKHILA",
        enLabel = "Khajna Dakhila",
        bnLabel = "খাজনা দাখিলা",
        description = "Land Development Tax Receipt (LD Tax)"
    ),
    MOUZA_MAP(
        code = "MOUZA_MAP",
        enLabel = "Mouza Map / Naksha",
        bnLabel = "মৌজা নকশা / সিট",
        description = "Cadastral parcel layout sheet map"
    ),
    DEED(
        code = "DEED",
        enLabel = "Deed / Dolil",
        bnLabel = "রেজিস্ট্রি দলিল",
        description = "Registered land deed / Sub-Kabala / Heba"
    ),
    OTHER(
        code = "OTHER",
        enLabel = "Other Record",
        bnLabel = "অন্যান্য নথি",
        description = "Court decree, warrant, or miscellaneous document"
    );

    companion object {
        fun fromCode(code: String): DocumentType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: OTHER
        }

        fun guessFromFileName(fileName: String): DocumentType {
            val lower = fileName.lowercase()
            return when {
                lower.contains("cs") || lower.contains("সিএস") -> CS
                lower.contains("sa") || lower.contains("এসএ") -> SA
                lower.contains("rs") || lower.contains("আরএস") -> RS
                lower.contains("bs") || lower.contains("brs") || lower.contains("city") || lower.contains("বিএস") || lower.contains("সিটি") -> BS
                lower.contains("namjari") || lower.contains("mutation") || lower.contains("নামজারি") || lower.contains("নামজারী") -> MUTATION
                lower.contains("dakhila") || lower.contains("khajna") || lower.contains("tax") || lower.contains("দাখিলা") || lower.contains("খাজনা") -> DAKHILA
                lower.contains("map") || lower.contains("naksha") || lower.contains("sheet") || lower.contains("নকশা") || lower.contains("সিট") || lower.contains("ম্যাপ") -> MOUZA_MAP
                lower.contains("deed") || lower.contains("dolil") || lower.contains("দলিল") -> DEED
                else -> OTHER
            }
        }
    }
}

data class LandRecord(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val sha256: String,
    val division: String,
    val district: String,
    val upazila: String,
    val mouza: String,
    val docType: DocumentType,
    val khatianOrPlotNo: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sourceUrl: String = ""
)
