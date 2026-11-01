package com.example.uare.core

import android.content.Context
import com.example.uare.api.CapabilityInfo
import com.example.uare.api.ExplorerConfig
import com.example.uare.api.ExplorerError
import com.example.uare.api.ExplorerResult
import com.example.uare.api.RuntimeCapabilities
import com.example.uare.api.RuntimeEnvironment
import com.example.uare.api.onFailure
import com.example.uare.api.onSuccess
import com.example.uare.binder.BinderExplorer
import com.example.uare.binder.SystemServiceInfo
import com.example.uare.core.session.RuntimeSession
import com.example.uare.core.session.SessionManager
import com.example.uare.discovery.ClassDiscoveryEngine
import com.example.uare.discovery.ClassLoaderExplorer
import com.example.uare.discovery.ClassLoaderInfo
import com.example.uare.discovery.DiscoveredClass
import com.example.uare.export.ExportEngine
import com.example.uare.inspection.ClassDetails
import com.example.uare.inspection.ClassInspector
import com.example.uare.inspection.FieldModificationResult
import com.example.uare.inspection.FieldOperations
import com.example.uare.inspection.FieldValueInfo
import com.example.uare.inspection.InvocationResult
import com.example.uare.inspection.ObjectHandle
import com.example.uare.inspection.ObjectInspectionNode
import com.example.uare.inspection.ObjectInspector
import com.example.uare.inspection.ObjectRegistry
import com.example.uare.inspection.ReflectionEngine
import com.example.uare.inspection.TypeConverter
import com.example.uare.monitor.MemoryMonitor
import com.example.uare.monitor.MemorySnapshot
import com.example.uare.monitor.ThreadInfoModel
import com.example.uare.monitor.ThreadMonitor
import com.example.uare.plugin.BinderDiagnosticPlugin
import com.example.uare.plugin.ExplorerContext
import com.example.uare.plugin.MemoryAuditPlugin
import com.example.uare.plugin.PluginManager
import com.example.uare.query.QueryEngine
import com.example.uare.runner.ArgumentInput
import com.example.uare.runner.ExecutionRecord
import com.example.uare.runner.RuntimeRunner
import com.example.uare.sample.SampleUserManager
import com.example.uare.snapshot.DiffEngine
import com.example.uare.snapshot.RuntimeSnapshot
import com.example.uare.snapshot.SnapshotDiff
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicLong

/**
 * Universal Android Runtime Explorer (UARE) Core Public SDK.
 * Main entrypoint for inspecting, discovering, monitoring, and controlling the Android runtime.
 */
object RuntimeExplorer {

    private var appContext: Context? = null
    private var config: ExplorerConfig = ExplorerConfig()
    private val sessionStartTime = System.currentTimeMillis()
    private val snapshotCounter = AtomicLong(1)

    // Subsystems
    val runtimeDetector = RuntimeDetector()
    val capabilityRegistry = CapabilityRegistry()
    val classLoaderExplorer = ClassLoaderExplorer()
    val classDiscoveryEngine = ClassDiscoveryEngine()
    val classInspector = ClassInspector()
    val objectRegistry = ObjectRegistry()
    val objectInspector = ObjectInspector()
    val typeConverter = TypeConverter()
    val reflectionEngine = ReflectionEngine(typeConverter)
    val fieldOperations = FieldOperations(typeConverter)
    val binderExplorer = BinderExplorer()
    val threadMonitor = ThreadMonitor()
    val memoryMonitor = MemoryMonitor()
    val diffEngine = DiffEngine()
    val queryEngine = QueryEngine()
    val pluginManager = PluginManager()
    val exportEngine = ExportEngine()
    val runtimeRunner = RuntimeRunner(typeConverter, objectRegistry)

    private var cachedEnvironment: RuntimeEnvironment? = null
    private var currentSession: RuntimeSession? = null

