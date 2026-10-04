package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.DocumentType
import com.example.storage.StorageManager
import com.example.storage.download.DownloadProgress
import com.example.storage.download.DownloadRequest
import com.example.storage.download.DownloadStatus
import com.example.storage.download.LandRecordDownloadService
import com.example.storage.download.OkHttpDownloadClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LandRecordDownloadServiceTest {

    private lateinit var context: Context
    private lateinit var downloadService: LandRecordDownloadService
    private lateinit var storageManager: StorageManager
    private var httpServer: HttpServer? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        LandRecordDownloadService.resetInstance()
        downloadService = LandRecordDownloadService(context)
        storageManager = StorageManager(context)
        storageManager.clearAllDownloadedData()
    }

    @After
    fun tearDown() {
        httpServer?.stop(0)
        LandRecordDownloadService.resetInstance()
        storageManager.clearAllDownloadedData()
    }

    @Test
    fun `enqueueDownload schedules WorkManager task with proper tags and inputs`() {
        val request = DownloadRequest(
            url = "https://example.gov.bd/cs_101.pdf",
            fileName = "Savar_CS_Khatian_101.pdf",
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            mouza = "Ashulia",
            docType = DocumentType.CS,
            khatianOrPlotNo = "101",
            requiresWifi = true,
            isForeground = true
        )

        val workId = downloadService.enqueueDownload(request)
        assertNotNull("Work ID should not be null", workId)

        val workInfo = WorkManager.getInstance(context).getWorkInfoById(workId).get()
        assertNotNull("WorkInfo should exist in WorkManager", workInfo)
        assertTrue(
            "State should be ENQUEUED or BLOCKED",
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.BLOCKED
        )
        assertTrue(
            "Should have download tag",
            workInfo.tags.contains(LandRecordDownloadService.TAG_LAND_RECORD_DOWNLOAD)
        )
        assertTrue(
            "Should have doc type tag",
            workInfo.tags.contains("doc_${DocumentType.CS.code}")
        )
    }

    @Test
    fun `enqueueBatch enqueues multiple requests`() {
        val requests = listOf(
            DownloadRequest(
                url = "https://example.com/record1.pdf",
                fileName = "record1.pdf",
                division = "Dhaka",
                district = "Dhaka",
                upazila = "Savar",
                mouza = "Savar Sadar",
                docType = DocumentType.RS
            ),
            DownloadRequest(
                url = "https://example.com/record2.pdf",
                fileName = "record2.pdf",
                division = "Dhaka",
                district = "Dhaka",
                upazila = "Savar",
                mouza = "Birulia",
                docType = DocumentType.SA
            )
        )

        val workIds = downloadService.enqueueBatch(requests)
        assertEquals(2, workIds.size)
        assertNotEquals(workIds[0], workIds[1])
    }

    @Test
    fun `OkHttp streaming download computes SHA256 and calls progress callbacks`() {
        runBlocking {
            val testContent = "SAMPLE LAND RECORD PDF DATA CONTENT STREAMING OVER OKHTTP".repeat(200)
            val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
            server.createContext("/test_khatian.pdf") { exchange ->
                val bytes = testContent.toByteArray()
                exchange.responseHeaders.set("Content-Type", "application/pdf")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
            httpServer = server

            val url = "http://localhost:${server.address.port}/test_khatian.pdf"
            val targetFile = File(context.cacheDir, "test_download_${System.currentTimeMillis()}.pdf")

            var progressCalls = 0
            var lastPercentage = -1
            var totalBytesReported = 0L

            val client = OkHttpDownloadClient()
            val result = client.downloadToFile(url, targetFile) { bytesRead, totalBytes, percent ->
                progressCalls++
                lastPercentage = percent
                totalBytesReported = totalBytes
            }

            assertTrue("Download should succeed", result.isSuccess)
            val fileInfo = result.getOrThrow()

            assertTrue("Target file should exist", targetFile.exists())
            assertEquals(testContent.toByteArray().size.toLong(), fileInfo.fileSize)
            assertEquals(targetFile.length(), fileInfo.fileSize)
            assertNotNull(fileInfo.sha256)
            assertTrue("SHA256 should have 64 hex characters", fileInfo.sha256.length == 64)
            assertTrue("Progress should have been reported", progressCalls > 0)
            assertEquals("Final progress should be 100%", 100, lastPercentage)

            targetFile.delete()
        }
    }

    @Test
    fun `StorageManager registerDownloadedFile creates record in LandArchive directory`() {
        val tempFile = File(context.cacheDir, "temp_reg_${System.currentTimeMillis()}.pdf").apply {
            writeText("%PDF-1.5 Land Record Mock Content")
        }

        val saveResult = storageManager.registerDownloadedFile(
            downloadedFile = tempFile,
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            mouza = "Ashulia",
            docType = DocumentType.BS,
            khatianOrPlotNo = "999",
            rawFileName = "Savar_BS_Ashulia_999.pdf",
            sourceUrl = "https://example.gov.bd/bs_999.pdf"
        )

        assertTrue(saveResult is com.example.storage.SaveResult.Success)
        val record = (saveResult as com.example.storage.SaveResult.Success).record

        assertEquals("Savar_BS_Ashulia_999.pdf", record.fileName)
        assertEquals("Savar", record.upazila)
        assertEquals(DocumentType.BS, record.docType)
        assertTrue(File(record.filePath).exists())

        // Ensure record exists in manifest
        val allRecords = storageManager.getAllRecords()
        assertTrue(allRecords.any { it.id == record.id })
    }

    @Test
    fun `cancelDownload cancels WorkManager task`() {
        val request = DownloadRequest(
            url = "https://example.com/cancel_test.pdf",
            fileName = "cancel_test.pdf",
            division = "Dhaka",
            district = "Dhaka",
            upazila = "Savar",
            mouza = "Ashulia",
            docType = DocumentType.RS
        )

        val workId = downloadService.enqueueDownload(request)
        downloadService.cancelDownload(workId)

        val workInfo = WorkManager.getInstance(context).getWorkInfoById(workId).get()
        assertTrue(
            "State should be CANCELLED",
            workInfo.state == WorkInfo.State.CANCELLED
        )
    }
}
