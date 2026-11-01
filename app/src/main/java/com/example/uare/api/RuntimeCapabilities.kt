package com.example.uare.api

/**
 * Core runtime capabilities supported by the current adapter.
 */
data class RuntimeCapabilities(
    val classDiscovery: Boolean,
    val dexInspection: Boolean,
    val classLoaderInspection: Boolean,
    val reflection: Boolean,
    val objectInspection: Boolean,
    val binderInspection: Boolean,
    val systemServiceDiscovery: Boolean,
    val threadMonitoring: Boolean,
    val memoryMonitoring: Boolean,
    val snapshotSupport: Boolean,
    val crossProcessInspection: Boolean
) {
    companion object {
        fun applicationDefault(): RuntimeCapabilities = RuntimeCapabilities(
            classDiscovery = true,
            dexInspection = true,
            classLoaderInspection = true,
            reflection = true,
            objectInspection = true,
            binderInspection = true,
            systemServiceDiscovery = true,
            threadMonitoring = true,
            memoryMonitoring = true,
            snapshotSupport = true,
            crossProcessInspection = false
        )
    }
}

/**
 * Verification status for each individual platform capability.
 */
enum class VerificationStatus {
    IMPLEMENTED,
    PARTIALLY_IMPLEMENTED,
    PLATFORM_DEPENDENT,
    UNSUPPORTED,
    NOT_TESTED
}

/**
 * Detailed capability specification with compatibility matrix and limitations.
 */
data class CapabilityInfo(
    val id: String,
    val name: String,
    val supported: Boolean,
    val apiLevel: Int,
    val environment: RuntimeType,
    val source: String?,
    val limitations: List<String>,
    val verificationStatus: VerificationStatus
)

/**
 * Explorer configuration settings.
 */
data class ExplorerConfig(
    val enableClassDiscovery: Boolean = true,
    val enableDexInspection: Boolean = true,
    val enableObjectInspection: Boolean = true,
    val enableBinderInspection: Boolean = true,
    val enableMonitoring: Boolean = true,
    val maxTraversalDepth: Int = 3,
    val maxClassDiscoveryLimit: Int = 1000,
    val allowReflectionMutations: Boolean = false,
    val safeMode: Boolean = true
)
