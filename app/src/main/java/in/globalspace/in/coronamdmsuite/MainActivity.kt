package `in`.globalspace.`in`.coronamdmsuite

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import `in`.globalspace.`in`.coronamdmsuite.Worker.PackageCheckWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val packageNames = mutableListOf<String>(
                "com.mergeall.medvol",    // Replace with actual package name
                "com.maesupport",    // Replace with actual package name
                "com.vcs.ecubixecp"
        // Replace with actual package name
        )
        super.onCreate(savedInstanceState)
        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }

            PackageCheckerApp(this)
        }
        val intent = Intent()
        val packageName = this.packageName
        val pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            this.startActivity(intent)
        }

        schedulePackageCheckWork(this,packageNames)



/*        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {


                delay(10000)
              //  createNotificationChannel(this@MainActivity)

                val workRequest = OneTimeWorkRequestBuilder<PackageCheckWorker>()
                    .setInputData(workDataOf("packages" to packageNames.toTypedArray()))
                    .build()

                WorkManager.getInstance(this@MainActivity).enqueueUniqueWork(
                        "packageCheckWork",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                )

            }
        }*/


    }

}

// Function to schedule OneTimeWorkRequest to check packages




private fun schedulePackageCheckWork(context: Context, packageNames: List<String>) {

    val workRequest = PeriodicWorkRequestBuilder<PackageCheckWorker>(15, TimeUnit.MINUTES)
        .setInputData(workDataOf("packages" to packageNames.toTypedArray()))
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "packageCheckWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
    )

}










@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PackageCheckerApp(context: Context) {
    var missingPackages by remember { mutableStateOf(emptyList<String>()) }
   // var missingUrl by remember { mutableStateOf(emptyList<String>()) }
    var showDialog by remember { mutableStateOf(false) }



    // Listen for WorkManager's result to update missing packages


    LaunchedEffect(Unit) {


        val workManager = WorkManager.getInstance(context)
        workManager.getWorkInfosForUniqueWorkLiveData("packageCheckWork").observeForever { workInfos ->
            val workInfo = workInfos.firstOrNull()
            if (workInfo != null && workInfo.state == WorkInfo.State.RUNNING) {
                // Task is scheduled, but not yet completed.
             //   if (workInfo.outputData.getBoolean("isSuccess", false)) {
                    val missing = getMissingPackages(context)


                    if (missing.isNotEmpty()) {
                        missingPackages = missing
                        showDialog = true
                    }
                //}
            }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Package Checker") },
                )
            }
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("Checking packages in background...")

            if (showDialog) {
                MissingPackageDialog(
                        missingPackages = missingPackages,
                        onDismiss = { showDialog = false }
                ) { packageName ->
                    installPackage(context, packageName)

                }
            }
        }
    }
}
/*
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "PackageCheckChannel"
        val channelName = "Package Check Notifications"
        val importance = NotificationManager.IMPORTANCE_LOW

        // Get the NotificationManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the notification channel already exists
        val existingChannel = notificationManager.getNotificationChannel(channelId)

        // Create the channel only if it doesn't exist
        if (existingChannel == null) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                // Optional: Add description
                description = "Channel for package check notifications"
            }
            // Create the notification channel
            notificationManager.createNotificationChannel(channel)
        }
    }
}
*/


fun createNotificationChannel(context: Context) {



    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "PackageCheckChannel"
        val channelName = "Package Check Notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the channel already exists
        val existingChannel = notificationManager.getNotificationChannel(channelId)

        if (existingChannel == null) { // Only create if it doesn't exist
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(channel)
        }
    }
}



/*@Composable
fun PackageCheckerApp() {
    val context = LocalContext.current
    var missingPackages by remember { mutableStateOf(emptyList<String>()) }
    var showDialog by remember { mutableStateOf(false) }

    // Observe the WorkManager result and show dialog if packages are missing
    LaunchedEffect(Unit) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("PackageCheckWork")
            .observeForever { workInfos ->
                workInfos?.firstOrNull()?.let { workInfo ->
                    if (workInfo.state.isFinished) {
                        val missing = workInfo.outputData.getStringArray("missingPackages")?.toList() ?: emptyList()

                        // Update the UI with missing packages
                        missingPackages = missing
                        showDialog = missing.isNotEmpty()
                    }
                }
            }
    }

    // Show dialog if packages are missing
    if (showDialog) {
        ShowPackageDialog(missingPackages, context) {
            // Re-check the missing packages
            missingPackages = getMissingPackages(context)
            showDialog = missingPackages.isNotEmpty() // Keep showing dialog if any packages are still missing
        }
    }

    // Main UI content
    Surface(modifier = Modifier.fillMaxSize()) {
        Text(text = "Checking package installation status...", style = MaterialTheme.typography.titleLarge)
    }
}*/



