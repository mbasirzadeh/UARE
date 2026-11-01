package com.example.uare.core

import android.os.Build
import com.example.uare.api.CapabilityInfo
import com.example.uare.api.RuntimeCapabilities
import com.example.uare.api.RuntimeEnvironment
import com.example.uare.api.RuntimeType
import com.example.uare.api.VerificationStatus

/**
 * Platform Compatibility and Capability Registry.
 * Explicitly tracks supported, platform-dependent, and restricted capabilities across Android releases.
 */
class CapabilityRegistry {

    fun evaluateCapabilities(env: RuntimeEnvironment): RuntimeCapabilities {
        return RuntimeCapabilities(
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
            crossProcessInspection = env.runtimeType == RuntimeType.SYSTEM_SERVER
        )
    }

    fun getDetailedCapabilities(env: RuntimeEnvironment): List<CapabilityInfo> {
        val api = Build.VERSION.SDK_INT
        return listOf(
            CapabilityInfo(
                id = "cap_runtime_detect",
                name = "Multi-Signal Runtime Detection",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "Application.getProcessName / /proc / ActivityThread",
                limitations = listOf("Process name may be truncated in strict SELinux isolated sandboxes"),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_classloader_explore",
                name = "ClassLoader Hierarchy & DexPathList Inspection",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "BaseDexClassLoader.pathList.dexElements reflection",
                limitations = listOf("InMemoryDexClassLoader elements may have null dex file paths"),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_class_discovery",
                name = "DEX & Loaded Class Enumeration",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "Dalvik DexFile class enumeration + ClassLoader tree",
                limitations = listOf(
                    "ART hidden API policy may restrict deep dalvik.system.DexFile access on release builds",
                    "DEX entry discovery does not guarantee the class is initialized or loaded"
                ),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_class_inspect",
                name = "Deep Class Inspector & Signature Resolution",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "Java Reflection + Modifiers + Annotation Mirroring",
                limitations = listOf("Synthetic and bridge methods require specific flag decoding"),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_reflection_engine",
                name = "Safe Reflection & Type-Converted Invocation",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "Method/Constructor/Field invocation with safety confirmation",
                limitations = listOf(
                    "Non-public member access is restricted by Android Hidden API policy on SDK >= 28",
                    "Arbitrary invocation is protected by user confirmation prompt"
                ),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_object_registry",
                name = "Object Registry & Cyclic Field Graph",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "Session Object Store + Traversal Depth Limiter",
                limitations = listOf(
                    "Cannot enumerate entire ART heap without JVMTI / Root",
                    "Bounded traversal to depth limit (default 3) to prevent OOM"
                ),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_binder_services",
                name = "ServiceManager & Binder Proxy Inspection",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "android.os.ServiceManager.listServices() reflection",
                limitations = listOf(
                    "Client process gets BinderProxy, not the remote service implementation object",
                    "SELinux policies restrict listing certain security services from untrusted apps"
                ),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_local_services",
                name = "LocalServices Discovery (System Server only)",
                supported = env.runtimeType == RuntimeType.SYSTEM_SERVER,
                apiLevel = 31,
                environment = RuntimeType.SYSTEM_SERVER,
                source = "com.android.server.LocalServices",
                limitations = listOf("Only exists inside system_server process; inaccessible in app runtime"),
                verificationStatus = if (env.runtimeType == RuntimeType.SYSTEM_SERVER) {
                    VerificationStatus.IMPLEMENTED
                } else {
                    VerificationStatus.UNSUPPORTED
                }
            ),
            CapabilityInfo(
                id = "cap_thread_monitor",
                name = "Thread Enumeration & Stack Trace Capture",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "Thread.getAllStackTraces()",
                limitations = listOf("Only captures threads within the current process memory space"),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_memory_monitor",
                name = "Runtime Memory & Heap Diagnostics",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "java.lang.Runtime + Debug.MemoryInfo",
                limitations = listOf("Process-scoped metrics only; system-wide memory requires root/dumpsys"),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_snapshot_diff",
                name = "Bounded Snapshot & Runtime Diff Engine",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "Structured memory/thread/class state comparison",
                limitations = listOf("Bounded to registered and discovered state; not a raw 4GB HPROF file"),
                verificationStatus = VerificationStatus.IMPLEMENTED
            ),
            CapabilityInfo(
                id = "cap_plugin_sdk",
                name = "Runtime Explorer Plugin Extensibility",
                supported = true,
                apiLevel = 31,
                environment = env.runtimeType,
                source = "RuntimeExplorerPlugin lifecycle API",
                limitations = listOf("Plugins run in-process with the same security privileges"),
                verificationStatus = VerificationStatus.IMPLEMENTED
            )
        )
    }
}
