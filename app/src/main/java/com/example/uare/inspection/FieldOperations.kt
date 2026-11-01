package com.example.uare.inspection

import com.example.uare.api.ExplorerError
import com.example.uare.api.ExplorerResult
import java.lang.reflect.Field
import java.lang.reflect.Modifier

data class FieldValueInfo(
    val name: String,
    val typeName: String,
    val value: Any?,
    val valueString: String,
    val isStatic: Boolean,
    val isFinal: Boolean,
    val isPrimitiveOrString: Boolean,
    val declaringClassName: String
)

data class FieldModificationResult(
    val fieldName: String,
    val oldValue: Any?,
    val newValue: Any?,
    val isSuccess: Boolean,
    val executionTimeMs: Long,
    val message: String
)

class FieldOperations(
    private val typeConverter: TypeConverter = TypeConverter()
) {

    fun readField(target: Any?, clazz: Class<*>, fieldName: String): ExplorerResult<FieldValueInfo> {
        return try {
            val field = findField(clazz, fieldName)
                ?: return ExplorerResult.Failure(
                    ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Field '$fieldName' not found on ${clazz.name}")
                )

            field.isAccessible = true
            val isStatic = Modifier.isStatic(field.modifiers)
            val inst = if (isStatic) null else target
            val value = field.get(inst)

            val type = field.type
            val isPrimOrStr = type.isPrimitive || type == java.lang.String::class.java ||
                    type == java.lang.Integer::class.java || type == java.lang.Boolean::class.java ||
                    type == java.lang.Long::class.java || type == java.lang.Double::class.java ||
                    type == java.lang.Float::class.java || type.isEnum

            ExplorerResult.Success(
                FieldValueInfo(
                    name = field.name,
                    typeName = type.simpleName,
                    value = value,
                    valueString = value?.toString() ?: "null",
                    isStatic = isStatic,
                    isFinal = Modifier.isFinal(field.modifiers),
                    isPrimitiveOrString = isPrimOrStr,
                    declaringClassName = field.declaringClass.name
                )
            )
        } catch (e: Throwable) {
            ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Error reading field '$fieldName': ${e.message}", e)
            )
        }
    }

    fun writeField(
        target: Any?,
        clazz: Class<*>,
        fieldName: String,
        rawStringValue: String
    ): ExplorerResult<FieldModificationResult> {
        val start = System.currentTimeMillis()
        return try {
            val field = findField(clazz, fieldName)
                ?: return ExplorerResult.Failure(
                    ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Field '$fieldName' not found on ${clazz.name}")
                )

            field.isAccessible = true
            val isStatic = Modifier.isStatic(field.modifiers)
            val inst = if (isStatic) null else target
            val oldValue = field.get(inst)

            val convertedRes = typeConverter.convertValue(rawStringValue, field.type)
            when (convertedRes) {
                is ExplorerResult.Failure -> ExplorerResult.Failure(convertedRes.error)
                is ExplorerResult.Success -> {
                    val newValue = convertedRes.value
                    field.set(inst, newValue)
                    val duration = System.currentTimeMillis() - start

                    ExplorerResult.Success(
                        FieldModificationResult(
                            fieldName = fieldName,
                            oldValue = oldValue,
                            newValue = newValue,
                            isSuccess = true,
                            executionTimeMs = duration,
                            message = "Field '$fieldName' updated: '$oldValue' -> '$newValue'"
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Failed to write field '$fieldName': ${e.message}", e)
            )
        }
    }

    fun restoreField(
        target: Any?,
        clazz: Class<*>,
        fieldName: String,
        previousValue: Any?
    ): ExplorerResult<FieldModificationResult> {
        val start = System.currentTimeMillis()
        return try {
            val field = findField(clazz, fieldName)
                ?: return ExplorerResult.Failure(
                    ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Field '$fieldName' not found on ${clazz.name}")
                )

            field.isAccessible = true
            val isStatic = Modifier.isStatic(field.modifiers)
            val inst = if (isStatic) null else target
            val currentVal = field.get(inst)

            field.set(inst, previousValue)
            val duration = System.currentTimeMillis() - start

            ExplorerResult.Success(
                FieldModificationResult(
                    fieldName = fieldName,
                    oldValue = currentVal,
                    newValue = previousValue,
                    isSuccess = true,
                    executionTimeMs = duration,
                    message = "Restored field '$fieldName' to previous value: '$previousValue'"
                )
            )
        } catch (e: Throwable) {
            ExplorerResult.Failure(
                ExplorerError.of(ExplorerError.ErrorCode.REFLECTION_FAILURE, "Failed to restore field '$fieldName': ${e.message}", e)
            )
        }
    }

    private fun findField(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            try {
                return current.getDeclaredField(name)
            } catch (ignored: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }
}
