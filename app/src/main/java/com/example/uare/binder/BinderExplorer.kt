package com.example.uare.binder

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import com.example.uare.inspection.ObjectRegistry

data class BinderMethodInfo(
    val name: String,
    val returnType: String,
    val parameterTypes: List<String>,
    val parameterNames: List<String>,
    val signature: String,
    val transactionCode: Int? = null
)

data class SystemServiceInfo(
    val name: String,
    val interfaceDescriptor: String?,
    val isAlive: Boolean,
    val isPingable: Boolean,
    val isProxy: Boolean,
    val binderClassName: String,
    val methods: List<BinderMethodInfo> = emptyList(),
    val proxyObjectHandleId: String? = null,
    val discoveryTimeMs: Long = System.currentTimeMillis()
)

interface ISampleRuntimeService {
    fun ping(): Boolean
    fun getServiceVersion(): Int
    fun echoMessage(message: String): String
    fun computeHash(payload: String, iterations: Int): String
    fun inspectProcess(pid: Int): String
    fun triggerSecurityFault(reason: String): String
}

class SampleRuntimeBinderService : Binder(), ISampleRuntimeService {
    override fun getInterfaceDescriptor(): String = "com.example.uare.binder.ISampleRuntimeService"

    override fun ping(): Boolean = true
    override fun getServiceVersion(): Int = 2
    override fun echoMessage(message: String): String = "IPC Echo: $message (Server Time: ${System.currentTimeMillis()})"
    override fun computeHash(payload: String, iterations: Int): String {
        var h = payload.hashCode()
        repeat(iterations.coerceIn(1, 100)) { h = (h * 31) xor 0x5f3759df }
        return "hash_0x" + Integer.toHexString(h)
    }
    override fun inspectProcess(pid: Int): String = "Process $pid status: ACTIVE, priority: FOREGROUND, memory: NORMAL"
    override fun triggerSecurityFault(reason: String): String {
        throw SecurityException("Binder IPC SecurityException: Caller is not authorized for operation ($reason)")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return super.onTransact(code, data, reply, flags)
    }
}