    fun initialize(context: Context, configuration: ExplorerConfig = ExplorerConfig()) {
        appContext = context.applicationContext
        config = configuration
        val env = runtimeDetector.detectEnvironment()
        cachedEnvironment = env
        currentSession = SessionManager.initializeSession(env)

        // Register default anchor objects into ObjectRegistry
        objectRegistry.registerObject(context.applicationContext, "ApplicationContext", retainStrong = true)

        // Register rich sample domain model for immediate multi-level interactive exploration & control
        val sampleManager = SampleUserManager()
        objectRegistry.registerObject(sampleManager, "SampleUserManager (Domain Controller)", retainStrong = true)
        objectRegistry.registerObject(sampleManager.currentUser, "SampleUserProfile (User Alex Rivers)", retainStrong = true)
        objectRegistry.registerObject(sampleManager.repository, "SampleRepository (API Endpoint)", retainStrong = true)

        // Register built-in diagnostic plugins
        pluginManager.registerPlugin(BinderDiagnosticPlugin())
        pluginManager.registerPlugin(MemoryAuditPlugin())

        val explorerContext = object : ExplorerContext {
            override val environment: RuntimeEnvironment = cachedEnvironment!!
            override val sessionStartTimeMs: Long = sessionStartTime
            override fun logDiagnostic(message: String) {
                android.util.Log.i("UARE", message)
            }
        }
        pluginManager.initializeAll(explorerContext)
    }

    fun getSession(): RuntimeSession? = currentSession

    fun getEnvironment(): RuntimeEnvironment {
        if (cachedEnvironment == null) {
            cachedEnvironment = runtimeDetector.detectEnvironment()
        }
        return cachedEnvironment!!
    }

    fun getCapabilities(): RuntimeCapabilities {
        return capabilityRegistry.evaluateCapabilities(getEnvironment())
    }

    fun getCapabilityRegistry(): List<CapabilityInfo> {
        return capabilityRegistry.getDetailedCapabilities(getEnvironment())
    }

    fun discoverClassLoaders(): List<ClassLoaderInfo> {
        val ctx = appContext ?: return emptyList()
        return classLoaderExplorer.discoverClassLoaders(ctx)
    }

    fun discoverClasses(query: String = "", packageFilter: String = "", limit: Int = 300): List<DiscoveredClass> {
        val ctx = appContext ?: return emptyList()
        return classDiscoveryEngine.discoverClasses(ctx, query, packageFilter, limit)
    }

    fun inspectClass(className: String): ExplorerResult<ClassDetails> {
        return try {
            val cl = appContext?.classLoader ?: ClassLoader.getSystemClassLoader()
            val clazz = Class.forName(className, false, cl)
            ExplorerResult.Success(classInspector.inspectClass(clazz))
        } catch (e: Throwable) {
            ExplorerResult.Failure(
                ExplorerError.of(
                    ExplorerError.ErrorCode.CLASS_NOT_FOUND,
                    "Failed to inspect class '$className': ${e.message}",
                    e
                )
            )
        }
    }

    fun registerObject(instance: Any, description: String = "", retainStrong: Boolean = false): ObjectHandle {
        return objectRegistry.registerObject(instance, description, retainStrong)
    }

    fun inspectObject(handle: ObjectHandle, maxDepth: Int = config.maxTraversalDepth): ExplorerResult<ObjectInspectionNode> {
        val obj = objectRegistry.getObject(handle.id)
            ?: return ExplorerResult.Failure(
                ExplorerError.of(
                    ExplorerError.ErrorCode.RESOURCE_LIMIT_REACHED,
                    "Object with id '${handle.id}' is no longer in registry (collected or unregistered)"
                )
            )
        return ExplorerResult.Success(objectInspector.inspectObject(obj, handle.description.ifEmpty { "target" }, maxDepth))
    }

