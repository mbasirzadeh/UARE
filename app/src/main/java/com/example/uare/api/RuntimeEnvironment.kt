package com.example.uare.api

/**
 * Execution environments for the Android Runtime Explorer.
 */
enum class RuntimeType {
    APPLICATION,
    SYSTEM_SERVER,
    SYSTEM_SERVICE,
    CUSTOM_PROCESS,
    UNKNOWN
}

/**
 * Verified runtime environment signals and metadata.
 */
data class RuntimeEnvironment(
    val processName: String?,
    val pid: Int,
    val uid: Int,
    val apiLevel: Int,
    val runtimeType: RuntimeType,
    val confidence: Float,
    val detectionSignals: List<String>,
    val buildFingerprint: String = android.os.Build.FINGERPRINT,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val manufacturer: String = android.os.Build.MANUFACTURER,
    val model: String = android.os.Build.MODEL,
    val supportedAbis: List<String> = android.os.Build.SUPPORTED_ABIS.toList(),
    val is64Bit: Boolean = android.os.Process.is64Bit(),
    val vmVersion: String = System.getProperty("java.vm.version") ?: "ART"
) {
    val isSystemServer: Boolean
        get() = runtimeType == RuntimeType.SYSTEM_SERVER

    val isApplication: Boolean
        get() = runtimeType == RuntimeType.APPLICATION

    val isPrivilegedUid: Boolean
        get() = uid in listOf(0, 1000)
}
