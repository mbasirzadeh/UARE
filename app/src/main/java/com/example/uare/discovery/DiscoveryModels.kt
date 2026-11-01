package com.example.uare.discovery

/**
 * Origin of a discovered class.
 */
enum class ClassDiscoverySource {
    LOADED_CLASS,
    DEX_ENTRY,
    REFLECTION,
    PLATFORM_INDEX
}

/**
 * Summary representation of a discovered class in the runtime.
 */
data class DiscoveredClass(
    val name: String,
    val simpleName: String,
    val packageName: String,
    val source: ClassDiscoverySource,
    val isLoaded: Boolean,
    val isInterface: Boolean = false,
    val isEnum: Boolean = false,
    val isAbstract: Boolean = false,
    val isFinal: Boolean = false,
    val superclassName: String? = null,
    val classLoaderName: String? = null
)

/**
 * Metadata for a DEX file or element in a PathList.
 */
data class DexElementInfo(
    val path: String,
    val isApk: Boolean,
    val isDex: Boolean,
    val estimatedClasses: Int? = null
)

/**
 * Hierarchical representation of an active ClassLoader in ART.
 */
data class ClassLoaderInfo(
    val id: String,
    val className: String,
    val parentId: String?,
    val dexElements: List<DexElementInfo>,
    val loadedClassCount: Int,
    val isBootClassLoader: Boolean = false,
    val isPathClassLoader: Boolean = false,
    val isDelegateLast: Boolean = false
)
