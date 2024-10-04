package `in`.globalspace.`in`.coronamdmsuite.Worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import `in`.globalspace.`in`.coronamdmsuite.MainActivity
import `in`.globalspace.`in`.coronamdmsuite.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext




class PackageCheckWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Show initial "Checking packages" notification in foreground
        Log.d("PackageCheckWorker", "Checking packages")

        createNotificationChannelIfNeeded()



//            setForeground(getInitialForegroundInfo())



        // Your background work logic here
        val packages = inputData.getStringArray("packages") ?: return Result.failure()


        // Check for missing packages
        val missingPackages = checkPackages(packages)

        // Update the notification with missing packages

        if (missingPackages.isEmpty()) {
            Log.d("PackageCheckWorker", "All packages are installed. Stopping notifications.")
            stopForegroundNotification()
            cancelMissingPackagesNotification()
            return Result.success()
        } else {
            // Show updated notification with missing packages
            updateNotificationWithMissingPackages(missingPackages)
            return Result.success(workDataOf("missingPackages" to missingPackages.toTypedArray()))
        }

       /* updateNotificationWithMissingPackages(missingPackages)

        // Prepare output data with the list of missing packages
        val outputData = workDataOf("missingPackages" to missingPackages.toTypedArray())
        return Result.success(outputData)*/
    }

    // Function to check for missing packages
    private fun checkPackages(packages: Array<String>): List<String> {
        return packages.filterNot { isPackageInstalled(it) }
    }

    // Function to check if a specific package is installed
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            applicationContext.packageManager.getPackageInfo(packageName, 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false // Package is not installed
        }
    }

    // Provide an initial notification when starting foreground work
    private fun getInitialForegroundInfo(): ForegroundInfo {
        val notificationId = 1
        val channelId = "PackageCheckChannel"

        // Build initial "Checking packages" notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Checking packages")
            .setTicker("Package check in progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true) // Ensure this is set to true
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    // Create a notification channel if needed (Android 8.0+)
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "PackageCheckChannel"
            val channelName = "Package Check Notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Function to update notification with missing packages
    private fun updateNotificationWithMissingPackages(missingPackages: List<String>) {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        val channelId = "PackageCheckChannel"

        // Log the missing packages for debugging
        Log.d("PackageCheckWorker", "Updating notification with missing packages: $missingPackages")

        // Build updated notification with missing packages
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.putExtra("missingPackages", missingPackages.toTypedArray())
        val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Missing Packages Detected")
            .setContentText("Some required packages are not installed. Tap to install.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(false) // Ensure this is also set for the updated notification
            .setAutoCancel(true)
            .build()

        // Update the existing notification with the same ID
        notificationManager.notify(2, notification)
    }

    private fun stopForegroundNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1) // Assuming 1 is the foreground notification ID
    }

    // Function to cancel missing packages notification
    private fun cancelMissingPackagesNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2) // Assuming 2 is the notification ID for missing packages
    }

}




