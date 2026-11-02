package com.example.uare.runner

import com.example.uare.api.ExplorerError
import com.example.uare.api.ExplorerResult
import com.example.uare.api.RuntimeOperation
import com.example.uare.inspection.FieldOperations
import com.example.uare.inspection.ObjectHandle
import com.example.uare.inspection.ObjectRegistry
import com.example.uare.inspection.TypeConverter
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.UUID

data class ParameterSpec(
    val index: Int,
    val name: String,
    val type: Class<*>,
    val typeName: String,
    val simpleTypeName: String,
    val isPrimitive: Boolean,
    val isString: Boolean,
    val isEnum: Boolean,
    val enumConstants: List<String> = emptyList(),
    val isObjectReference: Boolean
)

data class ArgumentInput(
    val parameterIndex: Int,
    val parameterName: String,
    val rawValue: String = "",
    val selectedObjectHandleId: String? = null,
    val isNull: Boolean = false
)

data class ExecutionRecord(
    val id: String = "exec_${UUID.randomUUID().toString().take(6)}",
    val timestamp: Long = System.currentTimeMillis(),
    val methodName: String,
    val targetHandleId: String?,
    val targetClassName: String,
    val argumentsSummary: String,
    val isSuccess: Boolean,
    val durationMs: Long,
    val resultString: String,
    val returnObjectHandle: ObjectHandle? = null,
    val exceptionMessage: String? = null,
    val stackTrace: List<String> = emptyList()
)

