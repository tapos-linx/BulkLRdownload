package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class MouzaInfo(
    val id: String = "",
    val name: String,
    val bnName: String,
    val jlNo: String,
    val isSelected: Boolean = true
)

@Serializable
data class District(
    val name: String,
    val bnName: String,
    val upazilas: List<String>,
    val mouzas: Map<String, List<MouzaInfo>> = emptyMap(),
    val isPriority: Boolean = false
)

@Serializable
data class Division(
    val name: String,
    val bnName: String,
    val districts: List<District>
)

enum class CatalogueSourceType(val labelBn: String, val labelEn: String) {
    OFFICIAL_BUNDLED("সরকারি/ভেরিফায়েড ক্যাটালগ (Verified Area Catalogue)", "Verified Bundled Catalogue"),
    IMPORTED_CSV("ইউজার CSV ইম্পোর্ট (Imported CSV)", "User Imported CSV"),
    IMPORTED_JSON("ইউজার JSON ইম্পোর্ট (Imported JSON)", "User Imported JSON"),
    CACHED("লোকাল ক্যাশ (Cached)", "Cached Catalogue")
}

enum class SurveyCategory(val code: String, val nameBn: String, val nameEn: String) {
    CS("CS", "সিএস (CS - Cadastral Survey)", "CS Survey"),
    SA("SA", "এসএ (SA - State Acquisition)", "SA Survey"),
    RS("RS", "আরএস (RS - Revisional Survey)", "RS Survey"),
    BS_BRS("BRS", "বিএস/বিআরএস (BS/BRS - Bangladesh Survey)", "BS/BRS Survey"),
    DIARA("DIARA", "দিয়ারা জরিপ (Diara Survey)", "Diara Survey"),
    PETTY("PETTY", "পেটি জরিপ (Petty Survey)", "Petty Survey")
}

enum class RecordCategory(val code: String, val nameBn: String, val nameEn: String) {
    KHATIAN("KHATIAN", "খতিয়ান / পর্চা (Khatian Volume)", "Khatian Record"),
    MOUZA_MAP("MOUZA_MAP", "মৌজা ম্যাপ / নকশা (Mouza Map Sheet)", "Mouza Map"),
    DAG_INDEX("DAG_INDEX", "দাগ সূচি (Plot/Dag Index)", "Plot Index")
}

enum class JobStatus(val labelBn: String, val badgeColorHex: Long) {
    PENDING("অপেক্ষমাণ (PENDING)", 0xFF64748B),
    DOWNLOADING("ডাউনলোড হচ্ছে (DOWNLOADING)", 0xFF0284C7),
    DOWNLOADED_UNVALIDATED("ডাউনলোড সম্পন্ন - যাচাই বাকি", 0xFFD97706),
    VALIDATED("যাচাইকৃত ও সংরক্ষিত (VALIDATED)", 0xFF059669),
    VERIFIED("উৎস নিশ্চিতকৃত (VERIFIED)", 0xFF047857),
    DUPLICATE("ডুপ্লিকেট ফাইল (DUPLICATE)", 0xFF7C3AED),
    NOT_FOUND("পাওয়া যায়নি (NOT_FOUND)", 0xFF94A3B8),
    FAILED("ব্যর্থ হয়েছে (FAILED)", 0xFFDC2626),
    INCOMPLETE("অসম্পূর্ণ (INCOMPLETE)", 0xFFF59E0B),
    AUTH_REQUIRED("লগইন প্রয়োজন (AUTH_REQUIRED)", 0xFFE11D48),
    ACCESS_RESTRICTED("প্রবেশাধিকার সীমিত (ACCESS_RESTRICTED)", 0xFFBE123C),
    SOURCE_UNVERIFIED("উৎস অযাচাইকৃত (SOURCE_UNVERIFIED)", 0xFF475569)
}
