package com.example.uare.inspection

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Session-scoped handle to an inspected runtime object.
 */
data class ObjectHandle(
    val id: String,
    val className: String,
    val identityHash: Int,
    val registeredAt: Long = System.currentTimeMillis(),
    val description: String = ""
)

/**
 * Object Registry storing references safely to prevent memory leaks while providing stable session IDs.
 */
class ObjectRegistry {

    private val idCounter = AtomicLong(1)
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    private val strongReferences = ConcurrentHashMap<String, Any>() // for explicitly retained anchors
    private val handles = ConcurrentHashMap<String, ObjectHandle>()

    fun registerObject(instance: Any, description: String = "", retainStrong: Boolean = false): ObjectHandle {
        val id = "obj_${idCounter.getAndIncrement()}_${instance.javaClass.simpleName}"
        val handle = ObjectHandle(
            id = id,
            className = instance.javaClass.name,
            identityHash = System.identityHashCode(instance),
            description = description
        )

        weakReferences[id] = WeakReference(instance)
        if (retainStrong) {
            strongReferences[id] = instance
        }
        handles[id] = handle
        return handle
    }

    fun getObject(id: String): Any? {
        val strong = strongReferences[id]
        if (strong != null) return strong
        val weak = weakReferences[id]?.get()
        if (weak == null && weakReferences.containsKey(id)) {
            // Object was garbage collected
            handles.remove(id)
            weakReferences.remove(id)
        }
        return weak
    }

    fun getHandle(id: String): ObjectHandle? = handles[id]

    fun getAllHandles(): List<ObjectHandle> {
        cleanCollected()
        return handles.values.toList()
    }

    fun unregister(id: String) {
        weakReferences.remove(id)
        strongReferences.remove(id)
        handles.remove(id)
    }

    fun clear() {
        weakReferences.clear()
        strongReferences.clear()
        handles.clear()
    }

    private fun cleanCollected() {
        val iterator = weakReferences.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null && !strongReferences.containsKey(entry.key)) {
                handles.remove(entry.key)
                iterator.remove()
            }
        }
    }
}
