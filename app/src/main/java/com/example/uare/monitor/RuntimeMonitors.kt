package com.example.uare.monitor

data class ThreadInfoModel(
    val id: Long,
    val name: String,
    val state: Thread.State,
    val priority: Int,
    val isDaemon: Boolean,
    val isAlive: Boolean,
    val threadGroupName: String?,
    val stackTrace: List<String>
)

data class MemorySnapshot(
    val totalMemoryBytes: Long,
    val freeMemoryBytes: Long,
    val maxMemoryBytes: Long,
    val usedMemoryBytes: Long,
    val usagePercentage: Float,
    val availableProcessors: Int,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val usedMemoryMb: Float get() = usedMemoryBytes / (1024f * 1024f)
    val maxMemoryMb: Float get() = maxMemoryBytes / (1024f * 1024f)
    val totalMemoryMb: Float get() = totalMemoryBytes / (1024f * 1024f)
}

class ThreadMonitor {

    fun captureThreads(): List<ThreadInfoModel> {
        val traces = Thread.getAllStackTraces()
        return traces.map { (thread, stackElements) ->
            ThreadInfoModel(
                id = thread.id,
                name = thread.name,
                state = thread.state,
                priority = thread.priority,
                isDaemon = thread.isDaemon,
                isAlive = thread.isAlive,
                threadGroupName = thread.threadGroup?.name,
                stackTrace = stackElements.map { it.toString() }
            )
        }.sortedWith(compareByDescending<ThreadInfoModel> { it.state == Thread.State.RUNNABLE }.thenBy { it.name })
    }
}

class MemoryMonitor {

    fun captureMemory(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        val free = runtime.freeMemory()
        val max = runtime.maxMemory()
        val used = total - free
        val percentage = if (max > 0) (used.toFloat() / max.toFloat()) * 100f else 0f

        return MemorySnapshot(
            totalMemoryBytes = total,
            freeMemoryBytes = free,
            maxMemoryBytes = max,
            usedMemoryBytes = used,
            usagePercentage = percentage,
            availableProcessors = runtime.availableProcessors()
        )
    }

    fun requestGarbageCollection() {
        System.gc()
    }
}
