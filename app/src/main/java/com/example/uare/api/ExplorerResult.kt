package com.example.uare.api

/**
 * Structured result type for all Universal Android Runtime Explorer operations.
 */
sealed interface ExplorerResult<out T> {
    data class Success<T>(val value: T) : ExplorerResult<T>
    data class Failure(val error: ExplorerError) : ExplorerResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw RuntimeException("${error.code}: ${error.message}", error.cause)
    }
}

inline fun <T, R> ExplorerResult<T>.map(transform: (T) -> R): ExplorerResult<R> = when (this) {
    is ExplorerResult.Success -> ExplorerResult.Success(transform(value))
    is ExplorerResult.Failure -> this
}

inline fun <T> ExplorerResult<T>.onFailure(action: (ExplorerError) -> Unit): ExplorerResult<T> {
    if (this is ExplorerResult.Failure) action(error)
    return this
}

inline fun <T> ExplorerResult<T>.onSuccess(action: (T) -> Unit): ExplorerResult<T> {
    if (this is ExplorerResult.Success) action(value)
    return this
}

/**
 * Structured error categories for runtime exploration diagnostics.
 */
data class ExplorerError(
    val code: ErrorCode,
    val message: String,
    val cause: Throwable? = null,
    val technicalDetails: Map<String, String> = emptyMap()
) {
    enum class ErrorCode {
        UNSUPPORTED_ENVIRONMENT,
        CAPABILITY_UNAVAILABLE,
        CLASS_NOT_FOUND,
        CLASS_NOT_LOADED,
        CLASSLOADER_RESOLUTION_FAILURE,
        ACCESS_DENIED,
        REFLECTION_FAILURE,
        INVALID_ARGUMENTS,
        INVOCATION_EXCEPTION,
        BINDER_FAILURE,
        TIMEOUT,
        CANCELLATION,
        RESOURCE_LIMIT_REACHED,
        PLATFORM_INCOMPATIBILITY,
        INTERNAL_ENGINE_FAILURE
    }

    companion object {
        fun of(code: ErrorCode, message: String, cause: Throwable? = null): ExplorerError {
            return ExplorerError(code, message, cause)
        }
    }
}
