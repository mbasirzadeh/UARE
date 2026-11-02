package com.example.uare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uare.api.CapabilityInfo
import com.example.uare.api.ExplorationTarget
import com.example.uare.api.ExplorerResult
import com.example.uare.api.ExplorerSelectionMode
import com.example.uare.api.RuntimeEnvironment
import com.example.uare.api.RuntimeOperation
import com.example.uare.binder.BinderMethodInfo
import com.example.uare.binder.SystemServiceInfo
import com.example.uare.core.RuntimeExplorer
import com.example.uare.core.session.RuntimeSession
import com.example.uare.discovery.ClassLoaderInfo
import com.example.uare.discovery.DiscoveredClass
import com.example.uare.inspection.ClassDetails
import com.example.uare.inspection.FieldModificationResult
import com.example.uare.inspection.FieldValueInfo
import com.example.uare.inspection.ObjectHandle
import com.example.uare.inspection.ObjectInspectionNode
import com.example.uare.monitor.MemorySnapshot
import com.example.uare.monitor.ThreadInfoModel
import com.example.uare.plugin.ExplorerAction
import com.example.uare.plugin.RuntimeExplorerPlugin
import com.example.uare.query.QueryPredicate
import com.example.uare.query.QueryResult
import com.example.uare.runner.ArgumentInput
import com.example.uare.runner.ExecutionRecord
import com.example.uare.runner.ParameterSpec
import com.example.uare.snapshot.RuntimeSnapshot
import com.example.uare.snapshot.SnapshotDiff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.reflect.Constructor
import java.lang.reflect.Method

enum class ActiveScreen {
    EXPLORER,
    RUNNER
}

enum class ExplorerTab(val title: String) {
    UNIFIED_EXPLORER("Unified Explorer"),
    RUNNER("Runtime Runner"),
    DASHBOARD("Dashboard"),
    CAPABILITIES("Capabilities"),
    CLASSES("Classes"),
    OBJECTS("Objects"),
    BINDER("Binder Services"),
    MONITOR("Telemetry"),
    SNAPSHOTS("Snapshots"),
    QUERY("Query"),
    PLUGINS("Plugins")
}

data class ActiveRunnerState(
    val operation: RuntimeOperation,
    val method: Method? = null,
    val constructor: Constructor<*>? = null,
    val parameterSpecs: List<ParameterSpec> = emptyList(),
    val arguments: MutableMap<Int, ArgumentInput> = mutableMapOf(),
    val availableMethods: List<String> = emptyList(),
    val isConfirmedSafe: Boolean = false,
    val isExecuting: Boolean = false,
    val lastExecutionRecord: ExecutionRecord? = null,
    val fieldCurrentValue: String = "",
    val fieldEditValue: String = "",
    val fieldLastModification: FieldModificationResult? = null
)

