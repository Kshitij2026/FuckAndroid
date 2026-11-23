package com.multiclone.app.virtualization

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.multiclone.app.domain.InstalledAppInfo

data class TargetLaunchSpec(
    val packageName: String,
    val launchActivityName: String?,
    val apkPaths: List<String>
)

class ApkPackageInspector(private val context: Context) {

    fun buildLaunchSpec(app: InstalledAppInfo): TargetLaunchSpec {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
        val activityName = resolveActivityName(pm, launchIntent)

        val allApks = buildList {
            if (app.baseApkPath.isNotBlank()) add(app.baseApkPath)
            addAll(app.splitApkPaths)
        }.distinct()

        return TargetLaunchSpec(
            packageName = app.packageName,
            launchActivityName = activityName,
            apkPaths = allApks
        )
    }

    private fun resolveActivityName(pm: PackageManager, intent: Intent?): String? {
        if (intent == null) return null
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, 0)
        }
        return resolved?.activityInfo?.name
    }
}