@Composable
fun ShowPackageDialog(missingPackages: List<String>, context: Context, onDismiss: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = if (missingPackages.isEmpty()) "All Packages Installed" else "Missing Packages")
            },
            text = {
                Text(text = if (missingPackages.isEmpty()) {
                    "All required packages have been installed."
                } else {
                    "The following packages are missing:\n${missingPackages.joinToString(", ")}\n\nWould you like to install them?"
                })
            },
            confirmButton = {
                if (missingPackages.isNotEmpty()) {
                    Button(onClick = { installMissingPackages(missingPackages, context) }) {
                        Text("Install")
                    }
                } else {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
    )
}

// Trigger installation via Play Store
private fun installMissingPackages(missingPackages: List<String>, context: Context) {
    missingPackages.forEach { packageName ->
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$packageName")
        }
        context.startActivity(intent)
    }
}



private fun getMissingPackages(context: Context): MutableList<String> {
    // Declare a mutable list to store package names
    val packageNames = mutableListOf(
            "com.mergeall.medvol",
            "com.maesupport",
            "com.vcs.ecubixecp"
    )

  /*  val url1 = "https://play.google.com/store/apps/details?id=com.vcs.ecubixecp&pcampaignid=web_share"
    val url2 = "https://play.google.com/store/apps/details?id=com.maesupport&pcampaignid=web_share"
    val url3 = "https://play.google.com/store/apps/details?id=com.mergeall.medvol&pcampaignid=web_share"

    var missingUrl = mutableListOf(url1,url2,url3)
*/
    // Remove installed packages from the list
    val installedPackages = packageNames.filter { isPackageInstalled(context, it) }
    //compareAppPackageWithUrl(missingUrl,installedPackages)
    packageNames.removeAll(installedPackages)





    // Return the list of remaining (missing) packages
    return packageNames
}

/*
private fun getMissingPackages(context: Context): Pair<List<String>, List<String>> {
    // Declare a mutable list to store package names
    val packageNames = mutableListOf(
            "com.mergeall.medvol",
            "com.maesupport",
            "com.vcs.ecubixecp"
    )

    val url1 = "https://play.google.com/store/apps/details?id=com.vcs.ecubixecp&pcampaignid=web_share"
    val url2 = "https://play.google.com/store/apps/details?id=com.maesupport&pcampaignid=web_share"
    val url3 = "https://play.google.com/store/apps/details?id=com.mergeall.medvol&pcampaignid=web_share"

    var missingUrl = mutableListOf(url1,url2,url3)

    // Remove installed packages from the list
    val installedPackages = packageNames.filter { isPackageInstalled(context, it) }

    val matchingUrls = findMatchingUrls(packageNames, missingUrl)

    // Prepare lists for missing packages and their URLs
    val missingPackages = mutableListOf<String>()
    val correspondingUrls = mutableListOf<String>()

    // Determine missing packages and URLs
    for ((packageName, url) in matchingUrls) {
        packageNames.remove(packageName)
        missingUrl.remove(url)
    }

    // Remaining package names are the missing ones
    missingPackages.addAll(packageNames)

    // Find corresponding URLs for the missing packages
    for (packageName in missingPackages) {
        val url = matchingUrls[packageName]
        if (url != null) {
            correspondingUrls.add(url)
        }
    }





    // Return the list of remaining (missing) packages
    return Pair(missingPackages, correspondingUrls)
}
*/




fun openAppLink(context: Context, appUrl: String) {
    try {
        // Try to open the app link in the Play Store app or a browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Log or handle if the activity is not found
        e.printStackTrace()
    }
}

// Helper function to manually check missing packages
/*private fun getMissingPackages(context: Context): List<String> {
    val packageNames = listOf(
            "com.mergeall.medvol",
            "com.maesupport",
            "com.vcs.ecubixecp"
    )
    return packageNames.filterNot { isPackageInstalled(context, it) }
}*/

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0) != null
    } catch (e: PackageManager.NameNotFoundException) {
        false // Package is not installed
    }
}











