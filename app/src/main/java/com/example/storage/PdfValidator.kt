package com.example.storage

import java.io.File
import java.security.MessageDigest

sealed class PdfValidationOutcome {
    data class Valid(
        val sha256Checksum: String,
        val fileSizeBytes: Long,
        val detailsBn: String = "PDF সিগনেচার (%PDF-) ও ইন্টিগ্রিটি যাচাই সফল"
    ) : PdfValidationOutcome()

    data class Invalid(
        val safeErrorCode: String,
        val reasonBn: String
    ) : PdfValidationOutcome()
}

object PdfValidator {

    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // "%PDF-"

    fun validatePartFile(partFile: File, mimeType: String = "application/pdf"): PdfValidationOutcome {
        if (!partFile.exists()) {
            return PdfValidationOutcome.Invalid(
                safeErrorCode = "FILE_MISSING",
                reasonBn = "ডাউনলোডকৃত অস্থায়ী (.part) ফাইলটি পাওয়া যায়নি"
            )
        }

        val length = partFile.length()
        if (length <= 0L) {
            return PdfValidationOutcome.Invalid(
                safeErrorCode = "EMPTY_FILE",
                reasonBn = "ফাইলটি খালি (0 Bytes) - প্রত্যাখ্যাত"
            )
        }

        val headerBytes = ByteArray(minOf(length.toInt(), 512))
        partFile.inputStream().use { input ->
            input.read(headerBytes)
        }

        val headerText = String(headerBytes, Charsets.UTF_8).trimStart()

        // Reject HTML login pages or error pages returned instead of PDFs
        if (headerText.startsWith("<!DOCTYPE html", ignoreCase = true) ||
            headerText.startsWith("<html", ignoreCase = true) ||
            headerText.contains("<form", ignoreCase = true) ||
            headerText.contains("login", ignoreCase = true) && !headerText.startsWith("%PDF-")
        ) {
            return PdfValidationOutcome.Invalid(
                safeErrorCode = "HTML_LOGIN_OR_ERROR_PAGE",
                reasonBn = "PDF এর পরিবর্তে HTML লগইন বা এরর পেজ এসেছে - ফাইলটি প্রত্যাখ্যাত হয়েছে"
            )
        }

        if (mimeType.contains("text/html", ignoreCase = true)) {
            return PdfValidationOutcome.Invalid(
                safeErrorCode = "INVALID_MIME_HTML",
                reasonBn = "অবৈধ MIME টাইপ (text/html) সনাক্ত হয়েছে"
            )
        }

        // Verify %PDF- signature in first 5 bytes
        if (headerBytes.size < 5 || !startsWithPdfMagic(headerBytes)) {
            return PdfValidationOutcome.Invalid(
                safeErrorCode = "INVALID_PDF_SIGNATURE",
                reasonBn = "বৈধ PDF সিগনেচার (%PDF-) পাওয়া যায়নি"
            )
        }

        // Verify basic EOF marker or minimum structural size
        if (length < 64L) {
            return PdfValidationOutcome.Invalid(
                safeErrorCode = "TRUNCATED_PDF",
                reasonBn = "অসম্পূর্ণ বা ক্ষতিগ্রস্ত PDF ফাইল"
            )
        }

        val sha256 = computeSha256(partFile)
        return PdfValidationOutcome.Valid(
            sha256Checksum = sha256,
            fileSizeBytes = length
        )
    }

    private fun startsWithPdfMagic(bytes: ByteArray): Boolean {
        for (i in PDF_MAGIC.indices) {
            if (bytes[i] != PDF_MAGIC[i]) return false
        }
        return true
    }

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun sanitizeFileNameSegment(raw: String): String {
        // Preserve Bangla Unicode letters, digits, English letters, underscores, hyphens, and spaces
        val cleaned = raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
        return if (cleaned.isBlank()) "Record" else cleaned
    }
}
