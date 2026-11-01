package com.example.uare.discovery

import android.content.Context
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexFile
import java.io.File
import java.lang.reflect.Modifier
import java.util.Enumeration

/**
 * Class Discovery Engine for scanning accessible runtime classes, DEX definitions, and framework classes.
 */
class ClassDiscoveryEngine {

    fun discoverClasses(
        context: Context,
        query: String = "",
        packageFilter: String = "",
        limit: Int = 300
    ): List<DiscoveredClass> {
        val results = mutableListOf<DiscoveredClass>()
        val seen = mutableSetOf<String>()

        // 1. Discover from BaseDexClassLoader pathList dexElements
        try {
            val classLoader = context.classLoader
            if (classLoader is BaseDexClassLoader) {
                val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList")
                pathListField.isAccessible = true
                val pathList = pathListField.get(classLoader)

                if (pathList != null) {
                    val dexElementsField = pathList.javaClass.getDeclaredField("dexElements")
                    dexElementsField.isAccessible = true
                    val dexElements = dexElementsField.get(pathList) as? Array<*>

                    if (dexElements != null) {
                        for (element in dexElements) {
                            if (element == null) continue
                            try {
                                val dexFileField = element.javaClass.getDeclaredField("dexFile")
                                dexFileField.isAccessible = true
                                val dexFile = dexFileField.get(element) as? DexFile
                                if (dexFile != null) {
                                    val entries: Enumeration<String> = dexFile.entries()
                                    while (entries.hasMoreElements() && results.size < limit) {
                                        val className = entries.nextElement()
                                        if (matchesFilter(className, query, packageFilter) && seen.add(className)) {
                                            results.add(createClassSummary(className, classLoader, ClassDiscoverySource.DEX_ENTRY))
                                        }
                                    }
                                }
                            } catch (ignored: Throwable) {}
                        }
                    }
                }
            }
        } catch (ignored: Throwable) {}

        // 2. Discover key known Android framework & application classes if needed
        if (results.size < limit) {
            val frameworkKnown = getKnownFrameworkClasses()
            for (className in frameworkKnown) {
                if (matchesFilter(className, query, packageFilter) && seen.add(className)) {
                    results.add(createClassSummary(className, context.classLoader, ClassDiscoverySource.PLATFORM_INDEX))
                    if (results.size >= limit) break
                }
            }
        }

        return results
    }

    private fun matchesFilter(className: String, query: String, packageFilter: String): Boolean {
        if (packageFilter.isNotEmpty() && !className.startsWith(packageFilter)) return false
        if (query.isNotEmpty() && !className.contains(query, ignoreCase = true)) return false
        return true
    }

    private fun createClassSummary(
        className: String,
        classLoader: ClassLoader,
        source: ClassDiscoverySource
    ): DiscoveredClass {
        val pkg = className.substringBeforeLast('.', "")
        val simpleName = className.substringAfterLast('.')

        var isLoaded = false
        var isInterface = false
        var isEnum = false
        var isAbstract = false
        var isFinal = false
        var superclassName: String? = null

        try {
            // Attempt reflection to inspect if already loaded or resolvable
            val clazz = Class.forName(className, false, classLoader)
            isLoaded = true
            val mods = clazz.modifiers
            isInterface = Modifier.isInterface(mods)
            isEnum = clazz.isEnum
            isAbstract = Modifier.isAbstract(mods)
            isFinal = Modifier.isFinal(mods)
            superclassName = clazz.superclass?.name
        } catch (ignored: Throwable) {}

        return DiscoveredClass(
            name = className,
            simpleName = simpleName,
            packageName = pkg,
            source = if (isLoaded) ClassDiscoverySource.LOADED_CLASS else source,
            isLoaded = isLoaded,
            isInterface = isInterface,
            isEnum = isEnum,
            isAbstract = isAbstract,
            isFinal = isFinal,
            superclassName = superclassName,
            classLoaderName = classLoader.javaClass.simpleName
        )
    }

    private fun getKnownFrameworkClasses(): List<String> = listOf(
        "android.app.Activity",
        "android.app.Service",
        "android.app.Application",
        "android.app.ActivityThread",
        "android.app.ActivityManager",
        "android.os.Process",
        "android.os.Build",
        "android.os.IBinder",
        "android.os.Binder",
        "android.os.ServiceManager",
        "android.os.Handler",
        "android.os.Looper",
        "android.content.Context",
        "android.content.Intent",
        "android.view.View",
        "android.view.WindowManager",
        "com.android.server.LocalServices",
        "com.android.server.SystemServer",
        "java.lang.Object",
        "java.lang.String",
        "java.lang.Thread",
        "java.lang.ClassLoader",
        "java.lang.reflect.Method",
        "java.lang.reflect.Field"
    )
}
