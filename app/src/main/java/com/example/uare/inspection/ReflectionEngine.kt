package com.example.uare.inspection

import com.example.uare.api.ExplorerError
import com.example.uare.api.ExplorerResult
import java.lang.reflect.Method
import java.lang.reflect.Modifier

data class InvocationResult(
    val methodName: String,
    val returnType: String,
    val result: Any?,
    val executionTimeMs: Long
)

class ReflectionEngine(
    private val typeConverter: TypeConverter = TypeConverter()
) {

    fun invokeMethod(
        target: Any?,
        clazz: Class<*>,
        methodName: String,
        rawArguments: List<String>
    ): ExplorerResult<InvocationResult> {
        val startTime = System.currentTimeMillis()
        try {
            // Find candidate methods matching name and parameter count
            val candidates = clazz.methods.filter {
                it.name == methodName && it.parameterTypes.size == rawArguments.size
            }

            if (candidates.isEmpty()) {
                // Try declared methods
                val declaredCandidates = clazz.declaredMethods.filter {
                    it.name == methodName && it.parameterTypes.size == rawArguments.size
                }
                if (declaredCandidates.isEmpty()) {
                    return ExplorerResult.Failure(
                        ExplorerError.of(
                            ExplorerError.ErrorCode.REFLECTION_FAILURE,
                            "No method '$methodName' with ${rawArguments.size} parameters found on ${clazz.name}"
                        )
                    )
                }
                return invokeCandidate(target, declaredCandidates.first(), rawArguments, startTime)
            }

            return invokeCandidate(target, candidates.first(), rawArguments, startTime)
        } catch (e: Throwable) {
            return ExplorerResult.Failure(
                ExplorerError.of(
                    ExplorerError.ErrorCode.INVOCATION_EXCEPTION,
                    "Execution error on $methodName: ${e.message}",
                    e
                )
            )
        }
    }

    private fun invokeCandidate(
        target: Any?,
        method: Method,
        rawArguments: List<String>,
        startTime: Long
    ): ExplorerResult<InvocationResult> {
        try {
            method.isAccessible = true
            val paramTypes = method.parameterTypes
            val convertedArgs = mutableListOf<Any?>()

            for (i in paramTypes.indices) {
                val conv = typeConverter.convertValue(rawArguments[i], paramTypes[i])
                when (conv) {
                    is ExplorerResult.Success -> convertedArgs.add(conv.value)
                    is ExplorerResult.Failure -> return ExplorerResult.Failure(conv.error)
                }
            }

            val isStatic = Modifier.isStatic(method.modifiers)
            val invTarget = if (isStatic) null else target

            val result = method.invoke(invTarget, *convertedArgs.toTypedArray())
            val duration = System.currentTimeMillis() - startTime

            return ExplorerResult.Success(
                InvocationResult(
                    methodName = method.name,
                    returnType = method.returnType.name,
                    result = result,
                    executionTimeMs = duration
                )
            )
        } catch (e: Throwable) {
            val actualCause = e.cause ?: e
            return ExplorerResult.Failure(
                ExplorerError.of(
                    ExplorerError.ErrorCode.INVOCATION_EXCEPTION,
                    "Method '${method.name}' threw: ${actualCause.javaClass.simpleName}: ${actualCause.message}",
                    actualCause
                )
            )
        }
    }
}
