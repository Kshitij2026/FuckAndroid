package com.multiclone.app.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.multiclone.app.androidintegration.InstalledAppScanner
import com.multiclone.app.data.InstanceRegistryStore
import com.multiclone.app.domain.InstalledAppInfo
import com.multiclone.app.utils.PocLogger
import com.multiclone.app.virtualization.VirtualSandboxEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PocRunnerActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private val logs = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scanner = InstalledAppScanner(this)
        val registry = InstanceRegistryStore(this)
        val engine = VirtualSandboxEngine(this, registry)

        logView = TextView(this).apply {
            setPadding(36, 36, 36, 36)
            textSize = 13f
            text = "MultiClone Phase 3 PoC Ready on Nothing Phone 3a."
        }

        val scanBtn = Button(this).apply {
            text = "1. Scan Installed Apps"
            setOnClickListener {
                lifecycleScope.launch {
                    val apps = withContext(Dispatchers.IO) { scanner.scanLaunchableApps() }
                    appendLog("Scanned ${apps.size} launchable apps.")
                    apps.take(8).forEach {
                        appendLog(" - ${it.label} (${it.packageName}) [${it.compatibilityTier}]")
                    }
                    val target = findTargetForProbe(apps)
                    if (target != null) {
                        appendLog("Selected probe target: ${target.label} (${target.packageName})")
                    }
                }
            }
        }

        val createAndTestBtn = Button(this).apply {
            text = "2. Create 3 Virtual Slots & Probe"
            setOnClickListener {
                lifecycleScope.launch {
                    val apps = withContext(Dispatchers.IO) { scanner.scanLaunchableApps() }
                    val target = findTargetForProbe(apps)
                    if (target == null) {
                        appendLog("No suitable target app found for PoC.")
                        return@launch
                    }
                    for (slot in 0..2) {
                        val res = withContext(Dispatchers.IO) {
                            engine.createVirtualInstance(
                                app = target,
                                customLabel = "${target.label} Virtual ${slot + 1}",
                                slotIndex = slot
                            )
                        }
                        res.onSuccess { rec ->
                            appendLog("Slot $slot ready: ${rec.customLabel} -> ${rec.dataRootPath}")
                        }.onFailure { err ->
                            appendLog("Slot $slot error: ${err.message}")
                        }
                    }
                    val first = registry.loadAll().firstOrNull { it.targetPackageName == target.packageName }
                    if (first != null) {
                        engine.launchInStubProcess(target, first).onSuccess { msg ->
                            appendLog(msg)
                        }.onFailure { err ->
                            appendLog("Launch dispatch error: ${err.message}")
                        }
                    }
                }
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            addView(scanBtn)
            addView(createAndTestBtn)
            addView(ScrollView(this@PocRunnerActivity).apply { addView(logView) })
        }

        setContentView(root)
    }

    private fun findTargetForProbe(apps: List<InstalledAppInfo>): InstalledAppInfo? {
        return apps.firstOrNull { it.packageName.contains("whatsapp", ignoreCase = true) }
            ?: apps.firstOrNull { it.packageName.contains("instagram", ignoreCase = true) }
            ?: apps.firstOrNull()
    }

    private fun appendLog(line: String) {
        logs.add(PocLogger.info(line))
        logView.text = logs.takeLast(35).joinToString("
")
    }
}
