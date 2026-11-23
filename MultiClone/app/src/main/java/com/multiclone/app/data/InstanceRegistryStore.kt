package com.multiclone.app.data

import android.content.Context
import com.multiclone.app.domain.VirtualInstanceRecord
import com.multiclone.app.security.IsolationGuard
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class InstanceRegistryStore(private val context: Context) {

    private val sandboxRoot: File
        get() = File(context.filesDir, "virtual_instances").apply { mkdirs() }

    private val registryFile: File
        get() = File(context.filesDir, "instance_registry.json")

    fun createInstanceDirectories(packageName: String, instanceId: String): File {
        val pkgDir = IsolationGuard.resolveContainedDir(sandboxRoot, packageName)
        val instDir = IsolationGuard.resolveContainedDir(pkgDir, instanceId)
        File(instDir, "data/shared_prefs").mkdirs()
        File(instDir, "data/databases").mkdirs()
        File(instDir, "files").mkdirs()
        File(instDir, "cache").mkdirs()
        File(instDir, "code_cache").mkdirs()
        return instDir
    }

    fun deleteInstance(record: VirtualInstanceRecord): Boolean {
        val pkgDir = IsolationGuard.resolveContainedDir(sandboxRoot, record.targetPackageName)
        val instDir = IsolationGuard.resolveContainedDir(pkgDir, record.instanceId)
        val deletedFiles = if (instDir.exists()) instDir.deleteRecursively() else true

        val updated = loadAll().filterNot { it.instanceId == record.instanceId }
        saveAll(updated)
        return deletedFiles
    }

    fun loadAll(): List<VirtualInstanceRecord> {
        if (!registryFile.exists()) return emptyList()
        val raw = registryFile.readText()
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    VirtualInstanceRecord(
                        instanceId = obj.getString("instanceId"),
                        targetPackageName = obj.getString("targetPackageName"),
                        customLabel = obj.getString("customLabel"),
                        slotIndex = obj.getInt("slotIndex"),
                        dataRootPath = obj.getString("dataRootPath"),
                        createdAtEpochMs = obj.getLong("createdAtEpochMs"),
                        lastLaunchStatus = obj.optString("lastLaunchStatus", "NOT_LAUNCHED")
                    )
                )
            }
        }
    }

    fun upsert(record: VirtualInstanceRecord) {
        val current = loadAll().toMutableList()
        val idx = current.indexOfFirst { it.instanceId == record.instanceId }
        if (idx >= 0) current[idx] = record else current.add(record)
        saveAll(current)
    }

    private fun saveAll(records: List<VirtualInstanceRecord>) {
        val array = JSONArray()
        records.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("instanceId", r.instanceId)
                    put("targetPackageName", r.targetPackageName)
                    put("customLabel", r.customLabel)
                    put("slotIndex", r.slotIndex)
                    put("dataRootPath", r.dataRootPath)
                    put("createdAtEpochMs", r.createdAtEpochMs)
                    put("lastLaunchStatus", r.lastLaunchStatus)
                }
            )
        }
        registryFile.writeText(array.toString(2))
    }
}
