package com.example.uare.inspection

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

data class ClassDetails(
    val fullyQualifiedName: String,
    val simpleName: String,
    val packageName: String,
    val modifiers: List<String>,
    val isInterface: Boolean,
    val isEnum: Boolean,
    val isArray: Boolean,
    val isPrimitive: Boolean,
    val superclassName: String?,
    val interfaces: List<String>,
    val annotations: List<String>,
    val classLoaderName: String?,
    val constructors: List<ConstructorDetails>,
    val methods: List<MethodDetails>,
    val fields: List<FieldDetails>
)

data class ConstructorDetails(
    val signature: String,
    val modifiers: List<String>,
    val parameterTypes: List<String>,
    val exceptionTypes: List<String>
)

data class MethodDetails(
    val name: String,
    val returnType: String,
    val parameterTypes: List<String>,
    val modifiers: List<String>,
    val declaringClass: String,
    val isStatic: Boolean,
    val isSynthetic: Boolean,
    val isBridge: Boolean,
    val annotations: List<String>,
    val signature: String
)

data class FieldDetails(
    val name: String,
    val type: String,
    val modifiers: List<String>,
    val declaringClass: String,
    val isStatic: Boolean,
    val isFinal: Boolean,
    val annotations: List<String>
)

class ClassInspector {

    fun inspectClass(clazz: Class<*>): ClassDetails {
        val mods = decodeModifiers(clazz.modifiers)

        val constructors = clazz.declaredConstructors.map { c ->
            ConstructorDetails(
                signature = "${clazz.simpleName}(${c.parameterTypes.joinToString { it.simpleName }})",
                modifiers = decodeModifiers(c.modifiers),
                parameterTypes = c.parameterTypes.map { it.name },
                exceptionTypes = c.exceptionTypes.map { it.name }
            )
        }

        val methods = clazz.declaredMethods.map { m ->
            MethodDetails(
                name = m.name,
                returnType = m.returnType.name,
                parameterTypes = m.parameterTypes.map { it.name },
                modifiers = decodeModifiers(m.modifiers),
                declaringClass = m.declaringClass.name,
                isStatic = Modifier.isStatic(m.modifiers),
                isSynthetic = m.isSynthetic,
                isBridge = m.isBridge,
                annotations = m.annotations.map { it.annotationClass.java.simpleName },
                signature = "${m.returnType.simpleName} ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})"
            )
        }

        val fields = clazz.declaredFields.map { f ->
            FieldDetails(
                name = f.name,
                type = f.type.name,
                modifiers = decodeModifiers(f.modifiers),
                declaringClass = f.declaringClass.name,
                isStatic = Modifier.isStatic(f.modifiers),
                isFinal = Modifier.isFinal(f.modifiers),
                annotations = f.annotations.map { it.annotationClass.java.simpleName }
            )
        }

        return ClassDetails(
            fullyQualifiedName = clazz.name,
            simpleName = clazz.simpleName,
            packageName = clazz.`package`?.name ?: clazz.name.substringBeforeLast('.', ""),
            modifiers = mods,
            isInterface = clazz.isInterface,
            isEnum = clazz.isEnum,
            isArray = clazz.isArray,
            isPrimitive = clazz.isPrimitive,
            superclassName = clazz.superclass?.name,
            interfaces = clazz.interfaces.map { it.name },
            annotations = clazz.annotations.map { it.annotationClass.java.simpleName },
            classLoaderName = clazz.classLoader?.javaClass?.simpleName,
            constructors = constructors,
            methods = methods,
            fields = fields
        )
    }

    private fun decodeModifiers(mod: Int): List<String> {
        val list = mutableListOf<String>()
        if (Modifier.isPublic(mod)) list.add("public")
        if (Modifier.isProtected(mod)) list.add("protected")
        if (Modifier.isPrivate(mod)) list.add("private")
        if (Modifier.isStatic(mod)) list.add("static")
        if (Modifier.isFinal(mod)) list.add("final")
        if (Modifier.isAbstract(mod)) list.add("abstract")
        if (Modifier.isSynchronized(mod)) list.add("synchronized")
        if (Modifier.isVolatile(mod)) list.add("volatile")
        if (Modifier.isTransient(mod)) list.add("transient")
        if (Modifier.isNative(mod)) list.add("native")
        return list
    }
}
