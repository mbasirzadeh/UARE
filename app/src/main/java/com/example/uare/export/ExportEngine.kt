package com.example.uare.export

import com.example.uare.api.RuntimeEnvironment
import com.example.uare.binder.SystemServiceInfo
import com.example.uare.discovery.DiscoveredClass
import com.example.uare.monitor.MemorySnapshot
import com.example.uare.monitor.ThreadInfoModel
import com.example.uare.snapshot.RuntimeSnapshot

class ExportEngine {

    fun exportSnapshotToJson(snapshot: RuntimeSnapshot): String {
        return buildString {
            append("{\n")
            append("  \"id\": \"${snapshot.id}\",\n")
            append("  \"timestampMs\": ${snapshot.timestampMs},\n")
            append("  \"environment\": {\n")
            append("    \"processName\": \"${snapshot.environment.processName}\",\n")
            append("    \"pid\": ${snapshot.environment.pid},\n")
            append("    \"uid\": ${snapshot.environment.uid},\n")
            append("    \"runtimeType\": \"${snapshot.environment.runtimeType}\",\n")
            append("    \"apiLevel\": ${snapshot.environment.apiLevel},\n")
            append("    \"confidence\": ${snapshot.environment.confidence}\n")
            append("  },\n")
            append("  \"memory\": {\n")
            append("    \"usedMb\": ${snapshot.memory.usedMemoryMb},\n")
            append("    \"maxMb\": ${snapshot.memory.maxMemoryMb},\n")
            append("    \"usagePercent\": ${snapshot.memory.usagePercentage}\n")
            append("  },\n")
            append("  \"threadCount\": ${snapshot.threads.size},\n")
            append("  \"serviceCount\": ${snapshot.services.size},\n")
            append("  \"classLoaderCount\": ${snapshot.classLoaders.size}\n")
            append("}")
        }
    }

    fun exportEnvironmentMarkdown(env: RuntimeEnvironment, mem: MemorySnapshot, threadCount: Int, serviceCount: Int): String {
        return buildString {
            append("# Universal Android Runtime Explorer (UARE) Diagnostic Report\n\n")
            append("### Environment Summary\n")
            append("- **Process Name:** `${env.processName ?: "unknown"}`\n")
            append("- **PID / UID:** `${env.pid}` / `${env.uid}`\n")
            append("- **Runtime Type:** **${env.runtimeType}** (Confidence: ${(env.confidence * 100).toInt()}%)\n")
            append("- **Android OS:** API ${env.apiLevel} (${env.androidVersion})\n")
            append("- **Build Fingerprint:** `${env.buildFingerprint}`\n")
            append("- **Device Model:** ${env.manufacturer} ${env.model}\n")
            append("- **64-bit Process:** ${env.is64Bit}\n\n")

            append("### Memory & Telemetry\n")
            append("- **Heap Used:** ${String.format("%.2f", mem.usedMemoryMb)} MB / ${String.format("%.2f", mem.maxMemoryMb)} MB (${String.format("%.1f", mem.usagePercentage)}%)\n")
            append("- **Available CPU Cores:** ${mem.availableProcessors}\n")
            append("- **Active Threads:** $threadCount\n")
            append("- **Discovered Services:** $serviceCount\n\n")

            append("### Detection Signals\n")
            for (signal in env.detectionSignals) {
                append("- $signal\n")
            }
        }
    }
}
