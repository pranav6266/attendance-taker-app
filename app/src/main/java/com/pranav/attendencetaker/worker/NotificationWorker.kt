package com.pranav.attendencetaker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pranav.attendencetaker.R
import com.pranav.attendencetaker.data.FirestoreRepository
import com.pranav.attendencetaker.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestoreRepo = FirestoreRepository()
    private val settingsRepo = SettingsRepository(context)

    override suspend fun doWork(): Result {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        // 1. Check Preferences
        val morningEnabled = settingsRepo.morningReminderFlow.first()
        val eveningEnabled = settingsRepo.eveningReminderFlow.first()

        // 2. Logic for Morning Reminder (Around 6:15 AM)
        // We define a window (e.g., 6:00 - 6:30) because background work isn't exact
        if (morningEnabled && hour == 6 && minute in 0..45) {
            val todayLog = firestoreRepo.getTodayLog()
            if (todayLog == null) {
                sendNotification(
                    1,
                    "Class Today?",
                    "Hi Sensei! Don't forget to take attendance if you have class today."
                )
            }
        }

        // 3. Logic for Evening Reminder (Around 7:00 PM)
        // Window (7:00 - 7:45)
        if (eveningEnabled && hour == 19 && minute in 0..45) {
            val todayLog = firestoreRepo.getTodayLog()
            // If log exists but is NOT finalized
            if (todayLog != null && !todayLog.finalized) {
                sendNotification(
                    2,
                    "Finalize Attendance",
                    "You haven't finalized the class log yet. Tap to wrap up!"
                )
            }
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "attendance_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Make sure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    companion object {
        fun schedule(context: Context) {
            // Run every 15 minutes to check if we are in the time window
            // This is a simple, battery-efficient way to handle "approximate" times
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_attendance_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}