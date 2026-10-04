package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DocumentType
import com.example.data.LandRecord
import com.example.storage.ImportCandidate
import com.example.storage.StorageManager
import com.example.storage.ZipManager
import com.example.ui.screens.ImportViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportViewModelTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var zipManager: ZipManager
    private lateinit var viewModel: ImportViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = StorageManager(context)
        zipManager = ZipManager(context, storageManager)
        viewModel = ImportViewModel(storageManager, zipManager)
    }

    private fun createCandidate(
        fileName: String,
        district: String,
        mouza: String,
        docType: DocumentType = DocumentType.RS,
        size: Long = 1024L
    ): ImportCandidate {
        return ImportCandidate(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            data = byteArrayOf(1, 2, 3, 4),
            size = size,
            sha256 = "dummy_sha256_${fileName}",
            docType = docType,
            division = "Dhaka",
            district = district,
            upazila = "Savar",
            mouza = mouza
        )
    }

    @Test
    fun `categorizeImportedRecordsIntoFolders groups correctly by district and mouza`() {
        val records = listOf(
            createCandidate("khatian_1.pdf", "Dhaka", "Savar"),
            createCandidate("khatian_2.pdf", "Dhaka", "Savar"),
            createCandidate("khatian_3.pdf", "Dhaka", "Ashulia"),
            createCandidate("khatian_4.pdf", "Gazipur", "Tongi"),
            createCandidate("khatian_5.pdf", "Gazipur", "Joydebpur")
        )

        val categorized = viewModel.categorizeImportedRecordsIntoFolders(records)

        assertEquals(2, categorized.size)
        assertTrue(categorized.containsKey("Dhaka"))
        assertTrue(categorized.containsKey("Gazipur"))

        val dhakaMouzas = categorized["Dhaka"]
        assertNotNull(dhakaMouzas)
        assertEquals(2, dhakaMouzas?.size)
        assertEquals(2, dhakaMouzas?.get("Savar")?.size)
        assertEquals(1, dhakaMouzas?.get("Ashulia")?.size)

        val gazipurMouzas = categorized["Gazipur"]
        assertNotNull(gazipurMouzas)
        assertEquals(2, gazipurMouzas?.size)
        assertEquals(1, gazipurMouzas?.get("Tongi")?.size)
        assertEquals(1, gazipurMouzas?.get("Joydebpur")?.size)
    }

    @Test
    fun `categorizeImportedRecordsIntoFolders handles empty and blank attributes gracefully`() {
        val records = listOf(
            createCandidate("unassigned_1.pdf", "", ""),
            createCandidate("unassigned_2.pdf", "   ", "   "),
            createCandidate("partial_1.pdf", "Dhaka", "")
        )

        val categorized = viewModel.categorizeImportedRecordsIntoFolders(records)

        assertTrue(categorized.containsKey(ImportViewModel.DEFAULT_UNASSIGNED_DISTRICT))
        assertTrue(categorized.containsKey("Dhaka"))

        val unassignedDistrict = categorized[ImportViewModel.DEFAULT_UNASSIGNED_DISTRICT]
        assertNotNull(unassignedDistrict)
        assertEquals(2, unassignedDistrict?.get(ImportViewModel.DEFAULT_UNASSIGNED_MOUZA)?.size)

        val dhakaMouzas = categorized["Dhaka"]
        assertEquals(1, dhakaMouzas?.get(ImportViewModel.DEFAULT_UNASSIGNED_MOUZA)?.size)
    }

    @Test
    fun `categorizeImportedRecordsIntoOfflineArchiveFolders generates structured archive folders`() {
        val records = listOf(
            createCandidate("doc1.pdf", "Dhaka", "Savar", size = 2000L),
            createCandidate("doc2.pdf", "Dhaka", "Savar", size = 3000L),
            createCandidate("doc3.pdf", "Dhaka", "Dhamrai", size = 1500L),
            createCandidate("doc4.pdf", "Gazipur", "Sreepur", size = 4000L)
        )

        val archiveFolders = viewModel.categorizeImportedRecordsIntoOfflineArchiveFolders(records)

        assertEquals(3, archiveFolders.size)

        val dhakaDhamrai = archiveFolders.firstOrNull { it.district == "Dhaka" && it.mouza == "Dhamrai" }
        assertNotNull(dhakaDhamrai)
        assertEquals("Dhaka/Dhamrai", dhakaDhamrai?.relativeFolderPath)
        assertEquals(1, dhakaDhamrai?.totalFiles)
        assertEquals(1500L, dhakaDhamrai?.totalSizeBytes)

        val dhakaSavar = archiveFolders.firstOrNull { it.district == "Dhaka" && it.mouza == "Savar" }
        assertNotNull(dhakaSavar)
        assertEquals("Dhaka/Savar", dhakaSavar?.relativeFolderPath)
        assertEquals(2, dhakaSavar?.totalFiles)
        assertEquals(5000L, dhakaSavar?.totalSizeBytes)

        val gazipurSreepur = archiveFolders.firstOrNull { it.district == "Gazipur" && it.mouza == "Sreepur" }
        assertNotNull(gazipurSreepur)
        assertEquals("Gazipur/Sreepur", gazipurSreepur?.relativeFolderPath)
        assertEquals(1, gazipurSreepur?.totalFiles)
        assertEquals(4000L, gazipurSreepur?.totalSizeBytes)
    }

    @Test
    fun `categorizeRecordsByDistrictAndMouza preserves folder hierarchy for UI`() {
        val records = listOf(
            createCandidate("fileA.pdf", "Chattogram", "Hathazari"),
            createCandidate("fileB.pdf", "Chattogram", "Raozan"),
            createCandidate("fileC.pdf", "Sylhet", "Golapganj")
        )

        viewModel.setCandidates(records)
        val districtFolders = viewModel.categorizedFolders.value

        assertEquals(2, districtFolders.size)
        val chattogram = districtFolders.firstOrNull { it.districtName == "Chattogram" }
        assertNotNull(chattogram)
        assertEquals(2, chattogram?.mouzaFolders?.size)
        assertEquals(2, chattogram?.totalRecordsCount)

        val sylhet = districtFolders.firstOrNull { it.districtName == "Sylhet" }
        assertNotNull(sylhet)
        assertEquals(1, sylhet?.mouzaFolders?.size)
        assertEquals(1, sylhet?.totalRecordsCount)
    }

    @Test
    fun `categorizeSavedRecordsIntoFolders categorizes LandRecord items`() {
        val savedRecords = listOf(
            LandRecord(
                id = "1",
                fileName = "khatian_rs.pdf",
                filePath = "/path/1",
                fileSize = 1000L,
                sha256 = "hash1",
                division = "Dhaka",
                district = "Faridpur",
                upazila = "Bhanga",
                mouza = "Hamdi",
                docType = DocumentType.RS
            ),
            LandRecord(
                id = "2",
                fileName = "khatian_cs.pdf",
                filePath = "/path/2",
                fileSize = 1200L,
                sha256 = "hash2",
                division = "Dhaka",
                district = "Faridpur",
                upazila = "Bhanga",
                mouza = "Hamdi",
                docType = DocumentType.CS
            )
        )

        val categorized = viewModel.categorizeSavedRecordsIntoFolders(savedRecords)
        assertEquals(1, categorized.size)
        assertTrue(categorized.containsKey("Faridpur"))
        assertEquals(2, categorized["Faridpur"]?.get("Hamdi")?.size)

        val archiveFolders = viewModel.categorizeSavedRecordsIntoOfflineArchiveFolders(savedRecords)
        assertEquals(1, archiveFolders.size)
        assertEquals("Faridpur/Hamdi", archiveFolders[0].relativeFolderPath)
        assertEquals(2, archiveFolders[0].totalFiles)
        assertEquals(2200L, archiveFolders[0].totalSizeBytes)
    }
}
