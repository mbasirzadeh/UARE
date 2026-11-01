package com.example.uare.plugin

import com.example.uare.api.RuntimeEnvironment

interface ExplorerAction {
    val id: String
    val title: String
    val description: String
    suspend fun execute(): String
}

interface ExplorerContext {
    val environment: RuntimeEnvironment
    val sessionStartTimeMs: Long
    fun logDiagnostic(message: String)
}

interface RuntimeExplorerPlugin {
    val id: String
    val name: String
    val version: String
    val description: String

    fun isSupported(environment: RuntimeEnvironment): Boolean
    fun initialize(context: ExplorerContext)
    fun getActions(): List<ExplorerAction>
    fun release()
}

class PluginManager {

    private val plugins = mutableListOf<RuntimeExplorerPlugin>()
    private var isInitialized = false

    fun registerPlugin(plugin: RuntimeExplorerPlugin) {
        if (!plugins.any { it.id == plugin.id }) {
            plugins.add(plugin)
        }
    }

    fun unregisterPlugin(pluginId: String) {
        val plugin = plugins.find { it.id == pluginId }
        plugin?.release()
        plugins.removeAll { it.id == pluginId }
    }

    fun initializeAll(context: ExplorerContext) {
        isInitialized = true
        for (plugin in plugins) {
            if (plugin.isSupported(context.environment)) {
                try {
                    plugin.initialize(context)
                } catch (t: Throwable) {
                    context.logDiagnostic("Plugin ${plugin.id} initialization failed: ${t.message}")
                }
            }
        }
    }

    fun getAllPlugins(): List<RuntimeExplorerPlugin> = plugins.toList()

    fun getAllActions(): List<ExplorerAction> = plugins.flatMap { it.getActions() }
}
