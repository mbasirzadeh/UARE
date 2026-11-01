package com.example.uare.binder

import android.os.IBinder

data class SystemServiceInfo(
    val name: String,
    val interfaceDescriptor: String?,
    val isAlive: Boolean,
    val isPingable: Boolean,
    val isProxy: Boolean,
    val binderClassName: String,
    val discoveryTimeMs: Long = System.currentTimeMillis()
)

class BinderExplorer {

    fun discoverServices(): List<SystemServiceInfo> {
        val results = mutableListOf<SystemServiceInfo>()

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
                            val descriptor = try { binder.interfaceDescriptor } catch (t: Throwable) { null }
                            val isAlive = try { binder.isBinderAlive } catch (t: Throwable) { false }
                            val isPingable = try { binder.pingBinder() } catch (t: Throwable) { false }
                            val isProxy = binder.javaClass.name.contains("Proxy")

                            results.add(
                                SystemServiceInfo(
                                    name = name,
                                    interfaceDescriptor = descriptor,
                                    isAlive = isAlive,
                                    isPingable = isPingable,
                                    isProxy = isProxy,
                                    binderClassName = binder.javaClass.name
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
                                    binderClassName = "null"
                                )
                            )
                        }
                    } catch (t: Throwable) {
                        results.add(
                            SystemServiceInfo(
                                name = name,
                                interfaceDescriptor = "[Restricted]",
                                isAlive = false,
                                isPingable = false,
                                isProxy = false,
                                binderClassName = "restricted"
                            )
                        )
                    }
                }
            }
        } catch (e: Throwable) {
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
                        results.add(
                            SystemServiceInfo(
                                name = name,
                                interfaceDescriptor = try { binder.interfaceDescriptor } catch (t: Throwable) { null },
                                isAlive = binder.isBinderAlive,
                                isPingable = binder.pingBinder(),
                                isProxy = binder.javaClass.name.contains("Proxy"),
                                binderClassName = binder.javaClass.name
                            )
                        )
                    }
                } catch (ignored: Throwable) {}
            }
        }

        return results.sortedBy { it.name }
    }
}
