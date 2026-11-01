package com.example.uare.snapshot

import com.example.uare.api.RuntimeEnvironment
import com.example.uare.binder.SystemServiceInfo
import com.example.uare.discovery.ClassLoaderInfo
import com.example.uare.monitor.MemorySnapshot
import com.example.uare.monitor.ThreadInfoModel

data class RuntimeSnapshot(
    val id: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val environment: RuntimeEnvironment,
    val memory: MemorySnapshot,
    val threads: List<ThreadInfoModel>,
    val classLoaders: List<ClassLoaderInfo>,
    val services: List<SystemServiceInfo>,
    val registeredObjectCount: Int,
    val label: String = "Snapshot"
)

data class SnapshotDiff(
    val baseSnapshotId: String,
    val targetSnapshotId: String,
    val durationBetweenMs: Long,
    val memoryDeltaMb: Float,
    val newThreads: List<String>,
    val terminatedThreads: List<String>,
    val threadStateChanges: List<String>,
    val newServices: List<String>,
    val removedServices: List<String>,
    val summary: String
)

class DiffEngine {

    fun compareSnapshots(base: RuntimeSnapshot, target: RuntimeSnapshot): SnapshotDiff {
        val duration = target.timestampMs - base.timestampMs
        val memDelta = target.memory.usedMemoryMb - base.memory.usedMemoryMb

        val baseThreadNames = base.threads.associateBy { it.name }
        val targetThreadNames = target.threads.associateBy { it.name }

        val newThreads = targetThreadNames.keys.filter { !baseThreadNames.containsKey(it) }
        val terminatedThreads = baseThreadNames.keys.filter { !targetThreadNames.containsKey(it) }

        val threadStateChanges = mutableListOf<String>()
        for ((name, targetTh) in targetThreadNames) {
            val baseTh = baseThreadNames[name]
            if (baseTh != null && baseTh.state != targetTh.state) {
                threadStateChanges.add("$name: ${baseTh.state} -> ${targetTh.state}")
            }
        }

        val baseServices = base.services.associateBy { it.name }
        val targetServices = target.services.associateBy { it.name }

        val newServices = targetServices.keys.filter { !baseServices.containsKey(it) }
        val removedServices = baseServices.keys.filter { !targetServices.containsKey(it) }

        val summary = buildString {
            append("Diff: Base(${base.id}) -> Target(${target.id}) in ${duration}ms.\n")
            append("Memory Delta: ${if (memDelta >= 0) "+" else ""}${String.format("%.2f", memDelta)} MB.\n")
            append("Threads: +${newThreads.size}, -${terminatedThreads.size}, ${threadStateChanges.size} state changes.\n")
            append("Services: +${newServices.size}, -${removedServices.size}.")
        }

        return SnapshotDiff(
            baseSnapshotId = base.id,
            targetSnapshotId = target.id,
            durationBetweenMs = duration,
            memoryDeltaMb = memDelta,
            newThreads = newThreads,
            terminatedThreads = terminatedThreads,
            threadStateChanges = threadStateChanges,
            newServices = newServices,
            removedServices = removedServices,
            summary = summary
        )
    }
}
