package com.multiclone.app.androidintegration

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.multiclone.app.domain.CompatibilityTier
import com.multiclone.app.domain.InstalledAppInfo

class InstalledAppScanner(private val context: Context) {

    fun scanLaunchableApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        return resolveInfos
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { appInfo -> toInstalledAppInfo(pm, appInfo) }
            .sortedBy { it.label.lowercase() }
    }

    private fun toInstalledAppInfo(pm: PackageManager, appInfo: ApplicationInfo): InstalledAppInfo {
        val label = pm.getApplicationLabel(appInfo)?.toString() ?: appInfo.packageName
        val splits = appInfo.splitSourceDirs?.toList().orEmpty()
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val (tier, notes) = classifyRisk(appInfo.packageName, splits, isSystem)

        return InstalledAppInfo(
            packageName = appInfo.packageName,
            label = label,
            baseApkPath = appInfo.sourceDir.orEmpty(),
            splitApkPaths = splits,
            targetSdk = appInfo.targetSdkVersion,
            isSystemApp = isSystem,
            compatibilityTier = tier,
            compatibilityNotes = notes
        )
    }

    private fun classifyRisk(
        pkg: String,
        splits: List<String>,
        isSystem: Boolean
    ): Pair<CompatibilityTier, String> {
        val lower = pkg.lowercase()
        return when {
            isSystem -> CompatibilityTier.HIGH_RISK_INTEGRITY_OR_GMS to
                "System package; likely tied to OS UID/signature."
            listOf("bank", "pay", "upi", "wallet", "finance", "crypto").any { lower.contains(it) } ->
                CompatibilityTier.HIGH_RISK_INTEGRITY_OR_GMS to
                    "Financial/payment app; Play Integrity or anti-tamper checks likely block virtualization."
            listOf("whatsapp", "instagram", "facebook", "snapchat", "telegram").any { lower.contains(it) } ->
                CompatibilityTier.CAUTION_COMPLEX_APP to
                    "Complex social/messaging app; requires testing GMS, providers, splits (${splits.size}), and push behavior."
            splits.isNotEmpty() ->
                CompatibilityTier.CAUTION_COMPLEX_APP to
                    "Uses ${splits.size} split APK(s); class/resource loader must include split paths."
            else ->
                CompatibilityTier.LIKELY_COMPATIBLE to
                    "Standard single-APK candidate for PoC class/resource sandbox testing."
        }
    }
}
