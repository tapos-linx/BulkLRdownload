package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DocumentType
import com.example.data.LandRecord
import com.example.storage.SecureCacheCleaner
import com.example.storage.StorageManager
import com.example.storage.ZipManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureCacheCleanerTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var cacheCleaner: SecureCacheCleaner
    private lateinit var zipManager: ZipManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = StorageManager(context)
        cacheCleaner = SecureCacheCleaner(context, storageManager)
        zipManager = ZipManager(context, storageManager)
    }

    @Test
    fun `securelyClearTemporaryCache wipes processed files and exported zip`() = runBlocking {
        // 1. Create a dummy record on disk
        val dummyDir = File(context.filesDir, "LandArchive/Dhaka/Dhaka/Savar/CS").apply { mkdirs() }
        val dummyFile = File(dummyDir, "test_doc.pdf").apply { writeText("Sample land record bytes for testing") }

        val record = LandRecord(
            id = UUID.randomUUID().toString(),
            fileName = "test_doc.pdf",
            filePath = dummyFile.absolutePath,
            fileSize = dummyFile.length(),
            sha256 = "dummy_sha256",
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            mouza = "Savar",
            docType = DocumentType.CS,
            khatianOrPlotNo = "101",
            timestamp = System.currentTimeMillis()
        )
        storageManager.saveAllRecords(listOf(record))

        // 2. Create a dummy exported zip in cacheDir/exports
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dummyZip = File(exportsDir, "LandArchive_Savar_dummy.zip").apply { writeText("PK dummy zip content") }

        assertTrue(dummyFile.exists())
        assertTrue(dummyZip.exists())

        // 3. Trigger secure cache wipe
        val result = cacheCleaner.securelyClearTemporaryCache(
            processedRecords = listOf(record),
            processedZipFile = dummyZip
        )

        assertTrue(result.success)
        assertTrue(result.clearedFilesCount >= 2)
        assertFalse("Processed document file must be deleted", dummyFile.exists())
        assertFalse("Temporary zip file must be deleted", dummyZip.exists())
        assertTrue("Storage records must be cleared of processed item", storageManager.getAllRecords().isEmpty())
    }

    @Test
    fun `securelyClearTemporaryCache aborts if output is not confirmed`() = runBlocking {
        val dummyDir = File(context.filesDir, "LandArchive/Dhaka/Dhaka/Savar/CS").apply { mkdirs() }
        val dummyFile = File(dummyDir, "test_doc_safe.pdf").apply { writeText("Must be preserved") }

        val record = LandRecord(
            id = UUID.randomUUID().toString(),
            fileName = "test_doc_safe.pdf",
            filePath = dummyFile.absolutePath,
            fileSize = dummyFile.length(),
            sha256 = "safe_sha256",
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            mouza = "Savar",
            docType = DocumentType.CS,
            khatianOrPlotNo = "102",
            timestamp = System.currentTimeMillis()
        )
        storageManager.saveAllRecords(listOf(record))

        val result = cacheCleaner.securelyClearTemporaryCache(
            processedRecords = listOf(record),
            processedZipFile = null,
            outputConfirmed = false // Output not confirmed!
        )

        assertFalse("Cleanup must abort when output stream is not confirmed", result.success)
        assertEquals(0, result.clearedFilesCount)
        assertTrue("File must remain preserved on disk", dummyFile.exists())
        assertEquals(1, storageManager.getAllRecords().size)
    }

    @Test
    fun `LocationRepository returns all mouzas without 6-item limitation`() {
        val repo = com.example.data.LocationRepository(context)

        val savarMouzas = repo.getMouzasForUpazila("Savar")
        assertTrue("Savar should have at least 15 mouzas, found: ${savarMouzas.size}", savarMouzas.size >= 15)
        assertNotEquals("Must not be limited to 6 mouzas", 6, savarMouzas.size)

        val tejgaonMouzas = repo.getMouzasForUpazila("Tejgaon")
        assertTrue("Tejgaon should have at least 14 mouzas, found: ${tejgaonMouzas.size}", tejgaonMouzas.size >= 14)
        assertNotEquals("Must not be limited to 6 mouzas", 6, tejgaonMouzas.size)

        // General arbitrary Upazila test
        val anyUpazilaMouzas = repo.getMouzasForUpazila("Amtali")
        assertTrue("Any upazila should have all authentic mouzas (at least 15), found: ${anyUpazilaMouzas.size}", anyUpazilaMouzas.size >= 15)
        assertNotEquals("Must not be limited to 6 mouzas", 6, anyUpazilaMouzas.size)
    }

    @Test
    fun `exportMasterPackageToPhoneMemory generates master folder and zip successfully`() = runBlocking {
        val dummyDir = File(context.filesDir, "LandArchive/Dhaka/Dhaka/Savar/CS").apply { mkdirs() }
        val dummyFile = File(dummyDir, "Savar_JL01_CS_Record.pdf").apply { writeText("%PDF-1.4 dummy content") }

        val record = LandRecord(
            id = UUID.randomUUID().toString(),
            fileName = "Savar_JL01_CS_Record.pdf",
            filePath = dummyFile.absolutePath,
            fileSize = dummyFile.length(),
            sha256 = storageManager.calculateSha256(dummyFile),
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            mouza = "Savar",
            docType = DocumentType.CS,
            khatianOrPlotNo = "102",
            timestamp = System.currentTimeMillis()
        )

        val zipRes = zipManager.createMasterZip(
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            records = listOf(record)
        )

        val exportRes = zipManager.exportMasterPackageToPhoneMemory(
            upazila = "Savar",
            records = listOf(record),
            zipFile = zipRes.zipFile
        )

        assertTrue("Master package export must report success", exportRes.isSuccess)
        assertTrue("Master zip must be marked exported", exportRes.masterZipExported)
        assertTrue("Physical folder must be marked exported", exportRes.physicalFolderExported)
        val folder = exportRes.exportedPhysicalFolder
        assertNotNull("Physical folder must be created", folder)
        assertTrue("Exported folder must exist", folder?.exists() == true)
        assertTrue("manifest.tsv must exist in master folder", File(folder!!, "manifest.tsv").exists())
    }
}
