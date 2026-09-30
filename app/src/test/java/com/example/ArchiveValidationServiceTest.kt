package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DocumentType
import com.example.data.MouzaInfo
import com.example.storage.ArchiveValidationService
import com.example.storage.DiscrepancyType
import com.example.storage.DocumentDirectoryService
import com.example.storage.StorageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArchiveValidationServiceTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var docDirService: DocumentDirectoryService
    private lateinit var validationService: ArchiveValidationService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = StorageManager(context)
        docDirService = DocumentDirectoryService(context)
        validationService = ArchiveValidationService(context, storageManager, docDirService)
    }

    @Test
    fun `validateAndPrepareArchive repairs missing files and guarantees 0 discrepancies`() = runBlocking {
        val mouzas = listOf(
            MouzaInfo("Savar", "সাভার", "JL 01"),
            MouzaInfo("Ashulia", "আশুলিয়া", "JL 02")
        )
        val docTypes = setOf(DocumentType.CS, DocumentType.RS)

        val report = validationService.validateAndPrepareArchive(
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            activeMouzas = mouzas,
            activeDocTypes = docTypes
        )

        // 2 mouzas * 2 types = 4 expected items
        assertEquals(4, report.totalExpected)
        assertEquals(4, report.totalVerified)
        assertEquals(0, report.discrepancyCount)
        assertTrue(report.isReadyForMasterZip)
        assertEquals(4, report.verifiedRecords.size)

        // Verify each physical file exists on disk and is non-empty
        report.verifiedRecords.forEach { record ->
            val file = File(record.filePath)
            assertTrue(file.exists())
            assertTrue(file.length() > 0)
        }
    }

    @Test
    fun `scanAndDetectDiscrepancies detects checksum mismatch and missing files against manifest`() = runBlocking {
        val mouzas = listOf(MouzaInfo("Savar", "সাভার", "JL 01"))
        val docTypes = setOf(DocumentType.CS)

        // 1. Initial scan when no files exist -> reports missing file
        val initialScan = validationService.scanAndDetectDiscrepancies(
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            activeMouzas = mouzas,
            activeDocTypes = docTypes
        )
        assertTrue(initialScan.hasDiscrepancies)
        assertEquals(1, initialScan.discrepancies.size)
        assertEquals(DiscrepancyType.MISSING_FILE, initialScan.discrepancies[0].issueType)

        // 2. Prepare/repair files
        val report = validationService.validateAndPrepareArchive(
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            activeMouzas = mouzas,
            activeDocTypes = docTypes
        )
        assertEquals(0, report.discrepancyCount)

        // 3. Scan again -> 0 discrepancies
        val validScan = validationService.scanAndDetectDiscrepancies(
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            activeMouzas = mouzas,
            activeDocTypes = docTypes
        )
        assertFalse(validScan.hasDiscrepancies)
        assertEquals(1, validScan.totalFoundValid)

        // 4. Intentionally corrupt the file on disk to trigger SHA-256 mismatch
        val verifiedRecord = report.verifiedRecords.first()
        val file = File(verifiedRecord.filePath)
        file.appendText("corrupted_data_injection")

        val corruptedScan = validationService.scanAndDetectDiscrepancies(
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            activeMouzas = mouzas,
            activeDocTypes = docTypes
        )
        assertTrue(corruptedScan.hasDiscrepancies)
        assertEquals(DiscrepancyType.CHECKSUM_MISMATCH, corruptedScan.discrepancies[0].issueType)
    }
}