data class ExplorerUiState(
    val activeScreen: ActiveScreen = ActiveScreen.EXPLORER,
    val currentTab: ExplorerTab = ExplorerTab.UNIFIED_EXPLORER,
    val selectionMode: ExplorerSelectionMode = ExplorerSelectionMode(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val explorationStack: List<ExplorationTarget> = emptyList(),
    val currentExplorationTarget: ExplorationTarget? = null,
    val session: RuntimeSession? = RuntimeExplorer.getSession(),
    val environment: RuntimeEnvironment = RuntimeExplorer.getEnvironment(),
    val memory: MemorySnapshot = RuntimeExplorer.getMemorySnapshot(),
    val capabilities: List<CapabilityInfo> = emptyList(),
    val classLoaders: List<ClassLoaderInfo> = emptyList(),
    val discoveredClasses: List<DiscoveredClass> = emptyList(),
    val classSearchQuery: String = "",
    val packageFilter: String = "",
    val selectedClassDetails: ClassDetails? = null,
    val registeredObjects: List<ObjectHandle> = emptyList(),
    val selectedObjectNode: ObjectInspectionNode? = null,
    val selectedObjectHandle: ObjectHandle? = null,
    val activeRunner: ActiveRunnerState? = null,
    val executionHistory: List<ExecutionRecord> = emptyList(),
    val services: List<SystemServiceInfo> = emptyList(),
    val threads: List<ThreadInfoModel> = emptyList(),
    val selectedThread: ThreadInfoModel? = null,
    val snapshots: List<RuntimeSnapshot> = emptyList(),
    val selectedDiff: SnapshotDiff? = null,
    val plugins: List<RuntimeExplorerPlugin> = emptyList(),
    val pluginActions: List<ExplorerAction> = emptyList(),
    val lastActionOutput: String? = null,
    val lastQueryResult: QueryResult? = null,
    val statusMessage: String? = null
)

class ExplorerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun navigateTo(screen: ActiveScreen) {
        _uiState.update { it.copy(activeScreen = screen) }
    }

    fun selectTab(tab: ExplorerTab) {
        _uiState.update { it.copy(currentTab = tab) }
        when (tab) {
            ExplorerTab.RUNNER -> navigateTo(ActiveScreen.RUNNER)
            else -> navigateTo(ActiveScreen.EXPLORER)
        }
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val env = RuntimeExplorer.getEnvironment()
            val mem = RuntimeExplorer.getMemorySnapshot()
            val caps = RuntimeExplorer.getCapabilityRegistry()
            val loaders = RuntimeExplorer.discoverClassLoaders()
            val objs = RuntimeExplorer.objectRegistry.getAllHandles()
            val th = RuntimeExplorer.getThreadSnapshot()
            val svcs = RuntimeExplorer.discoverServices()
            val plugs = RuntimeExplorer.pluginManager.getAllPlugins()
            val acts = RuntimeExplorer.pluginManager.getAllActions()
            val hist = RuntimeExplorer.runtimeRunner.getHistory()
            val sess = RuntimeExplorer.getSession()

            val classes = RuntimeExplorer.discoverClasses(limit = 150)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    session = sess,
                    environment = env,
                    memory = mem,
                    capabilities = caps,
                    classLoaders = loaders,
                    discoveredClasses = classes,
                    registeredObjects = objs,
                    threads = th,
                    services = svcs,
                    plugins = plugs,
                    pluginActions = acts,
                    executionHistory = hist
                )
            }
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch(Dispatchers.IO) {
            val mem = RuntimeExplorer.getMemorySnapshot()
            val th = RuntimeExplorer.getThreadSnapshot()
            _uiState.update { it.copy(memory = mem, threads = th) }
        }
    }

    fun refreshServices() {
        viewModelScope.launch(Dispatchers.IO) {
            val svcs = RuntimeExplorer.discoverServices()
            _uiState.update { it.copy(services = svcs) }
        }
    }

    fun refreshClassLoaders() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaders = RuntimeExplorer.discoverClassLoaders()
            _uiState.update { it.copy(classLoaders = loaders) }
        }
    }

    fun searchClasses(query: String = _uiState.value.classSearchQuery, pkg: String = _uiState.value.packageFilter) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearching = true, classSearchQuery = query, packageFilter = pkg) }
            val classes = RuntimeExplorer.discoverClasses(query, pkg, limit = 200)
            _uiState.update { it.copy(isSearching = false, discoveredClasses = classes) }
        }
    }

    fun searchGlobal(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearching = true, classSearchQuery = query) }
            val classes = RuntimeExplorer.discoverClasses(query, limit = 200)
            _uiState.update { it.copy(isSearching = false, discoveredClasses = classes) }
        }
    }

    fun inspectClass(className: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = RuntimeExplorer.inspectClass(className)) {
                is ExplorerResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, selectedClassDetails = res.value) }
                }
                is ExplorerResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Class not found: ${res.error.message}") }
                }
            }
        }
    }

    fun clearSelectedClass() {
        _uiState.update { it.copy(selectedClassDetails = null) }
    }

    fun inspectObject(handle: ObjectHandle) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, selectedObjectHandle = handle) }
            when (val res = RuntimeExplorer.inspectObject(handle)) {
                is ExplorerResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, selectedObjectNode = res.value) }
                }
                is ExplorerResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, statusMessage = res.error.message) }
                }
            }
        }
    }

    // ---------------- UNLIMITED NESTED EXPLORATION ----------------

    fun exploreClass(className: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = RuntimeExplorer.inspectClass(className)) {
                is ExplorerResult.Success -> {
                    val target = ExplorationTarget.ClassTarget(className)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            activeScreen = ActiveScreen.EXPLORER,
                            selectedClassDetails = res.value,
                            selectedObjectHandle = null,
                            selectedObjectNode = null,
                            explorationStack = state.explorationStack + target,
                            currentExplorationTarget = target
                        )
                    }
                }
                is ExplorerResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Cannot inspect class: ${res.error.message}") }
                }
            }
        }
    }

    fun exploreObject(handle: ObjectHandle) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = RuntimeExplorer.inspectObject(handle)) {
                is ExplorerResult.Success -> {
                    val target = ExplorationTarget.ObjectTarget(handle)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            activeScreen = ActiveScreen.EXPLORER,
                            selectedObjectHandle = handle,
                            selectedObjectNode = res.value,
                            selectedClassDetails = null,
                            explorationStack = state.explorationStack + target,
                            currentExplorationTarget = target
                        )
                    }
                }
                is ExplorerResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Cannot inspect object: ${res.error.message}") }
                }
            }
        }
    }

    fun drillDownField(parentHandle: ObjectHandle, fieldName: String, fieldNode: ObjectInspectionNode) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _uiState.value.registeredObjects.find { it.id == fieldNode.valueString || it.id == fieldNode.name }
            if (existing != null) {
                exploreObject(existing)
            } else {
                val parentObj = RuntimeExplorer.objectRegistry.getObject(parentHandle.id)
                if (parentObj != null) {
                    try {
                        val field = parentObj.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
                        val value = field.get(parentObj)
                        if (value != null && !value.javaClass.isPrimitive && value.javaClass != String::class.java) {
                            val newHandle = RuntimeExplorer.objectRegistry.registerObject(
                                value,
                                "${parentHandle.id}.$fieldName (${value.javaClass.simpleName})",
                                retainStrong = true
                            )
                            refreshObjects()
                            exploreObject(newHandle)
                            return@launch
                        }
                    } catch (_: Throwable) {}
                }
                exploreClass(fieldNode.type)
            }
        }
    }

    fun popExploration() {
        _uiState.update { state ->
            if (state.explorationStack.size <= 1) {
                state.copy(
                    explorationStack = emptyList(),
                    currentExplorationTarget = null,
                    selectedClassDetails = null,
                    selectedObjectHandle = null,
                    selectedObjectNode = null
                )
            } else {
                val newStack = state.explorationStack.dropLast(1)
                val newCurrent = newStack.last()
                state.copy(
                    explorationStack = newStack,
                    currentExplorationTarget = newCurrent
                )
            }
        }
        val current = _uiState.value.currentExplorationTarget
        when (current) {
            is ExplorationTarget.ClassTarget -> inspectClass(current.className)
            is ExplorationTarget.ObjectTarget -> inspectObject(current.handle)
            null -> {}
        }
    }

    fun jumpToExplorationIndex(index: Int) {
        _uiState.update { state ->
            if (index < 0 || index >= state.explorationStack.size) {
                state.copy(
                    explorationStack = emptyList(),
                    currentExplorationTarget = null,
                    selectedClassDetails = null,
                    selectedObjectHandle = null,
                    selectedObjectNode = null
                )
            } else {
                val newStack = state.explorationStack.take(index + 1)
                val newCurrent = newStack.last()
                state.copy(
                    explorationStack = newStack,
                    currentExplorationTarget = newCurrent
                )
            }
        }
        val current = _uiState.value.currentExplorationTarget
        when (current) {
            is ExplorationTarget.ClassTarget -> inspectClass(current.className)
            is ExplorationTarget.ObjectTarget -> inspectObject(current.handle)
            null -> {}
        }
    }

    fun resetExplorationToRoot() {
        _uiState.update {
            it.copy(
                explorationStack = emptyList(),
                currentExplorationTarget = null,
                selectedClassDetails = null,
                selectedObjectHandle = null,
                selectedObjectNode = null
            )
        }
    }

    fun openObjectInExplorer(handle: ObjectHandle) {
        exploreObject(handle)
    }

    fun openClassInExplorer(className: String) {
        exploreClass(className)
    }

    // ---------------- CONTEXTUAL "USE" ACTIONS ----------------

    fun useMethod(targetHandle: ObjectHandle?, targetClassName: String, methodName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clazz = Class.forName(targetClassName)
                val method = clazz.methods.firstOrNull { it.name == methodName }
                    ?: clazz.declaredMethods.firstOrNull { it.name == methodName }

                val allMethods = (clazz.methods.map { it.name } + clazz.declaredMethods.map { it.name })
                    .filter { !it.contains("$") }
                    .distinct()
                    .sorted()

                if (method != null) {
                    val specs = RuntimeExplorer.runtimeRunner.extractParameterSpecs(method)
                    val initialArgs = mutableMapOf<Int, ArgumentInput>()
                    specs.forEach { spec ->
                        val defaultVal = when {
                            spec.isEnum && spec.enumConstants.isNotEmpty() -> spec.enumConstants.first()
                            spec.isPrimitive && spec.simpleTypeName.contains("bool", ignoreCase = true) -> "true"
                            spec.isPrimitive -> "0"
                            spec.isString -> ""
                            else -> ""
                        }
                        initialArgs[spec.index] = ArgumentInput(
                            parameterIndex = spec.index,
                            parameterName = spec.name,
                            rawValue = defaultVal,
                            selectedObjectHandleId = if (spec.isObjectReference) _uiState.value.registeredObjects.firstOrNull()?.id else null
                        )
                    }

                    val operation = RuntimeOperation.InvokeMethod(
                        target = targetHandle,
                        targetClassName = targetClassName,
                        methodName = methodName,
                        parameterTypes = method.parameterTypes.map { it.simpleName },
                        returnType = method.returnType.simpleName
                    )

                    _uiState.update {
                        it.copy(
                            activeScreen = ActiveScreen.RUNNER,
                            selectionMode = ExplorerSelectionMode(active = false),
                            activeRunner = ActiveRunnerState(
                                operation = operation,
                                method = method,
                                parameterSpecs = specs,
                                arguments = initialArgs,
                                availableMethods = allMethods,
                                isConfirmedSafe = false
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(statusMessage = "Method $methodName not found on $targetClassName") }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(statusMessage = "Cannot resolve $targetClassName: ${e.message}") }
            }
        }
    }

    fun switchRunnerMethod(newMethodName: String) {
        val runner = _uiState.value.activeRunner ?: return
        when (val op = runner.operation) {
            is RuntimeOperation.InvokeMethod -> {
                useMethod(op.target, op.targetClassName, newMethodName)
            }
            is RuntimeOperation.InvokeBinderOperation -> {
                val svc = _uiState.value.services.firstOrNull { it.name == op.serviceName }
                val methodInfo = svc?.methods?.firstOrNull { it.name == newMethodName }
                if (methodInfo != null) {
                    useBinderMethod(op.serviceName, op.descriptor, methodInfo, op.proxyHandleId)
                }
            }
            else -> {}
        }
    }

    fun useConstructor(targetClassName: String, constructorSignature: String, paramTypes: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clazz = Class.forName(targetClassName)
                val constructor = clazz.declaredConstructors.firstOrNull { it.parameterTypes.size == paramTypes.size }
                    ?: clazz.constructors.firstOrNull()

                if (constructor != null) {
                    val specs = RuntimeExplorer.runtimeRunner.extractConstructorParameterSpecs(constructor)
                    val initialArgs = mutableMapOf<Int, ArgumentInput>()
                    specs.forEach { spec ->
                        val defaultVal = if (spec.isPrimitive) "0" else ""
                        initialArgs[spec.index] = ArgumentInput(
                            parameterIndex = spec.index,
                            parameterName = spec.name,
                            rawValue = defaultVal
                        )
                    }

                    val operation = RuntimeOperation.CreateInstance(
                        targetClassName = targetClassName,
                        constructorSignature = constructorSignature,
                        parameterTypes = paramTypes
                    )

                    _uiState.update {
                        it.copy(
                            activeScreen = ActiveScreen.RUNNER,
                            selectionMode = ExplorerSelectionMode(active = false),
                            activeRunner = ActiveRunnerState(
                                operation = operation,
                                constructor = constructor,
                                parameterSpecs = specs,
                                arguments = initialArgs,
                                isConfirmedSafe = true
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(statusMessage = "Cannot inspect constructor: ${e.message}") }
            }
        }
    }

    fun useField(targetHandle: ObjectHandle, targetClassName: String, fieldName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = RuntimeExplorer.readField(targetHandle, fieldName)) {
                is ExplorerResult.Success -> {
                    val info = res.value
                    val operation = RuntimeOperation.WriteField(
                        target = targetHandle,
                        targetClassName = targetClassName,
                        fieldName = fieldName,
                        fieldType = info.typeName,
                        currentValue = info.valueString
                    )

                    _uiState.update {
                        it.copy(
                            activeScreen = ActiveScreen.RUNNER,
                            selectionMode = ExplorerSelectionMode(active = false),
                            activeRunner = ActiveRunnerState(
                                operation = operation,
                                fieldCurrentValue = info.valueString,
                                fieldEditValue = info.valueString,
                                isConfirmedSafe = true
                            )
                        )
                    }
                }
                is ExplorerResult.Failure -> {
                    _uiState.update { it.copy(statusMessage = "Cannot read field: ${res.error.message}") }
                }
            }
        }
    }

    fun useBinderOperation(serviceName: String, descriptor: String?, operationName: String = "pingBinder") {
        val svc = _uiState.value.services.firstOrNull { it.name == serviceName }
        val methodInfo = svc?.methods?.firstOrNull { it.name == operationName }
            ?: BinderMethodInfo(operationName, "Boolean", emptyList(), emptyList(), "$operationName()")

        useBinderMethod(serviceName, descriptor, methodInfo, svc?.proxyObjectHandleId)
    }

    fun useBinderMethod(
        serviceName: String,
        descriptor: String?,
        methodInfo: BinderMethodInfo,
        proxyHandleId: String? = null
    ) {
        val operation = RuntimeOperation.InvokeBinderOperation(
            serviceName = serviceName,
            descriptor = descriptor,
            operationName = methodInfo.name,
            parameterTypes = methodInfo.parameterTypes,
            parameterNames = methodInfo.parameterNames,
            returnType = methodInfo.returnType,
            proxyHandleId = proxyHandleId
        )

        val specs = RuntimeExplorer.runtimeRunner.extractBinderParameterSpecs(methodInfo.parameterTypes, methodInfo.parameterNames)
        val initialArgs = mutableMapOf<Int, ArgumentInput>()
        specs.forEach { spec ->
            val defaultVal = when {
                spec.isPrimitive && spec.simpleTypeName.contains("bool", ignoreCase = true) -> "true"
                spec.isPrimitive -> "0"
                spec.isString -> ""
                else -> ""
            }
            initialArgs[spec.index] = ArgumentInput(
                parameterIndex = spec.index,
                parameterName = spec.name,
                rawValue = defaultVal,
                selectedObjectHandleId = if (spec.isObjectReference) _uiState.value.registeredObjects.firstOrNull()?.id else null
            )
        }

        val svc = _uiState.value.services.firstOrNull { it.name == serviceName }
        val otherMethods = svc?.methods?.map { it.name } ?: listOf(methodInfo.name)

        _uiState.update {
            it.copy(
                activeScreen = ActiveScreen.RUNNER,
                selectionMode = ExplorerSelectionMode(active = false),
                activeRunner = ActiveRunnerState(
                    operation = operation,
                    parameterSpecs = specs,
                    arguments = initialArgs,
                    availableMethods = otherMethods,
                    isConfirmedSafe = true
                )
            )
        }
    }

    fun setReturnedObjectAsRunnerTarget(handle: ObjectHandle) {
        val clazz = try { Class.forName(handle.className) } catch (_: Throwable) { null }
        val firstMethod = clazz?.methods?.firstOrNull { it.declaringClass != Any::class.java }?.name ?: "toString"
        useMethod(handle, handle.className, firstMethod)
    }

    fun reloadArgumentsFromHistory(record: ExecutionRecord) {
        val handle = record.targetHandleId?.let { id -> _uiState.value.registeredObjects.find { it.id == id } }
        val rawMethod = record.methodName.substringAfterLast('#').substringBefore('(')
        useMethod(handle, record.targetClassName, rawMethod)
    }

    // ---------------- BROWSE WORKFLOW ----------------

    fun startObjectBrowseForParameter(paramIndex: Int, expectedType: String, paramName: String) {
        _uiState.update {
            it.copy(
                activeScreen = ActiveScreen.EXPLORER,
                selectionMode = ExplorerSelectionMode(
                    active = true,
                    targetParameterIndex = paramIndex,
                    expectedTypeName = expectedType,
                    parameterName = paramName,
                    prompt = "Select an instance of $expectedType for parameter '$paramName'"
                )
            )
        }
    }

    fun cancelSelectionMode() {
        _uiState.update {
            it.copy(
                activeScreen = ActiveScreen.RUNNER,
                selectionMode = ExplorerSelectionMode(active = false)
            )
        }
    }

    fun selectObjectForRunnerParameter(handle: ObjectHandle) {
        _uiState.update { state ->
            val runner = state.activeRunner
            val selMode = state.selectionMode
            if (runner != null && selMode.active && selMode.targetParameterIndex >= 0) {
                val updatedArgs = runner.arguments.toMutableMap()
                val currentArg = updatedArgs[selMode.targetParameterIndex]
                    ?: ArgumentInput(selMode.targetParameterIndex, selMode.parameterName)
                updatedArgs[selMode.targetParameterIndex] = currentArg.copy(
                    selectedObjectHandleId = handle.id,
                    rawValue = "@${handle.id}",
                    isNull = false
                )
                state.copy(
                    activeScreen = ActiveScreen.RUNNER,
                    selectionMode = ExplorerSelectionMode(active = false),
                    activeRunner = runner.copy(arguments = updatedArgs),
                    statusMessage = "Bound ${handle.id} to parameter '${selMode.parameterName}'"
                )
            } else {
                state.copy(
                    activeScreen = ActiveScreen.RUNNER,
                    selectionMode = ExplorerSelectionMode(active = false)
                )
            }
        }
    }

    fun setRunnerArgumentValue(index: Int, rawValue: String) {
        _uiState.update { state ->
            val runner = state.activeRunner ?: return@update state
            val currentArg = runner.arguments[index] ?: ArgumentInput(index, "arg$index")
            val updatedMap = runner.arguments.toMutableMap()
            updatedMap[index] = currentArg.copy(rawValue = rawValue, selectedObjectHandleId = null, isNull = false)
            state.copy(activeRunner = runner.copy(arguments = updatedMap))
        }
    }

    fun setRunnerArgumentNull(index: Int, isNull: Boolean) {
        _uiState.update { state ->
            val runner = state.activeRunner ?: return@update state
            val currentArg = runner.arguments[index] ?: ArgumentInput(index, "arg$index")
            val updatedMap = runner.arguments.toMutableMap()
            updatedMap[index] = currentArg.copy(isNull = isNull)
            state.copy(activeRunner = runner.copy(arguments = updatedMap))
        }
    }

    fun setRunnerFieldEditValue(newValue: String) {
        _uiState.update { state ->
            val runner = state.activeRunner ?: return@update state
            state.copy(activeRunner = runner.copy(fieldEditValue = newValue))
        }
    }

    fun setRunnerSafetyConfirmed(confirmed: Boolean) {
        _uiState.update { state ->
            val runner = state.activeRunner ?: return@update state
            state.copy(activeRunner = runner.copy(isConfirmedSafe = confirmed))
        }
    }

    fun executeRunner() {
        val runner = _uiState.value.activeRunner ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { state ->
                val r = state.activeRunner ?: return@update state
                state.copy(activeRunner = r.copy(isExecuting = true))
            }

            when (val op = runner.operation) {
                is RuntimeOperation.InvokeMethod -> {
                    val method = runner.method ?: return@launch
                    val argsList = runner.parameterSpecs.map { spec ->
                        runner.arguments[spec.index] ?: ArgumentInput(spec.index, spec.name, isNull = true)
                    }

                    val res = RuntimeExplorer.executeMethod(
                        targetHandle = op.target,
                        targetClassName = op.targetClassName,
                        method = method,
                        arguments = argsList
                    )
                    refreshHistory()
                    refreshObjects()

                    when (res) {
                        is ExplorerResult.Success -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(
                                    activeRunner = r.copy(isExecuting = false, lastExecutionRecord = res.value),
                                    statusMessage = "Method executed in ${res.value.durationMs}ms -> ${res.value.resultString}"
                                )
                            }
                        }
                        is ExplorerResult.Failure -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(
                                    activeRunner = r.copy(isExecuting = false, lastExecutionRecord = r.lastExecutionRecord),
                                    statusMessage = res.error.message
                                )
                            }
                        }
                    }
                }

                is RuntimeOperation.CreateInstance -> {
                    val constructor = runner.constructor ?: return@launch
                    val argsList = runner.parameterSpecs.map { spec ->
                        runner.arguments[spec.index] ?: ArgumentInput(spec.index, spec.name, isNull = true)
                    }
                    val clazz = Class.forName(op.targetClassName)
                    val res = RuntimeExplorer.runtimeRunner.executeConstructor(clazz, constructor, argsList)
                    refreshHistory()
                    refreshObjects()

                    when (res) {
                        is ExplorerResult.Success -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(
                                    activeRunner = r.copy(isExecuting = false, lastExecutionRecord = res.value),
                                    statusMessage = "Instance created -> ${res.value.returnObjectHandle?.id}"
                                )
                            }
                        }
                        is ExplorerResult.Failure -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(
                                    activeRunner = r.copy(isExecuting = false, lastExecutionRecord = r.lastExecutionRecord),
                                    statusMessage = res.error.message
                                )
                            }
                        }
                    }
                }

                is RuntimeOperation.WriteField -> {
                    val clazz = Class.forName(op.targetClassName)
                    val res = RuntimeExplorer.runtimeRunner.executeFieldModification(
                        targetHandle = op.target,
                        targetClass = clazz,
                        fieldName = op.fieldName,
                        newValueString = runner.fieldEditValue
                    )
                    refreshHistory()
                    refreshObjects()

                    when (res) {
                        is ExplorerResult.Success -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(
                                    activeRunner = r.copy(
                                        isExecuting = false,
                                        lastExecutionRecord = res.value,
                                        fieldCurrentValue = runner.fieldEditValue
                                    ),
                                    statusMessage = "Field '${op.fieldName}' updated to ${runner.fieldEditValue}"
                                )
                            }
                        }
                        is ExplorerResult.Failure -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(activeRunner = r.copy(isExecuting = false), statusMessage = res.error.message)
                            }
                        }
                    }
                }

                is RuntimeOperation.InvokeBinderOperation -> {
                    val argsList = runner.parameterSpecs.map { spec ->
                        runner.arguments[spec.index] ?: ArgumentInput(spec.index, spec.name, isNull = true)
                    }
                    val res = RuntimeExplorer.runtimeRunner.executeBinderOperation(op, argsList)
                    refreshHistory()
                    refreshObjects()

                    when (res) {
                        is ExplorerResult.Success -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(
                                    activeRunner = r.copy(isExecuting = false, lastExecutionRecord = res.value),
                                    statusMessage = "Binder IPC executed in ${res.value.durationMs}ms -> ${res.value.resultString}"
                                )
                            }
                        }
                        is ExplorerResult.Failure -> {
                            _uiState.update { state ->
                                val r = state.activeRunner ?: return@update state
                                state.copy(
                                    activeRunner = r.copy(isExecuting = false, lastExecutionRecord = r.lastExecutionRecord),
                                    statusMessage = res.error.message
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- OTHER TELEMETRY & MANAGEMENT ----------------

    fun refreshObjects() {
        val objs = RuntimeExplorer.objectRegistry.getAllHandles()
        _uiState.update { it.copy(registeredObjects = objs) }
    }

    fun refreshHistory() {
        val hist = RuntimeExplorer.runtimeRunner.getHistory()
        _uiState.update { it.copy(executionHistory = hist) }
    }

    fun selectThread(thread: ThreadInfoModel?) {
        _uiState.update { it.copy(selectedThread = thread) }
    }

    fun triggerGc() {
        Runtime.getRuntime().gc()
        refreshTelemetry()
    }

    fun takeSnapshot(label: String = "Manual Snapshot") = createSnapshot(label)

    fun createSnapshot(label: String = "Manual Snapshot") {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = RuntimeExplorer.createSnapshot(label)
            _uiState.update { it.copy(snapshots = it.snapshots + snapshot, statusMessage = "Snapshot '${snapshot.label}' created") }
        }
    }

    fun compareSnapshots(base: RuntimeSnapshot, target: RuntimeSnapshot) {
        viewModelScope.launch(Dispatchers.IO) {
            val diff = RuntimeExplorer.diffSnapshots(base, target)
            _uiState.update { it.copy(selectedDiff = diff) }
        }
    }

    fun executePluginAction(action: ExplorerAction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val output = action.execute()
                _uiState.update { it.copy(lastActionOutput = output, statusMessage = "Action succeeded: ${action.title}") }
            } catch (e: Throwable) {
                _uiState.update { it.copy(statusMessage = "Action failed: ${e.message}") }
            }
        }
    }

    fun executeQuery(field: String, operator: QueryPredicate.Operator, value: String, targetType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val predicate = QueryPredicate(field, operator, value)
            val res = when (targetType) {
                "Classes" -> RuntimeExplorer.queryEngine.queryClasses(_uiState.value.discoveredClasses, listOf(predicate))
                "Services" -> RuntimeExplorer.queryEngine.queryServices(_uiState.value.services, listOf(predicate))
                else -> RuntimeExplorer.queryEngine.queryThreads(_uiState.value.threads, listOf(predicate))
            }
            _uiState.update { it.copy(lastQueryResult = res) }
        }
    }

    fun registerObjectFromClassName(className: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clazz = Class.forName(className)
                val constructor = clazz.getDeclaredConstructor().apply { isAccessible = true }
                val instance = constructor.newInstance()
                val handle = RuntimeExplorer.objectRegistry.registerObject(instance, "Created $className", retainStrong = true)
                refreshObjects()
                _uiState.update { it.copy(statusMessage = "Registered new instance ${handle.id}") }
            } catch (e: Throwable) {
                _uiState.update { it.copy(statusMessage = "Failed to instantiate $className: ${e.message}") }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun clearStatusMessage() = clearStatus()
}

