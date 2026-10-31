package com.example.storage

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class DownloadNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "lrgetbd_download_channel"
        const val CHANNEL_NAME = "LRgetBD ডাউনলোড ও আর্কাইভ নোটিফিকেশন"
        const val NOTIFICATION_ID_PROMPT = 2101
        const val NOTIFICATION_ID_PROGRESS = 2102
        const val NOTIFICATION_ID_COMPLETE = 2103
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "বাংলাদেশ ল্যান্ড রেকর্ড ডাউনলোড নিশ্চিতকরণ, অগ্রগতি এবং ফোল্ডার সংরক্ষণের নোটিফিকেশন"
            }
            manager?.createNotificationChannel(channel)
        }
    }

    fun notifyDownloadPrompt(upazilaBn: String, mouzaCount: Int, folderLabel: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("LRgetBD: ডাউনলোড নিশ্চিতকরণ ($upazilaBn)")
            .setContentText("আপনি কি $mouzaCount টি মৌজার ফাইল ফোন মেমোরিতে ($folderLabel) ডাউনলোড করতে চান?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID_PROMPT, notification)
        } catch (_: SecurityException) {
        }
    }

    fun notifyQueueProgress(completed: Int, total: Int, currentMouzaBn: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("LRgetBD: ল্যান্ড রেকর্ড সংগ্রহ চলছে ($completed/$total)")
            .setContentText("চলমান মৌজা: $currentMouzaBn | SHA-256 যাচাইকরণ সক্রিয়")
            .setProgress(total, completed, false)
            .setOngoing(completed < total)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            manager.notify(NOTIFICATION_ID_PROGRESS, notification)
        } catch (_: SecurityException) {
        }
    }

    fun notifyBatchCompleted(validatedCount: Int, duplicateCount: Int, failedCount: Int, savePath: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.cancel(NOTIFICATION_ID_PROGRESS)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("LRgetBD: সংগ্রহ সম্পন্ন ($validatedCount টি যাচাইকৃত)")
            .setContentText("সংরক্ষিত স্থান: $savePath (ডুপ্লিকেট: $duplicateCount, ব্যর্থ: $failedCount)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID_COMPLETE, notification)
        } catch (_: SecurityException) {
        }
    }
}
