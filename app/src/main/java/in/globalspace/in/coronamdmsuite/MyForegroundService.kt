package `in`.globalspace.`in`.coronamdmsuite

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import `in`.globalspace.`in`.coronamdmsuite.Worker.PackageCheckWorker




class MyForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MyForegroundService", "Service started")

        startForegroundService()
         val packageNames = mutableListOf<String>(
                "com.mergeall.medvol",
                "com.maesupport",
                "com.vcs.ecubixecp"
        )

        // Create and enqueue the WorkRequest
        val workRequest = OneTimeWorkRequest.Builder(PackageCheckWorker::class.java)
            .setInputData(workDataOf("packages" to packageNames.toTypedArray()))
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)

        return START_STICKY // Ensures the service continues running
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "PackageCheckChannel")
            .setContentTitle("Running Background Task")
            .setContentText("The app is running a task in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent())
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun getPendingIntent(): PendingIntent {
        val notificationIntent = Intent(this, MainActivity::class.java) // Replace with your main activity

        // Use FLAG_IMMUTABLE to meet the requirements of Android 12 and above
        return PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
        )
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    "PackageCheckChannel",
                    "Package Check Notifications",
                    NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
