package com.multiclone.app.virtualization

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import com.multiclone.app.data.InstanceRegistryStore
import com.multiclone.app.domain.InstalledAppInfo
import com.multiclone.app.domain.VirtualInstanceRecord
import com.multiclone.app.utils.PocLogger
import dalvik.system.PathClassLoader
import java.io.File

class VirtualSandboxEngine(
    private val context: Context,
    private val registryStore: InstanceRegistryStore,
    private val inspector: ApkPackageInspector = ApkPackageInspector(context)
) {

    fun createVirtualInstance(
        app: InstalledAppInfo,
        customLabel: String,
        slotIndex: Int
    ): Result<VirtualInstanceRecord> {
        require(slotIndex in 0..4) { "PoC supports slots 0..4 (up to 5 virtual instances)" }
        val instanceId = "${app.packageName.replace('.', '_')}_slot_$slotIndex"
        val instDir = registryStore.createInstanceDirectories(app.packageName, instanceId)

        val record = VirtualInstanceRecord(
            instanceId = instanceId,
            targetPackageName = app.packageName,
            customLabel = customLabel.ifBlank { "${app.label} Clone ${slotIndex + 1}" },
            slotIndex = slotIndex,
            dataRootPath = instDir.absolutePath,
            createdAtEpochMs = System.currentTimeMillis(),
            lastLaunchStatus = "CREATED"
        )
        registryStore.upsert(record)
        PocLogger.info("Created virtual instance '${record.customLabel}' at ${record.dataRootPath}")
        return Result.success(record)
    }

    fun launchInStubProcess(app: InstalledAppInfo, record: VirtualInstanceRecord): Result<String> {
        val spec = inspector.buildLaunchSpec(app)
        if (spec.apkPaths.isEmpty()) {
            return Result.failure(IllegalStateException("No APK path found for ${app.packageName}"))
        }

        val intent = Intent(context, StubVirtualActivity::class.java).apply {
            putExtra(StubVirtualActivity.EXTRA_INSTANCE_ID, record.instanceId)
            putExtra(StubVirtualActivity.EXTRA_CUSTOM_LABEL, record.customLabel)
            putExtra(StubVirtualActivity.EXTRA_TARGET_PKG, spec.packageName)
            putExtra(StubVirtualActivity.EXTRA_LAUNCH_ACTIVITY, spec.launchActivityName)
            putExtra(StubVirtualActivity.EXTRA_DATA_ROOT, record.dataRootPath)
            putStringArrayListExtra(StubVirtualActivity.EXTRA_APK_PATHS, ArrayList(spec.apkPaths))
        }

        context.startActivity(intent)
        val updated = record.copy(lastLaunchStatus = "DISPATCHED_TO_SLOT_${record.slotIndex}")
        registryStore.upsert(updated)
        return Result.success("Dispatched ${record.customLabel} to stub process :vproc${record.slotIndex}")
    }

    fun probeClassAndResources(
        apkPaths: List<String>,
        launchActivityName: String?,
        optOutDir: File
    ): Result<String> {
        return try {
            optOutDir.mkdirs()
            val classPath = apkPaths.joinToString(File.pathSeparator)
            val classLoader = PathClassLoader(classPath, context.classLoader)

            @Suppress("DEPRECATION")
            val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
            apkPaths.forEach { path ->
                val cookie = addAssetPath.invoke(assetManager, path) as? Int ?: 0
                if (cookie == 0) {
                    PocLogger.warn("addAssetPath returned 0 for $path")
                }
            }

            @Suppress("DEPRECATION")
            val resources = Resources(assetManager, context.resources.displayMetrics, context.resources.configuration)

            val activityLoaded = if (!launchActivityName.isNullOrBlank()) {
                val clazz = classLoader.loadClass(launchActivityName)
                "Loaded class: ${clazz.name}"
            } else {
                "No entry activity resolved; APK/resources loaded (${resources.displayMetrics.widthPixels}px)"
            }

            Result.success("Probe OK across ${apkPaths.size} APK(s). $activityLoaded")
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
