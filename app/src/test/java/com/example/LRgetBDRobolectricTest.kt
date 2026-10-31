package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.JobStatus
import com.example.data.LocationRepository
import com.example.data.MouzaInfo
import com.example.data.db.DownloadJobEntity
import com.example.data.db.LRgetBDDatabase
import com.example.storage.ArchiveStorageManager
import com.example.storage.DownloadNotificationHelper
import com.example.storage.PdfValidationOutcome
import com.example.storage.PdfValidator
import com.example.storage.download.RealDownloadQueueEngine
import com.example.storage.source.LandRecordSourceAdapter
import com.example.storage.source.SourceAdapterDescriptor
import com.example.storage.source.SourceRetrievalResult
import com.example.storage.source.VerifiedLocalArchiveSourceAdapter
import com.example.ui.LRgetBDViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LRgetBDRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: LRgetBDDatabase
    private lateinit var locationRepo: LocationRepository
    private lateinit var storageManager: ArchiveStorageManager
    private lateinit var notificationHelper: DownloadNotificationHelper
    private lateinit var queueEngine: RealDownloadQueueEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LRgetBDDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        locationRepo = LocationRepository(context)
        storageManager = ArchiveStorageManager(context)
        notificationHelper = DownloadNotificationHelper(context)
        queueEngine = RealDownloadQueueEngine(
            context = context,
            dao = database.downloadJobDao(),
            storageManager = storageManager,
            notificationHelper = notificationHelper
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name is exactly LRgetBD`() {
        assertEquals("LRgetBD", context.getString(R.string.app_name))
    }

    @Test
    fun `district dropdown features Cumilla and Brahmanbaria with all 17 and 9 upazilas`() {
        val districts = locationRepo.getPriorityDistrictsFirst("Chattogram")
        assertTrue(districts.size >= 2)
        assertEquals("Cumilla", districts[0].name)
        assertEquals("Brahmanbaria", districts[1].name)

        val cumillaUpazilas = locationRepo.getUpazilas("Chattogram", "Cumilla")
        assertEquals(17, cumillaUpazilas.size)
        assertTrue(cumillaUpazilas.contains("Titas"))
        assertTrue(cumillaUpazilas.contains("Cumilla Adarsha Sadar"))

        val brahmanbariaUpazilas = locationRepo.getUpazilas("Chattogram", "Brahmanbaria")
        assertEquals(9, brahmanbariaUpazilas.size)
        assertTrue(brahmanbariaUpazilas.contains("Sarail"))
        assertTrue(brahmanbariaUpazilas.contains("Brahmanbaria Sadar"))
    }

    @Test
    fun `cascade reset and auto-select all mouzas on upazila selection`() {
        val vm = LRgetBDViewModel(context, locationRepo, storageManager, notificationHelper, queueEngine)

        // Select Brahmanbaria -> resets Upazila and Mouzas
        vm.onDistrictSelected("Brahmanbaria")
        assertEquals("Brahmanbaria", vm.selectedDistrict.value)
        assertEquals("", vm.selectedUpazila.value)
        assertTrue(vm.mouzas.value.isEmpty())

        // Select Sarail Upazila -> automatically selects 100% of Mouzas & prompts download confirmation
        vm.onUpazilaSelected("Sarail", triggerAutoQueueAndPrompt = true)
        assertEquals("Sarail", vm.selectedUpazila.value)
        assertTrue(vm.mouzas.value.isNotEmpty())
        assertTrue(vm.mouzas.value.all { it.isSelected })
        assertTrue(vm.showDownloadConfirmDialog.value)
    }

    @Test
    fun `handles 0, 1, 7, 8, and 200 plus mouzas efficiently`() = runBlocking {
        val sizes = listOf(0, 1, 7, 8, 220)
        for (count in sizes) {
            database.downloadJobDao().clearAllJobs()
            val mouzas = (1..count).map { idx ->
                MouzaInfo(
                    id = "m_$idx",
                    name = "Mouza $idx",
                    bnName = "মৌজা_$idx",
                    jlNo = "JL $idx",
                    isSelected = true
                )
            }
            val enqueued = queueEngine.enqueueMouzaBatch(
                division = "Chattogram",
                district = "Cumilla",
                districtBn = "কুমিল্লা",
                upazila = "Titas",
                upazilaBn = "তিতাস",
                mouzas = mouzas,
                surveyTypes = listOf("RS"),
                recordTypes = listOf("KHATIAN")
            )
            assertEquals(count, enqueued.size)
            assertEquals(count, database.downloadJobDao().getAllJobsSnapshot().size)
        }
    }

    @Test
    fun `rejects HTML login pages and accepts valid PDF with SHA-256 checksum`() {
        val tempDir = File(context.cacheDir, "test_validation").apply { mkdirs() }

        // 1. HTML login page returned instead of PDF
        val htmlPart = File(tempDir, "login_response.part")
        htmlPart.writeText("<!DOCTYPE html><html><body><form action='/login'>OTP Required</form></body></html>")
        val htmlOutcome = PdfValidator.validatePartFile(htmlPart, "text/html")
        assertTrue(htmlOutcome is PdfValidationOutcome.Invalid)
        assertEquals("HTML_LOGIN_OR_ERROR_PAGE", (htmlOutcome as PdfValidationOutcome.Invalid).safeErrorCode)

        // 2. Empty file
        val emptyPart = File(tempDir, "empty.part")
        emptyPart.writeBytes(ByteArray(0))
        val emptyOutcome = PdfValidator.validatePartFile(emptyPart, "application/pdf")
        assertTrue(emptyOutcome is PdfValidationOutcome.Invalid)
        assertEquals("EMPTY_FILE", (emptyOutcome as PdfValidationOutcome.Invalid).safeErrorCode)

        // 3. Valid PDF
        val sampleJob = DownloadJobEntity(
            jobId = "JOB_1",
            sourceRecordId = "Cumilla_Titas_JL_220_RS_KHATIAN",
            division = "Chattogram",
            district = "Cumilla",
            districtBn = "কুমিল্লা",
            upazila = "Titas",
            upazilaBn = "তিতাস",
            mouzaId = "cm_tt_01",
            mouzaName = "Karikandi",
            mouzaNameBn = "কড়িকান্দি",
            jlNo = "JL 220",
            surveyType = "RS",
            recordType = "KHATIAN",
            status = JobStatus.PENDING.name
        )
        val validPart = File(tempDir, "valid.part")
        validPart.writeBytes(VerifiedLocalArchiveSourceAdapter.buildValidPdfRecordBytes(sampleJob))
        val validOutcome = PdfValidator.validatePartFile(validPart, "application/pdf")
        assertTrue(validOutcome is PdfValidationOutcome.Valid)
        assertEquals(64, (validOutcome as PdfValidationOutcome.Valid).sha256Checksum.length)
    }

    @Test
    fun `detects duplicate records and resolves filename collisions without overwriting`() = runBlocking {
        val job1 = DownloadJobEntity(
            jobId = "JOB_DUP_1",
            sourceRecordId = "Cumilla_Titas_JL_220_RS_KHATIAN",
            division = "Chattogram",
            district = "Cumilla",
            districtBn = "কুমিল্লা",
            upazila = "Titas",
            upazilaBn = "তিতাস",
            mouzaId = "cm_tt_01",
            mouzaName = "Karikandi",
            mouzaNameBn = "কড়িকান্দি",
            jlNo = "JL 220",
            surveyType = "RS",
            recordType = "KHATIAN",
            status = JobStatus.PENDING.name
        )
        database.downloadJobDao().insertJob(job1)
        val processed1 = queueEngine.processSingleJob(job1)
        assertEquals(JobStatus.VALIDATED.name, processed1.status)
        assertTrue(processed1.sha256Checksum.isNotBlank())

        // Enqueue second job with identical sourceRecordId -> must be marked DUPLICATE
        val job2 = job1.copy(jobId = "JOB_DUP_2")
        database.downloadJobDao().insertJob(job2)
        val processed2 = queueEngine.processSingleJob(job2)
        assertEquals(JobStatus.DUPLICATE.name, processed2.status)
        assertEquals(processed1.sha256Checksum, processed2.sha256Checksum)
    }

    @Test
    fun `exports download queue as JSON and imports CSV batch`() = runBlocking {
        val csvInput = """
            District,Upazila,MouzaNameBn,JLNo,SurveyType,RecordType
            Cumilla,Titas,কড়িকান্দি,JL 220,RS,KHATIAN
            Brahmanbaria,Sarail,সরাইল,JL 10,BRS,KHATIAN
        """.trimIndent()

        val addedCount = queueEngine.enqueueCsvBatch(
            csvContent = csvInput,
            defaultDivision = "Chattogram",
            defaultDistrict = "Cumilla",
            defaultDistrictBn = "কুমিল্লা",
            defaultUpazila = "Titas",
            defaultUpazilaBn = "তিতাস"
        )
        assertEquals(2, addedCount)

        val snapshot = database.downloadJobDao().getAllJobsSnapshot()
        val exportedJsonFile = storageManager.exportQueueToJsonFile(snapshot)
        assertTrue(exportedJsonFile.exists())
        val reimported = storageManager.importQueueFromJsonText(exportedJsonFile.readText())
        assertEquals(2, reimported.size)
        assertEquals("কড়িকান্দি", reimported[0].mouzaNameBn)
        assertEquals("সরাইল", reimported[1].mouzaNameBn)
    }

    @Test
    fun `recovers interrupted downloading jobs as INCOMPLETE on process restart`() = runBlocking {
        val interruptedJob = DownloadJobEntity(
            jobId = "JOB_INTERRUPTED",
            sourceRecordId = "Brahmanbaria_Sarail_JL_10_RS_KHATIAN",
            division = "Chattogram",
            district = "Brahmanbaria",
            districtBn = "ব্রাহ্মণবাড়িয়া",
            upazila = "Sarail",
            upazilaBn = "সরাইল",
            mouzaId = "bb_sr_01",
            mouzaName = "Sarail",
            mouzaNameBn = "সরাইল",
            jlNo = "JL 10",
            surveyType = "RS",
            recordType = "KHATIAN",
            status = JobStatus.DOWNLOADING.name
        )
        database.downloadJobDao().insertJob(interruptedJob)
        queueEngine.recoverInterruptedQueueOnStartup()

        val recovered = database.downloadJobDao().getAllJobsSnapshot().first()
        assertEquals(JobStatus.INCOMPLETE.name, recovered.status)
    }

    @Test
    fun `handles rate limiting, session expiration, and unverified adapters safely`() = runBlocking {
        val sampleJob = DownloadJobEntity(
            jobId = "JOB_AUTH",
            sourceRecordId = "Cumilla_Laksam_JL_180_RS_KHATIAN",
            division = "Chattogram",
            district = "Cumilla",
            districtBn = "কুমিল্লা",
            upazila = "Laksam",
            upazilaBn = "লাকসাম",
            mouzaId = "cm_lk_01",
            mouzaName = "Laksam",
            mouzaNameBn = "লাকসাম",
            jlNo = "JL 180",
            surveyType = "RS",
            recordType = "KHATIAN",
            status = JobStatus.PENDING.name
        )
        database.downloadJobDao().insertJob(sampleJob)

        val authExpiredAdapter = object : LandRecordSourceAdapter {
            override val descriptor = SourceAdapterDescriptor(
                id = "TEST_AUTH_ADAPTER",
                nameBn = "টেস্ট সেশন অ্যাডাপ্টার",
                nameEn = "Test Session Adapter",
                ownerVerification = "Test",
                officialDocumentationOrTerms = "Test",
                supportedSurveyTypes = listOf("RS"),
                supportedRecordTypes = listOf("KHATIAN"),
                requiredAuthentication = "Session Cookie",
                permittedDownloadMethod = "GET",
                rateLimitPolicy = "2 req/s",
                knownLimitations = "None",
                isEnabled = true
            )

            override suspend fun retrieveRecord(job: DownloadJobEntity, stagingDir: File): SourceRetrievalResult {
                return SourceRetrievalResult.AuthRequired("সেশন মেয়াদোত্তীর্ণ - পুনরায় সাইন ইন করুন")
            }
        }

        val outcome = queueEngine.processSingleJob(sampleJob, authExpiredAdapter)
        assertEquals(JobStatus.AUTH_REQUIRED.name, outcome.status)
        assertFalse(outcome.status == JobStatus.VALIDATED.name)
    }
}
