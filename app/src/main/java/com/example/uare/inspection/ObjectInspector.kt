package com.example.uare.inspection

import java.lang.reflect.Modifier

data class ObjectInspectionNode(
    val name: String,
    val type: String,
    val valueString: String,
    val isPrimitiveOrWrapper: Boolean,
    val isCircularReference: Boolean = false,
    val depth: Int,
    val children: List<ObjectInspectionNode> = emptyList()
)

class ObjectInspector {

    fun inspectObject(
        target: Any?,
        fieldName: String = "this",
        maxDepth: Int = 3,
        currentDepth: Int = 0,
        visited: MutableSet<Int> = mutableSetOf()
    ): ObjectInspectionNode {
        if (target == null) {
            return ObjectInspectionNode(
                name = fieldName,
                type = "null",
                valueString = "null",
                isPrimitiveOrWrapper = true,
                depth = currentDepth
            )
        }

        val targetClass = target.javaClass
        val isPrimitive = isPrimitiveOrDirectValue(targetClass)

        if (isPrimitive) {
            return ObjectInspectionNode(
                name = fieldName,
                type = targetClass.simpleName,
                valueString = target.toString(),
                isPrimitiveOrWrapper = true,
                depth = currentDepth
            )
        }

        val identity = System.identityHashCode(target)
        if (visited.contains(identity)) {
            return ObjectInspectionNode(
                name = fieldName,
                type = targetClass.simpleName,
                valueString = "[Circular Ref: ${targetClass.simpleName}@${Integer.toHexString(identity)}]",
                isPrimitiveOrWrapper = false,
                isCircularReference = true,
                depth = currentDepth
            )
        }

        if (currentDepth >= maxDepth) {
            return ObjectInspectionNode(
                name = fieldName,
                type = targetClass.simpleName,
                valueString = "[Depth limit reached: ${targetClass.simpleName}@${Integer.toHexString(identity)}]",
                isPrimitiveOrWrapper = false,
                depth = currentDepth
            )
        }

        visited.add(identity)
        val children = mutableListOf<ObjectInspectionNode>()

        try {
            when (target) {
                is Array<*> -> {
                    target.take(20).forEachIndexed { index, item ->
                        children.add(inspectObject(item, "[$index]", maxDepth, currentDepth + 1, visited))
                    }
                }
                is Collection<*> -> {
                    target.take(20).forEachIndexed { index, item ->
                        children.add(inspectObject(item, "[$index]", maxDepth, currentDepth + 1, visited))
                    }
                }
                is Map<*, *> -> {
                    target.entries.take(20).forEach { (k, v) ->
                        children.add(inspectObject(v, "key: $k", maxDepth, currentDepth + 1, visited))
                    }
                }
                else -> {
                    var clazz: Class<*>? = targetClass
                    while (clazz != null && clazz != Any::class.java) {
                        for (field in clazz.declaredFields) {
                            if (Modifier.isStatic(field.modifiers)) continue
                            field.isAccessible = true
                            try {
                                val value = field.get(target)
                                children.add(inspectObject(value, field.name, maxDepth, currentDepth + 1, visited))
                            } catch (e: Throwable) {
                                children.add(
                                    ObjectInspectionNode(
                                        name = field.name,
                                        type = field.type.simpleName,
                                        valueString = "[Inaccessible: ${e.message}]",
                                        isPrimitiveOrWrapper = true,
                                        depth = currentDepth + 1
                                    )
                                )
                            }
                        }
                        clazz = clazz.superclass
                    }
                }
            }
        } catch (ignored: Throwable) {}

        return ObjectInspectionNode(
            name = fieldName,
            type = targetClass.simpleName,
            valueString = "${targetClass.simpleName}@${Integer.toHexString(identity)} (${children.size} members)",
            isPrimitiveOrWrapper = false,
            depth = currentDepth,
            children = children
        )
    }

    private fun isPrimitiveOrDirectValue(clazz: Class<*>): Boolean {
        return clazz.isPrimitive ||
                clazz == java.lang.String::class.java ||
                clazz == java.lang.Integer::class.java ||
                clazz == java.lang.Long::class.java ||
                clazz == java.lang.Boolean::class.java ||
                clazz == java.lang.Float::class.java ||
                clazz == java.lang.Double::class.java ||
                clazz == java.lang.Byte::class.java ||
                clazz == java.lang.Short::class.java ||
                clazz == java.lang.Character::class.java ||
                clazz.isEnum
    }
}