class BinderExplorer(
    private val objectRegistry: ObjectRegistry? = null
) {
    private val sampleBinderService = SampleRuntimeBinderService()

    fun discoverServices(): List<SystemServiceInfo> {
        val results = mutableListOf<SystemServiceInfo>()

        // 1. Add interactive sample runtime Binder service for guaranteed testability & invocation
        val sampleHandle = objectRegistry?.registerObject(
            sampleBinderService,
            "SampleRuntimeBinderService (In-Process Binder)",
            retainStrong = true
        )
        val sampleMethods = ISampleRuntimeService::class.java.methods.map { m ->
            val pNames = try {
                m.parameters.map { it.name }
            } catch (_: Throwable) {
                m.parameterTypes.indices.map { "arg$it" }
            }
            BinderMethodInfo(
                name = m.name,
                returnType = m.returnType.simpleName,
                parameterTypes = m.parameterTypes.map { it.simpleName },
                parameterNames = pNames,
                signature = "${m.name}(${m.parameterTypes.joinToString(", ") { it.simpleName }}): ${m.returnType.simpleName}"
            )
        }.sortedBy { it.name }

        results.add(
            SystemServiceInfo(
                name = "uare.runtime.service",
                interfaceDescriptor = sampleBinderService.interfaceDescriptor,
                isAlive = true,
                isPingable = true,
                isProxy = false,
                binderClassName = sampleBinderService.javaClass.name,
                methods = sampleMethods,
                proxyObjectHandleId = sampleHandle?.id
            )
        )

        // 2. Discover platform and framework Binder services
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val listServicesMethod = serviceManagerClass.getMethod("listServices")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)

            val serviceNames = listServicesMethod.invoke(null) as? Array<*>
            if (serviceNames != null) {
                for (item in serviceNames) {
                    val name = item as? String ?: continue
                    try {
                        val binder = getServiceMethod.invoke(null, name) as? IBinder
                        if (binder != null) {
                            val descriptor = try { binder.interfaceDescriptor } catch (_: Throwable) { null }
                            val isAlive = try { binder.isBinderAlive } catch (_: Throwable) { false }
                            val isPingable = try { binder.pingBinder() } catch (_: Throwable) { false }
                            val isProxy = binder.javaClass.name.contains("Proxy")

                            val (methods, proxyInstance) = introspectBinderMethods(binder, descriptor)
                            var proxyHandleId: String? = null
                            if (proxyInstance != null && objectRegistry != null) {
                                proxyHandleId = objectRegistry.registerObject(
                                    proxyInstance,
                                    "BinderProxy for $name",
                                    retainStrong = false
                                ).id
                            }

                            results.add(
                                SystemServiceInfo(
                                    name = name,
                                    interfaceDescriptor = descriptor,
                                    isAlive = isAlive,
                                    isPingable = isPingable,
                                    isProxy = isProxy,
                                    binderClassName = binder.javaClass.name,
                                    methods = methods,
                                    proxyObjectHandleId = proxyHandleId
                                )
                            )
                        } else {
                            results.add(
                                SystemServiceInfo(
                                    name = name,
                                    interfaceDescriptor = null,
                                    isAlive = false,
                                    isPingable = false,
                                    isProxy = false,
                                    binderClassName = "null",
                                    methods = defaultBinderMethods()
                                )
                            )
                        }
                    } catch (_: Throwable) {
                        results.add(
                            SystemServiceInfo(
                                name = name,
                                interfaceDescriptor = "[Restricted]",
                                isAlive = false,
                                isPingable = false,
                                isProxy = false,
                                binderClassName = "restricted",
                                methods = defaultBinderMethods()
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            // Fallback: query common known system services directly if listServices is blocked by SELinux
            val knownServices = listOf(
                "activity", "package", "window", "power", "connectivity", "display",
                "audio", "vibrator", "notification", "input", "clipboard", "telephony.registry",
                "batteryproperties", "alarm", "appops", "account", "user", "device_policy"
            )
            for (name in knownServices) {
                try {
                    val smClass = Class.forName("android.os.ServiceManager")
                    val getServiceMethod = smClass.getMethod("getService", String::class.java)
                    val binder = getServiceMethod.invoke(null, name) as? IBinder
                    if (binder != null) {
                        val descriptor = try { binder.interfaceDescriptor } catch (_: Throwable) { null }
                        val (methods, proxyInstance) = introspectBinderMethods(binder, descriptor)
                        var proxyHandleId: String? = null
                        if (proxyInstance != null && objectRegistry != null) {
                            proxyHandleId = objectRegistry.registerObject(
                                proxyInstance,
                                "BinderProxy for $name",
                                retainStrong = false
                            ).id
                        }
                        results.add(
                            SystemServiceInfo(
                                name = name,
                                interfaceDescriptor = descriptor,
                                isAlive = binder.isBinderAlive,
                                isPingable = binder.pingBinder(),
                                isProxy = binder.javaClass.name.contains("Proxy"),
                                binderClassName = binder.javaClass.name,
                                methods = methods,
                                proxyObjectHandleId = proxyHandleId
                            )
                        )
                    }
                } catch (_: Throwable) {}
            }
        }

        return results.sortedWith(compareBy({ it.name != "uare.runtime.service" }, { it.name }))
    }

    private fun introspectBinderMethods(binder: IBinder, descriptor: String?): Pair<List<BinderMethodInfo>, Any?> {
        val methods = mutableListOf<BinderMethodInfo>()
        var proxyInstance: Any? = null

        if (descriptor != null) {
            // 1. Try to obtain the AIDL proxy via $Stub.asInterface
            try {
                val stubClass = Class.forName("$descriptor\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                proxyInstance = asInterface.invoke(null, binder)
            } catch (_: Throwable) {}

            // 2. Discover methods on interface class or proxy class
            val ifaceClass = try {
                Class.forName(descriptor)
            } catch (_: Throwable) {
                proxyInstance?.javaClass
            }

            if (ifaceClass != null) {
                val reflectionMethods = ifaceClass.methods.filter { m ->
                    m.declaringClass != Any::class.java &&
                    m.declaringClass != IBinder::class.java &&
                    m.name != "asBinder" &&
                    m.name != "equals" &&
                    m.name != "hashCode" &&
                    m.name != "toString"
                }

                for (m in reflectionMethods) {
                    val pNames = try {
                        m.parameters.map { it.name }
                    } catch (_: Throwable) {
                        m.parameterTypes.indices.map { "arg$it" }
                    }
                    methods.add(
                        BinderMethodInfo(
                            name = m.name,
                            returnType = m.returnType.simpleName,
                            parameterTypes = m.parameterTypes.map { it.simpleName },
                            parameterNames = pNames,
                            signature = "${m.name}(${m.parameterTypes.joinToString(", ") { it.simpleName }}): ${m.returnType.simpleName}"
                        )
                    )
                }
            }
        }

        if (methods.isEmpty()) {
            methods.addAll(defaultBinderMethods())
        }

        return Pair(methods.sortedBy { it.name }, proxyInstance)
    }

    private fun defaultBinderMethods(): List<BinderMethodInfo> {
        return listOf(
            BinderMethodInfo(
                name = "pingBinder",
                returnType = "Boolean",
                parameterTypes = emptyList(),
                parameterNames = emptyList(),
                signature = "pingBinder(): Boolean"
            ),
            BinderMethodInfo(
                name = "isBinderAlive",
                returnType = "Boolean",
                parameterTypes = emptyList(),
                parameterNames = emptyList(),
                signature = "isBinderAlive(): Boolean"
            )
        )
    }
}
