package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DocumentType
import com.example.data.LandRecord
import com.example.storage.SecureCacheCleaner
import com.example.storage.StorageManager
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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = StorageManager(context)
        cacheCleaner = SecureCacheCleaner(context, storageManager)
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
}
