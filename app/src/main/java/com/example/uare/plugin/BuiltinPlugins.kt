package com.example.uare.plugin

import com.example.uare.api.RuntimeEnvironment

class BinderDiagnosticPlugin : RuntimeExplorerPlugin {
    override val id: String = "plugin_binder_diag"
    override val name: String = "Binder Health & Diagnostic Plugin"
    override val version: String = "1.0.0"
    override val description: String = "Verifies Binder IPC responsiveness and ping latency for ServiceManager services."

    private var explorerContext: ExplorerContext? = null

    override fun isSupported(environment: RuntimeEnvironment): Boolean = true

    override fun initialize(context: ExplorerContext) {
        this.explorerContext = context
        context.logDiagnostic("BinderDiagnosticPlugin initialized.")
    }

    override fun getActions(): List<ExplorerAction> = listOf(
        object : ExplorerAction {
            override val id: String = "action_ping_core_services"
            override val title: String = "Ping Core System Services"
            override val description: String = "Tests pingBinder() on activity, window, power, and package managers."
            override suspend fun execute(): String {
                val sm = Class.forName("android.os.ServiceManager")
                val getService = sm.getMethod("getService", String::class.java)
                val testList = listOf("activity", "package", "window", "power", "connectivity")
                val results = mutableListOf<String>()
                for (name in testList) {
                    val binder = getService.invoke(null, name) as? android.os.IBinder
                    if (binder != null) {
                        val ping = binder.pingBinder()
                        results.add("$name: ${if (ping) "ALIVE (Ping OK)" else "UNRESPONSIVE"}")
                    } else {
                        results.add("$name: NOT FOUND / RESTRICTED")
                    }
                }
                return results.joinToString("\n")
            }
        }
    )

    override fun release() {
        explorerContext = null
    }
}

class MemoryAuditPlugin : RuntimeExplorerPlugin {
    override val id: String = "plugin_mem_audit"
    override val name: String = "Memory & GC Audit Plugin"
    override val version: String = "1.0.0"
    override val description: String = "Calculates heap reclamation efficiency before and after explicit garbage collection."

    private var explorerContext: ExplorerContext? = null

    override fun isSupported(environment: RuntimeEnvironment): Boolean = true

    override fun initialize(context: ExplorerContext) {
        this.explorerContext = context
    }

    override fun getActions(): List<ExplorerAction> = listOf(
        object : ExplorerAction {
            override val id: String = "action_gc_audit"
            override val title: String = "Audit GC Heap Reclamation"
            override val description: String = "Measures used heap, triggers System.gc(), and reports reclaimed memory."
            override suspend fun execute(): String {
                val rt = Runtime.getRuntime()
                val usedBefore = rt.totalMemory() - rt.freeMemory()
                System.gc()
                kotlinx.coroutines.delay(100)
                val usedAfter = rt.totalMemory() - rt.freeMemory()
                val reclaimedBytes = usedBefore - usedAfter
                val reclaimedMb = reclaimedBytes / (1024f * 1024f)
                return "GC Audit Complete:\nBefore GC: ${usedBefore / (1024 * 1024)} MB\nAfter GC: ${usedAfter / (1024 * 1024)} MB\nReclaimed: ${String.format("%.2f", reclaimedMb)} MB"
            }
        }
    )

    override fun release() {
        explorerContext = null
    }
}
