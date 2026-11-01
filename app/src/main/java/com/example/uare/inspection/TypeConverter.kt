package com.example.uare.inspection

import com.example.uare.api.ExplorerError
import com.example.uare.api.ExplorerResult

/**
 * Type converter engine supporting argument conversions for dynamic reflection.
 */
class TypeConverter {

    fun convertValue(rawInput: String, targetType: Class<*>): ExplorerResult<Any?> {
        return try {
            val trimmed = rawInput.trim()
            val result: Any? = when (targetType) {
                java.lang.String::class.java -> trimmed
                Int::class.java, java.lang.Integer::class.java -> trimmed.toIntOrNull() ?: 0
                Long::class.java, java.lang.Long::class.java -> trimmed.toLongOrNull() ?: 0L
                Boolean::class.java, java.lang.Boolean::class.java -> trimmed.equals("true", ignoreCase = true)
                Float::class.java, java.lang.Float::class.java -> trimmed.toFloatOrNull() ?: 0f
                Double::class.java, java.lang.Double::class.java -> trimmed.toDoubleOrNull() ?: 0.0
                Byte::class.java, java.lang.Byte::class.java -> trimmed.toByteOrNull() ?: 0.toByte()
                Short::class.java, java.lang.Short::class.java -> trimmed.toShortOrNull() ?: 0.toShort()
                Char::class.java, java.lang.Character::class.java -> trimmed.firstOrNull() ?: ' '
                Class::class.java -> Class.forName(trimmed)
                else -> {
                    if (targetType.isEnum) {
                        targetType.enumConstants?.firstOrNull { (it as Enum<*>).name.equals(trimmed, ignoreCase = true) }
                    } else if (trimmed.isEmpty() || trimmed == "null") {
                        null
                    } else {
                        // Attempt single string constructor
                        try {
                            val constructor = targetType.getConstructor(String::class.java)
                            constructor.newInstance(trimmed)
                        } catch (e: Throwable) {
                            return ExplorerResult.Failure(
                                ExplorerError.of(
                                    ExplorerError.ErrorCode.INVALID_ARGUMENTS,
                                    "Cannot convert '$trimmed' to ${targetType.name}: ${e.message}",
                                    e
                                )
                            )
                        }
                    }
                }
            }
            ExplorerResult.Success(result)
        } catch (e: Throwable) {
            ExplorerResult.Failure(
                ExplorerError.of(
                    ExplorerError.ErrorCode.INVALID_ARGUMENTS,
                    "Conversion failed for input '$rawInput' to ${targetType.name}: ${e.message}",
                    e
                )
            )
        }
    }
}
