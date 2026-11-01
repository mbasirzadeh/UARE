package com.example.uare.core

import android.os.Build
import android.os.Process
import com.example.uare.api.RuntimeEnvironment
import com.example.uare.api.RuntimeType
import java.io.File

/**
 * Runtime detector that discovers process and platform context using multiple verified signals.
 * Never relies on UID 1000 alone to assert system_server execution.
 */
class RuntimeDetector {

    fun detectEnvironment(): RuntimeEnvironment {
        val pid = Process.myPid()
        val uid = Process.myUid()
        val apiLevel = Build.VERSION.SDK_INT
        val signals = mutableListOf<String>()

        var detectedProcessName: String? = null

        // Signal 1: Android P+ Process.getProcessName()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pName = android.app.Application.getProcessName()
                detectedProcessName = pName
                signals.add("Application.getProcessName(): $pName")
            }
        } catch (ignored: Throwable) {}

        // Signal 2: /proc/self/cmdline
        try {
            val cmdline = File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n')
            if (cmdline.isNotEmpty()) {
                if (detectedProcessName == null) detectedProcessName = cmdline
                signals.add("/proc/self/cmdline: $cmdline")
            }
        } catch (ignored: Throwable) {
            signals.add("/proc/self/cmdline: inaccessible (SELinux restricted)")
        }

        // Signal 3: ActivityThread.currentProcessName() via reflection
        try {
            val actThreadClass = Class.forName("android.app.ActivityThread")
            val method = actThreadClass.getDeclaredMethod("currentProcessName")
            method.isAccessible = true
            val actPName = method.invoke(null) as? String
            if (actPName != null) {
                if (detectedProcessName == null) detectedProcessName = actPName
                signals.add("ActivityThread.currentProcessName(): $actPName")
            }
        } catch (ignored: Throwable) {}

        // Signal 4: UID & GID Checks
        signals.add("Process UID: $uid, PID: $pid")

        // Signal 5: SystemServer presence check
        var hasSystemServerClass = false
        try {
            Class.forName("com.android.server.SystemServer")
            hasSystemServerClass = true
            signals.add("com.android.server.SystemServer class resolved")
        } catch (ignored: ClassNotFoundException) {
            signals.add("com.android.server.SystemServer not found")
        }

        // Signal 6: LocalServices availability
        var hasLocalServices = false
        try {
            val localServicesClass = Class.forName("com.android.server.LocalServices")
            val getServiceMethod = localServicesClass.getMethod("getService", Class::class.java)
            hasLocalServices = true
            signals.add("com.android.server.LocalServices accessible")
        } catch (ignored: Throwable) {
            signals.add("com.android.server.LocalServices not accessible")
        }

        // Signal 7: ActivityManager / Context check
        signals.add("SDK Target: API $apiLevel (${Build.VERSION.RELEASE}), VM: ${System.getProperty("java.vm.version")}")

        // Evaluation
        val isSystemServerName = detectedProcessName == "system_server"
        val isSystemUid = uid == 1000

        val (runtimeType, confidence) = when {
            isSystemServerName && isSystemUid && hasSystemServerClass -> {
                Pair(RuntimeType.SYSTEM_SERVER, 0.99f)
            }
            isSystemServerName && isSystemUid -> {
                Pair(RuntimeType.SYSTEM_SERVER, 0.85f)
            }
            isSystemUid && !isSystemServerName -> {
                // UID 1000 system app (e.g. Settings, Keyguard, OEM system app), NOT system_server!
                Pair(RuntimeType.SYSTEM_SERVICE, 0.75f)
            }
            detectedProcessName?.contains(":remote") == true || detectedProcessName?.contains(":service") == true -> {
                Pair(RuntimeType.CUSTOM_PROCESS, 0.80f)
            }
            detectedProcessName != null && uid >= 10000 -> {
                Pair(RuntimeType.APPLICATION, 0.95f)
            }
            else -> {
                Pair(RuntimeType.APPLICATION, 0.70f)
            }
        }

        return RuntimeEnvironment(
            processName = detectedProcessName,
            pid = pid,
            uid = uid,
            apiLevel = apiLevel,
            runtimeType = runtimeType,
            confidence = confidence,
            detectionSignals = signals
        )
    }
}
