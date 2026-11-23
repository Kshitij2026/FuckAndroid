package com.multiclone.app.virtualization

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.multiclone.app.utils.PocLogger
import java.io.File

class StubVirtualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val instanceId = intent.getStringExtra(EXTRA_INSTANCE_ID).orEmpty()
        val label = intent.getStringExtra(EXTRA_CUSTOM_LABEL).orEmpty()
        val targetPkg = intent.getStringExtra(EXTRA_TARGET_PKG).orEmpty()
        val launchActivity = intent.getStringExtra(EXTRA_LAUNCH_ACTIVITY)
        val dataRoot = intent.getStringExtra(EXTRA_DATA_ROOT).orEmpty()
        val apkPaths = intent.getStringArrayListExtra(EXTRA_APK_PATHS).orEmpty()

        val sandboxEngine = VirtualSandboxEngine(
            context = applicationContext,
            registryStore = com.multiclone.app.data.InstanceRegistryStore(applicationContext)
        )
        val optDir = File(dataRoot, "dex_opt")
        val probeResult = sandboxEngine.probeClassAndResources(apkPaths, launchActivity, optDir)

        val report = buildString {
            appendLine("Virtual Stub Host Active")
            appendLine("Instance: $label ($instanceId)")
            appendLine("Target Package: $targetPkg")
            appendLine("Isolated Data Dir: $dataRoot")
            appendLine("APK Count: ${apkPaths.size}")
            appendLine()
            probeResult.fold(
                onSuccess = { msg ->
                    appendLine("RESULT: $msg")
                    appendLine("Note: Direct activity start of external third-party activities inside a foreign UID requires context/provider/permission proxying; this PoC verifies class & resource loading isolation.")
                },
                onFailure = { err ->
                    appendLine("RESULT: FAILED - ${err.javaClass.simpleName}: ${err.message}")
                    appendLine("This is why real compatibility testing is critical on Nothing OS 5.0.")
                }
            )
        }

        PocLogger.info(report.replace('
', ' | '))

        val textView = TextView(this).apply {
            setPadding(48, 64, 48, 64)
            textSize = 14f
            text = report
        }
        setContentView(ScrollView(this).apply { addView(textView) })
    }

    companion object {
        const val EXTRA_INSTANCE_ID = "extra_instance_id"
        const val EXTRA_CUSTOM_LABEL = "extra_custom_label"
        const val EXTRA_TARGET_PKG = "extra_target_pkg"
        const val EXTRA_LAUNCH_ACTIVITY = "extra_launch_activity"
        const val EXTRA_DATA_ROOT = "extra_data_root"
        const val EXTRA_APK_PATHS = "extra_apk_paths"
    }
}
