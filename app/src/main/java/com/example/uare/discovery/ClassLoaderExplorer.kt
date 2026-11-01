package com.example.uare.discovery

import android.content.Context
import java.io.File

/**
 * ClassLoader Explorer for inspecting the runtime delegation hierarchy and DEX path lists.
 */
class ClassLoaderExplorer {

    fun discoverClassLoaders(context: Context): List<ClassLoaderInfo> {
        val loaders = mutableListOf<ClassLoaderInfo>()
        val seen = mutableSetOf<Int>()

        fun inspectLoader(cl: ClassLoader?, parentId: String?): String? {
            if (cl == null) return null
            val identity = System.identityHashCode(cl)
            val id = "cl_${cl.javaClass.simpleName}_$identity"

            if (seen.add(identity)) {
                val clClassName = cl.javaClass.name
                val isBoot = clClassName.contains("BootClassLoader")
                val isPath = clClassName.contains("PathClassLoader")
                val isDelegateLast = clClassName.contains("DelegateLastClassLoader")

                val dexElements = extractDexElements(cl)
                val parent = cl.parent
                val currentParentId = if (parent != null) inspectLoader(parent, null) else null

                loaders.add(
                    ClassLoaderInfo(
                        id = id,
                        className = clClassName,
                        parentId = currentParentId,
                        dexElements = dexElements,
                        loadedClassCount = estimateLoadedClasses(cl),
                        isBootClassLoader = isBoot,
                        isPathClassLoader = isPath,
                        isDelegateLast = isDelegateLast
                    )
                )
            }
            return id
        }

        // Start from context class loader
        inspectLoader(context.classLoader, null)
        // Check system class loader
        try {
            inspectLoader(ClassLoader.getSystemClassLoader(), null)
        } catch (ignored: Throwable) {}
        // Check current thread class loader
        try {
            inspectLoader(Thread.currentThread().contextClassLoader, null)
        } catch (ignored: Throwable) {}

        return loaders.distinctBy { it.id }
    }

    private fun extractDexElements(cl: ClassLoader): List<DexElementInfo> {
        val elements = mutableListOf<DexElementInfo>()
        try {
            val pathListField = cl.javaClass.superclass?.getDeclaredField("pathList")
                ?: cl.javaClass.getDeclaredField("pathList")
            pathListField.isAccessible = true
            val pathList = pathListField.get(cl) ?: return emptyList()

            val dexElementsField = pathList.javaClass.getDeclaredField("dexElements")
            dexElementsField.isAccessible = true
            val dexElementsArray = dexElementsField.get(pathList) as? Array<*> ?: return emptyList()

            for (elem in dexElementsArray) {
                if (elem == null) continue
                var pathStr = elem.toString()
                try {
                    val fileField = elem.javaClass.getDeclaredField("path")
                    fileField.isAccessible = true
                    val file = fileField.get(elem) as? File
                    if (file != null) pathStr = file.absolutePath
                } catch (ignored: Throwable) {}

                elements.add(
                    DexElementInfo(
                        path = pathStr,
                        isApk = pathStr.endsWith(".apk", ignoreCase = true),
                        isDex = pathStr.endsWith(".dex", ignoreCase = true)
                    )
                )
            }
        } catch (ignored: Throwable) {
            // ClassLoader might be BootClassLoader or custom without pathList
        }
        return elements
    }

    private fun estimateLoadedClasses(cl: ClassLoader): Int {
        // Safe heuristic or reflection on classes field if available
        try {
            val classesField = ClassLoader::class.java.getDeclaredField("classes")
            classesField.isAccessible = true
            val classes = classesField.get(cl) as? java.util.Vector<*>
            if (classes != null) return classes.size
        } catch (ignored: Throwable) {}
        return 0
    }
}
