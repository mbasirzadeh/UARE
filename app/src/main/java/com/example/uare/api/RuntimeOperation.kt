package com.example.uare.api

import com.example.uare.inspection.ObjectHandle

/**
 * Strongly typed runtime operation configured from Explorer and executed in Runtime Runner.
 */
sealed interface RuntimeOperation {
    val title: String
    val operationType: String
    val targetClassName: String

    data class InvokeMethod(
        val target: ObjectHandle?,
        override val targetClassName: String,
        val methodName: String,
        val parameterTypes: List<String>,
        val returnType: String
    ) : RuntimeOperation {
        override val title: String get() = "${targetClassName.substringAfterLast('.')}#$methodName()"
        override val operationType: String get() = if (target != null) "INSTANCE METHOD INVOCATION" else "STATIC METHOD INVOCATION"
    }

    data class WriteField(
        val target: ObjectHandle,
        override val targetClassName: String,
        val fieldName: String,
        val fieldType: String,
        val currentValue: String
    ) : RuntimeOperation {
        override val title: String get() = "${targetClassName.substringAfterLast('.')}.${fieldName}"
        override val operationType: String get() = "FIELD MODIFICATION"
    }

    data class CreateInstance(
        override val targetClassName: String,
        val constructorSignature: String,
        val parameterTypes: List<String>
    ) : RuntimeOperation {
        override val title: String get() = "new ${targetClassName.substringAfterLast('.')}()"
        override val operationType: String get() = "CONSTRUCTOR INVOCATION"
    }

    data class InvokeBinderOperation(
        val serviceName: String,
        val descriptor: String?,
        val operationName: String
    ) : RuntimeOperation {
        override val targetClassName: String get() = descriptor ?: "android.os.IBinder"
        override val title: String get() = "$serviceName#$operationName"
        override val operationType: String get() = "BINDER SERVICE OPERATION"
    }
}

/**
 * Selection mode metadata when Runner requests Explorer to browse and pick a compatible object.
 */
data class ExplorerSelectionMode(
    val active: Boolean = false,
    val targetParameterIndex: Int = -1,
    val expectedTypeName: String = "",
    val parameterName: String = "",
    val prompt: String = ""
)