    fun readField(handle: ObjectHandle, fieldName: String): ExplorerResult<FieldValueInfo> {
        val target = objectRegistry.getObject(handle.id)
            ?: return ExplorerResult.Failure(ExplorerError.of(ExplorerError.ErrorCode.RESOURCE_LIMIT_REACHED, "Object collected"))
        val clazz = target.javaClass
        return fieldOperations.readField(target, clazz, fieldName)
    }

    fun writeField(handle: ObjectHandle, fieldName: String, newValueString: String): ExplorerResult<FieldModificationResult> {
        val target = objectRegistry.getObject(handle.id)
            ?: return ExplorerResult.Failure(ExplorerError.of(ExplorerError.ErrorCode.RESOURCE_LIMIT_REACHED, "Object collected"))
        val clazz = target.javaClass
        val res = fieldOperations.writeField(target, clazz, fieldName, newValueString)
        res.onSuccess {
            currentSession?.log("MODIFY_FIELD", "${handle.id}.$fieldName", "SUCCESS", "$fieldName -> $newValueString")
        }
        return res
    }

    fun restoreField(handle: ObjectHandle, fieldName: String, previousValue: Any?): ExplorerResult<FieldModificationResult> {
        val target = objectRegistry.getObject(handle.id)
            ?: return ExplorerResult.Failure(ExplorerError.of(ExplorerError.ErrorCode.RESOURCE_LIMIT_REACHED, "Object collected"))
        val clazz = target.javaClass
        return fieldOperations.restoreField(target, clazz, fieldName, previousValue)
    }

    fun executeMethod(
        targetHandle: ObjectHandle?,
        targetClassName: String,
        method: Method,
        arguments: List<ArgumentInput>
    ): ExplorerResult<ExecutionRecord> {
        val clazz = try {
            Class.forName(targetClassName)
        } catch (e: Throwable) {
            return ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.CLASS_NOT_FOUND, "Class $targetClassName not found", e)
            )
        }
        val res = runtimeRunner.executeMethod(targetHandle, clazz, method, arguments)
        res.onSuccess {
            currentSession?.log("EXECUTE_METHOD", "${targetClassName}#${method.name}", "SUCCESS", "Returned: ${it.resultString}")
        }
        res.onFailure {
            currentSession?.log("EXECUTE_METHOD", "${targetClassName}#${method.name}", "FAILURE", it.message)
        }
        return res
    }

    fun discoverServices(): List<SystemServiceInfo> {
        return binderExplorer.discoverServices()
    }

    fun getThreadSnapshot(): List<ThreadInfoModel> {
        return threadMonitor.captureThreads()
    }

    fun getMemorySnapshot(): MemorySnapshot {
        return memoryMonitor.captureMemory()
    }

    fun createSnapshot(label: String = "Snapshot #${snapshotCounter.get()}"): RuntimeSnapshot {
        val env = getEnvironment()
        val mem = getMemorySnapshot()
        val th = getThreadSnapshot()
        val loaders = discoverClassLoaders()
        val services = discoverServices()
        val objCount = objectRegistry.getAllHandles().size
        val id = "snap_${snapshotCounter.getAndIncrement()}"

        return RuntimeSnapshot(
            id = id,
            environment = env,
            memory = mem,
            threads = th,
            classLoaders = loaders,
            services = services,
            registeredObjectCount = objCount,
            label = label
        )
    }

    fun diffSnapshots(base: RuntimeSnapshot, target: RuntimeSnapshot): SnapshotDiff {
        return diffEngine.compareSnapshots(base, target)
    }

    fun invokeMethod(handle: ObjectHandle, methodName: String, args: List<String>): ExplorerResult<InvocationResult> {
        val target = objectRegistry.getObject(handle.id)
            ?: return ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Object not found in registry")
            )
        return try {
            val clazz = Class.forName(handle.className)
            reflectionEngine.invokeMethod(target, clazz, methodName, args)
        } catch (e: Throwable) {
            ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Failed to invoke: ${e.message}", e)
            )
        }
    }
}
