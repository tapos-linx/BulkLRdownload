package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey val jobId: String,
    val sourceName: String = "VERIFIED_LOCAL_PORTAL_ADAPTER",
    val sourceRecordId: String,
    val division: String,
    val district: String,
    val districtBn: String,
    val upazila: String,
    val upazilaBn: String,
    val mouzaId: String,
    val mouzaName: String,
    val mouzaNameBn: String,
    val jlNo: String,
    val surveyType: String,
    val recordType: String,
    val status: String, // JobStatus.name
    val progress: Int = 0,
    val fileSizeBytes: Long = 0L,
    val sha256Checksum: String = "",
    val validationResult: String = "",
    val attemptCount: Int = 0,
    val safeErrorCode: String = "",
    val archiveRelativePath: String = "",
    val userSelectedArchiveUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: String,
    val eventType: String,
    val messageBn: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DownloadJobDao {
    @Query("SELECT * FROM download_jobs ORDER BY createdAt DESC")
    fun observeAllJobs(): Flow<List<DownloadJobEntity>>

    @Query("SELECT * FROM download_jobs ORDER BY createdAt ASC")
    suspend fun getAllJobsSnapshot(): List<DownloadJobEntity>

    @Query("SELECT * FROM download_jobs WHERE status IN ('PENDING', 'INCOMPLETE') ORDER BY createdAt ASC")
    suspend fun getPendingOrIncompleteJobs(): List<DownloadJobEntity>

    @Query("SELECT * FROM download_jobs WHERE sha256Checksum = :sha256 AND sha256Checksum != '' LIMIT 1")
    suspend fun findByChecksum(sha256: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE sourceRecordId = :sourceRecordId AND status = 'VALIDATED' LIMIT 1")
    suspend fun findValidatedBySourceRecordId(sourceRecordId: String): DownloadJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<DownloadJobEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: DownloadJobEntity)

    @Update
    suspend fun updateJob(job: DownloadJobEntity)

    @Query("UPDATE download_jobs SET status = :newStatus, updatedAt = :timestamp WHERE status = :oldStatus")
    suspend fun transitionStatuses(oldStatus: String, newStatus: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM download_jobs")
    suspend fun clearAllJobs()

    @Query("DELETE FROM download_jobs WHERE jobId = :jobId")
    suspend fun deleteJobById(jobId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun observeRecentAuditLogs(): Flow<List<AuditLogEntity>>
}

@Database(
    entities = [DownloadJobEntity::class, AuditLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LRgetBDDatabase : RoomDatabase() {
    abstract fun downloadJobDao(): DownloadJobDao

    companion object {
        @Volatile
        private var INSTANCE: LRgetBDDatabase? = null

        fun getInstance(context: Context): LRgetBDDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LRgetBDDatabase::class.java,
                    "lrgetbd_archive.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
