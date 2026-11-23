package com.multiclone.app.domain

enum class CompatibilityTier {
    LIKELY_COMPATIBLE,
    CAUTION_COMPLEX_APP,
    HIGH_RISK_INTEGRITY_OR_GMS
}

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val baseApkPath: String,
    val splitApkPaths: List<String>,
    val targetSdk: Int,
    val isSystemApp: Boolean,
    val compatibilityTier: CompatibilityTier,
    val compatibilityNotes: String
)

data class VirtualInstanceRecord(
    val instanceId: String,
    val targetPackageName: String,
    val customLabel: String,
    val slotIndex: Int,
    val dataRootPath: String,
    val createdAtEpochMs: Long,
    val lastLaunchStatus: String = "NOT_LAUNCHED"
)
