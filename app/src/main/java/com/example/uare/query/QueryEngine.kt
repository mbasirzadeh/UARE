package com.example.uare.query

import com.example.uare.binder.SystemServiceInfo
import com.example.uare.discovery.DiscoveredClass
import com.example.uare.monitor.ThreadInfoModel

data class QueryPredicate(
    val field: String,
    val operator: Operator,
    val value: String
) {
    enum class Operator {
        EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        REGEX
    }

    fun matches(input: String?): Boolean {
        if (input == null) return false
        return when (operator) {
            Operator.EQUALS -> input.equals(value, ignoreCase = true)
            Operator.CONTAINS -> input.contains(value, ignoreCase = true)
            Operator.STARTS_WITH -> input.startsWith(value, ignoreCase = true)
            Operator.ENDS_WITH -> input.endsWith(value, ignoreCase = true)
            Operator.REGEX -> try { Regex(value).containsMatchIn(input) } catch (t: Throwable) { false }
        }
    }
}

data class QueryResult(
    val queryType: String,
    val executionTimeMs: Long,
    val totalFound: Int,
    val matchedClassNames: List<String> = emptyList(),
    val matchedServiceNames: List<String> = emptyList(),
    val matchedThreadNames: List<String> = emptyList()
)

class QueryEngine {

    fun queryClasses(classes: List<DiscoveredClass>, predicates: List<QueryPredicate>): QueryResult {
        val start = System.currentTimeMillis()
        val filtered = classes.filter { clazz ->
            predicates.all { p ->
                when (p.field.lowercase()) {
                    "name" -> p.matches(clazz.name)
                    "package" -> p.matches(clazz.packageName)
                    "superclass" -> p.matches(clazz.superclassName)
                    "loader" -> p.matches(clazz.classLoaderName)
                    else -> p.matches(clazz.name)
                }
            }
        }
        return QueryResult(
            queryType = "Class Query (${predicates.size} predicates)",
            executionTimeMs = System.currentTimeMillis() - start,
            totalFound = filtered.size,
            matchedClassNames = filtered.map { it.name }
        )
    }

    fun queryServices(services: List<SystemServiceInfo>, predicates: List<QueryPredicate>): QueryResult {
        val start = System.currentTimeMillis()
        val filtered = services.filter { svc ->
            predicates.all { p ->
                when (p.field.lowercase()) {
                    "name" -> p.matches(svc.name)
                    "descriptor" -> p.matches(svc.interfaceDescriptor)
                    "class" -> p.matches(svc.binderClassName)
                    else -> p.matches(svc.name)
                }
            }
        }
        return QueryResult(
            queryType = "Service Query (${predicates.size} predicates)",
            executionTimeMs = System.currentTimeMillis() - start,
            totalFound = filtered.size,
            matchedServiceNames = filtered.map { it.name }
        )
    }

    fun queryThreads(threads: List<ThreadInfoModel>, predicates: List<QueryPredicate>): QueryResult {
        val start = System.currentTimeMillis()
        val filtered = threads.filter { th ->
            predicates.all { p ->
                when (p.field.lowercase()) {
                    "name" -> p.matches(th.name)
                    "state" -> p.matches(th.state.name)
                    "group" -> p.matches(th.threadGroupName)
                    else -> p.matches(th.name)
                }
            }
        }
        return QueryResult(
            queryType = "Thread Query (${predicates.size} predicates)",
            executionTimeMs = System.currentTimeMillis() - start,
            totalFound = filtered.size,
            matchedThreadNames = filtered.map { it.name }
        )
    }
}
