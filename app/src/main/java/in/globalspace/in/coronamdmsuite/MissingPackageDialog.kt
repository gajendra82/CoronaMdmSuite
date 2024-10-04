package `in`.globalspace.`in`.coronamdmsuite

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MissingPackageDialog(
    missingPackages: List<String>,
    onDismiss: () -> Unit,
    onInstallPackage: (String) -> Unit,
) {
    Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
        ) {
            Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
            ) {
                Text(
                        text = "Missing Packages",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                Text("The following packages are missing:")
                Spacer(modifier = Modifier.height(8.dp))

                for (packageName in missingPackages) {
                    Text(text = packageName)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                        onClick = {
                            for (packageName in missingPackages) {



                                onInstallPackage(packageName)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Install Missing Packages")
                }
            }
        }
    }
}


fun installPackage(context: Context, packageName: String) {
    try {


        // Try to open the app in the Play Store
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // If the Play Store app is not available, open in a browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/*fun installPackage(context: Context, packageName: String) {
    // Intent to prompt installation from Play Store or other installation method
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    context.startActivity(intent)
}*/