class RuntimeRunner(
    private val typeConverter: TypeConverter = TypeConverter(),
    private val objectRegistry: ObjectRegistry
) {
    private val executionHistory = mutableListOf<ExecutionRecord>()
    private val fieldOperations = FieldOperations(typeConverter)

    fun getHistory(): List<ExecutionRecord> = executionHistory.toList()

    fun clearHistory() {
        executionHistory.clear()
    }

    fun extractParameterSpecs(method: Method): List<ParameterSpec> {
        return extractSpecsFromClasses(method.parameterTypes, try {
            method.parameters.map { it.name }
        } catch (t: Throwable) {
            method.parameterTypes.indices.map { "arg$it" }
        })
    }

    fun extractConstructorParameterSpecs(constructor: Constructor<*>): List<ParameterSpec> {
        return extractSpecsFromClasses(constructor.parameterTypes, try {
            constructor.parameters.map { it.name }
        } catch (t: Throwable) {
            constructor.parameterTypes.indices.map { "param$it" }
        })
    }

    private fun extractSpecsFromClasses(paramTypes: Array<Class<*>>, paramNames: List<String>): List<ParameterSpec> {
        return paramTypes.mapIndexed { idx, type ->
            val isPrim = type.isPrimitive || type == java.lang.Integer::class.java ||
                    type == java.lang.Boolean::class.java || type == java.lang.Long::class.java ||
                    type == java.lang.Double::class.java || type == java.lang.Float::class.java ||
                    type == java.lang.Byte::class.java || type == java.lang.Short::class.java ||
                    type == java.lang.Character::class.java
            val isStr = type == java.lang.String::class.java
            val isEnum = type.isEnum
            val enumConstants = if (isEnum) type.enumConstants?.map { (it as Enum<*>).name } ?: emptyList() else emptyList()
            val isObjRef = !isPrim && !isStr && !isEnum

            ParameterSpec(
                index = idx,
                name = paramNames.getOrElse(idx) { "param$idx" },
                type = type,
                typeName = type.name,
                simpleTypeName = type.simpleName,
                isPrimitive = isPrim,
                isString = isStr,
                isEnum = isEnum,
                enumConstants = enumConstants,
                isObjectReference = isObjRef
            )
        }
    }

    fun executeMethod(
        targetHandle: ObjectHandle?,
        targetClass: Class<*>,
        method: Method,
        arguments: List<ArgumentInput>
    ): ExplorerResult<ExecutionRecord> {
        val start = System.currentTimeMillis()
        return try {
            method.isAccessible = true
            val isStatic = Modifier.isStatic(method.modifiers)
            val instance = if (isStatic) null else {
                if (targetHandle == null) {
                    return ExplorerResult.Failure(
                        ExplorerError.of(ExplorerError.ErrorCode.INVALID_ARGUMENTS, "Instance required for non-static method ${method.name}")
                    )
                }
                objectRegistry.getObject(targetHandle.id) ?: return ExplorerResult.Failure(
                    ExplorerError.of(ExplorerError.ErrorCode.RESOURCE_LIMIT_REACHED, "Target object ${targetHandle.id} was garbage collected")
                )
            }

            val paramTypes = method.parameterTypes
            val resolvedArgs = mutableListOf<Any?>()
            val summaryTokens = mutableListOf<String>()

            for (i in paramTypes.indices) {
                val input = arguments.getOrNull(i)
                val paramType = paramTypes[i]

                if (input == null || input.isNull) {
                    resolvedArgs.add(null)
                    summaryTokens.add("null")
                    continue
                }

                if (input.selectedObjectHandleId != null) {
                    val obj = objectRegistry.getObject(input.selectedObjectHandleId)
                    resolvedArgs.add(obj)
                    summaryTokens.add("@${input.selectedObjectHandleId}")
                } else {
                    val conv = typeConverter.convertValue(input.rawValue, paramType)
                    when (conv) {
                        is ExplorerResult.Failure -> return ExplorerResult.Failure(conv.error)
                        is ExplorerResult.Success -> {
                            resolvedArgs.add(conv.value)
                            summaryTokens.add("'${input.rawValue}'")
                        }
                    }
                }
            }

            val result = method.invoke(instance, *resolvedArgs.toTypedArray())
            val duration = System.currentTimeMillis() - start

            var returnHandle: ObjectHandle? = null
            if (result != null && !isPrimitiveOrDirect(result.javaClass)) {
                returnHandle = objectRegistry.registerObject(
                    result,
                    "Result of ${method.name}()",
                    retainStrong = true
                )
            }

            val record = ExecutionRecord(
                methodName = method.name,
                targetHandleId = targetHandle?.id,
                targetClassName = targetClass.name,
                argumentsSummary = summaryTokens.joinToString(", "),
                isSuccess = true,
                durationMs = duration,
                resultString = result?.toString() ?: "null",
                returnObjectHandle = returnHandle
            )
            executionHistory.add(0, record)
            ExplorerResult.Success(record)
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - start
            val cause = e.cause ?: e
            val record = ExecutionRecord(
                methodName = method.name,
                targetHandleId = targetHandle?.id,
                targetClassName = targetClass.name,
                argumentsSummary = arguments.joinToString { it.rawValue },
                isSuccess = false,
                durationMs = duration,
                resultString = "[Exception Thrown]",
                exceptionMessage = "${cause.javaClass.simpleName}: ${cause.message}",
                stackTrace = cause.stackTrace.take(10).map { it.toString() }
            )
            executionHistory.add(0, record)
            ExplorerResult.Failure(
                ExplorerError.of(
                    ExplorerError.ErrorCode.INVOCATION_EXCEPTION,
                    "Execution of ${method.name} failed: ${cause.message}",
                    cause
                )
            )
        }
    }

    fun executeConstructor(
        targetClass: Class<*>,
        constructor: Constructor<*>,
        arguments: List<ArgumentInput>
    ): ExplorerResult<ExecutionRecord> {
        val start = System.currentTimeMillis()
        return try {
            constructor.isAccessible = true
            val paramTypes = constructor.parameterTypes
            val resolvedArgs = mutableListOf<Any?>()
            val summaryTokens = mutableListOf<String>()

            for (i in paramTypes.indices) {
                val input = arguments.getOrNull(i)
                val paramType = paramTypes[i]

                if (input == null || input.isNull) {
                    resolvedArgs.add(null)
                    summaryTokens.add("null")
                    continue
                }

                if (input.selectedObjectHandleId != null) {
                    val obj = objectRegistry.getObject(input.selectedObjectHandleId)
                    resolvedArgs.add(obj)
                    summaryTokens.add("@${input.selectedObjectHandleId}")
                } else {
                    val conv = typeConverter.convertValue(input.rawValue, paramType)
                    when (conv) {
                        is ExplorerResult.Failure -> return ExplorerResult.Failure(conv.error)
                        is ExplorerResult.Success -> {
                            resolvedArgs.add(conv.value)
                            summaryTokens.add("'${input.rawValue}'")
                        }
                    }
                }
            }

            val newInstance = constructor.newInstance(*resolvedArgs.toTypedArray())
            val duration = System.currentTimeMillis() - start

            val handle = objectRegistry.registerObject(
                newInstance,
                "Constructed ${targetClass.simpleName}",
                retainStrong = true
            )

            val record = ExecutionRecord(
                methodName = "new ${targetClass.simpleName}()",
                targetHandleId = handle.id,
                targetClassName = targetClass.name,
                argumentsSummary = summaryTokens.joinToString(", "),
                isSuccess = true,
                durationMs = duration,
                resultString = "Instantiated: $newInstance",
                returnObjectHandle = handle
            )
            executionHistory.add(0, record)
            ExplorerResult.Success(record)
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - start
            val cause = e.cause ?: e
            val record = ExecutionRecord(
                methodName = "new ${targetClass.simpleName}()",
                targetHandleId = null,
                targetClassName = targetClass.name,
                argumentsSummary = arguments.joinToString { it.rawValue },
                isSuccess = false,
                durationMs = duration,
                resultString = "[Constructor Exception]",
                exceptionMessage = "${cause.javaClass.simpleName}: ${cause.message}",
                stackTrace = cause.stackTrace.take(10).map { it.toString() }
            )
            executionHistory.add(0, record)
            ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.INVOCATION_EXCEPTION, "Constructor failed: ${cause.message}", cause)
            )
        }
    }

    fun executeFieldModification(
        targetHandle: ObjectHandle,
        targetClass: Class<*>,
        fieldName: String,
        newValueString: String
    ): ExplorerResult<ExecutionRecord> {
        val start = System.currentTimeMillis()
        val target = objectRegistry.getObject(targetHandle.id)
            ?: return ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.RESOURCE_LIMIT_REACHED, "Target object ${targetHandle.id} collected")
            )

        val writeRes = fieldOperations.writeField(target, targetClass, fieldName, newValueString)
        val duration = System.currentTimeMillis() - start

        return when (writeRes) {
            is ExplorerResult.Success -> {
                val mod = writeRes.value
                val record = ExecutionRecord(
                    methodName = "set $fieldName",
                    targetHandleId = targetHandle.id,
                    targetClassName = targetClass.name,
                    argumentsSummary = newValueString,
                    isSuccess = true,
                    durationMs = duration,
                    resultString = "Field '$fieldName' modified: ${mod.oldValue} -> ${mod.newValue}"
                )
                executionHistory.add(0, record)
                ExplorerResult.Success(record)
            }
            is ExplorerResult.Failure -> {
                val record = ExecutionRecord(
                    methodName = "set $fieldName",
                    targetHandleId = targetHandle.id,
                    targetClassName = targetClass.name,
                    argumentsSummary = newValueString,
                    isSuccess = false,
                    durationMs = duration,
                    resultString = "[Field Write Failed]",
                    exceptionMessage = writeRes.error.message
                )
                executionHistory.add(0, record)
                ExplorerResult.Failure(writeRes.error)
            }
        }
    }

    fun extractBinderParameterSpecs(paramTypeNames: List<String>, paramNames: List<String>): List<ParameterSpec> {
        val resolvedClasses = paramTypeNames.map { name ->
            when (name.lowercase()) {
                "int", "integer" -> Int::class.javaPrimitiveType ?: Int::class.java
                "boolean", "bool" -> Boolean::class.javaPrimitiveType ?: Boolean::class.java
                "long" -> Long::class.javaPrimitiveType ?: Long::class.java
                "float" -> Float::class.javaPrimitiveType ?: Float::class.java
                "double" -> Double::class.javaPrimitiveType ?: Double::class.java
                "byte" -> Byte::class.javaPrimitiveType ?: Byte::class.java
                "short" -> Short::class.javaPrimitiveType ?: Short::class.java
                "char", "character" -> Char::class.javaPrimitiveType ?: Char::class.java
                "string" -> String::class.java
                else -> try {
                    Class.forName(name)
                } catch (_: Throwable) {
                    Any::class.java
                }
            }
        }.toTypedArray()
        return extractSpecsFromClasses(resolvedClasses, paramNames)
    }

    fun executeBinderOperation(
        operation: RuntimeOperation.InvokeBinderOperation,
        arguments: List<ArgumentInput>
    ): ExplorerResult<ExecutionRecord> {
        val start = System.currentTimeMillis()
        return try {
            val proxyInstance = operation.proxyHandleId?.let { objectRegistry.getObject(it) }

            val result: Any?
            if (proxyInstance != null) {
                val method = proxyInstance.javaClass.methods.firstOrNull {
                    it.name == operation.operationName && it.parameterTypes.size == operation.parameterTypes.size
                } ?: return ExplorerResult.Failure(
                    ExplorerError.of(ExplorerError.ErrorCode.INVOCATION_EXCEPTION, "Binder method '${operation.operationName}' not found on proxy")
                )
                method.isAccessible = true
                val paramTypes = method.parameterTypes
                val resolvedArgs = mutableListOf<Any?>()
                val summaryTokens = mutableListOf<String>()

                for (i in paramTypes.indices) {
                    val input = arguments.getOrNull(i)
                    val paramType = paramTypes[i]
                    if (input == null || input.isNull) {
                        resolvedArgs.add(null)
                        summaryTokens.add("null")
                    } else if (input.selectedObjectHandleId != null) {
                        resolvedArgs.add(objectRegistry.getObject(input.selectedObjectHandleId))
                        summaryTokens.add("@${input.selectedObjectHandleId}")
                    } else {
                        val conv = typeConverter.convertValue(input.rawValue, paramType)
                        when (conv) {
                            is ExplorerResult.Success -> {
                                resolvedArgs.add(conv.value)
                                summaryTokens.add("'${input.rawValue}'")
                            }
                            is ExplorerResult.Failure -> return ExplorerResult.Failure(conv.error)
                        }
                    }
                }
                result = method.invoke(proxyInstance, *resolvedArgs.toTypedArray())
            } else {
                val sm = Class.forName("android.os.ServiceManager")
                val getService = sm.getMethod("getService", String::class.java)
                val binder = getService.invoke(null, operation.serviceName) as? android.os.IBinder
                    ?: return ExplorerResult.Failure(
                        ExplorerError.of(ExplorerError.ErrorCode.RESOURCE_LIMIT_REACHED, "Service '${operation.serviceName}' not found or restricted")
                    )

                result = when (operation.operationName) {
                    "pingBinder" -> binder.pingBinder()
                    "isBinderAlive" -> binder.isBinderAlive
                    "getInterfaceDescriptor" -> binder.interfaceDescriptor
                    else -> "Binder ${binder.interfaceDescriptor} is alive (ping=${binder.pingBinder()})"
                }
            }

            val duration = System.currentTimeMillis() - start
            var returnHandle: ObjectHandle? = null
            if (result != null && !isPrimitiveOrDirect(result.javaClass)) {
                returnHandle = objectRegistry.registerObject(
                    result,
                    "Result of ${operation.serviceName}#${operation.operationName}()",
                    retainStrong = true
                )
            }

            val record = ExecutionRecord(
                methodName = operation.title,
                targetHandleId = operation.proxyHandleId,
                targetClassName = operation.targetClassName,
                argumentsSummary = arguments.joinToString { it.rawValue },
                isSuccess = true,
                durationMs = duration,
                resultString = result?.toString() ?: "null (void)",
                returnObjectHandle = returnHandle
            )
            executionHistory.add(0, record)
            ExplorerResult.Success(record)
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - start
            val cause = e.cause ?: e
            val record = ExecutionRecord(
                methodName = operation.title,
                targetHandleId = operation.proxyHandleId,
                targetClassName = operation.targetClassName,
                argumentsSummary = arguments.joinToString { it.rawValue },
                isSuccess = false,
                durationMs = duration,
                resultString = "[Binder IPC Exception]",
                exceptionMessage = "${cause.javaClass.simpleName}: ${cause.message}",
                stackTrace = cause.stackTrace.take(10).map { it.toString() }
            )
            executionHistory.add(0, record)
            ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.INVOCATION_EXCEPTION, "Binder IPC failed: ${cause.message}", cause)
            )
        }
    }

    private fun isPrimitiveOrDirect(clazz: Class<*>): Boolean {
        return clazz.isPrimitive ||
                clazz == java.lang.String::class.java ||
                clazz == java.lang.Integer::class.java ||
                clazz == java.lang.Boolean::class.java ||
                clazz == java.lang.Long::class.java ||
                clazz == java.lang.Double::class.java ||
                clazz == java.lang.Float::class.java ||
                clazz == java.lang.Byte::class.java ||
                clazz == java.lang.Short::class.java ||
                clazz.isEnum
    }
}

