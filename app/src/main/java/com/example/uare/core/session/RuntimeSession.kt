package com.example.uare.core.session

import com.example.uare.api.RuntimeEnvironment
import com.example.uare.api.RuntimeType
import java.util.UUID

/**
 * Representation of an active execution agent (Application Process vs Platform/system_server).
 */
sealed interface ProcessAgent {
    val agentId: String
    val processName: String
    val isPlatformIntegration: Boolean

    data class ApplicationAgent(
        override val agentId: String = "agent_app_${UUID.randomUUID().toString().take(8)}",
        override val processName: String
    ) : ProcessAgent {
        override val isPlatformIntegration: Boolean = false
    }

    data class PlatformAgent(
        override val agentId: String = "agent_platform_${UUID.randomUUID().toString().take(8)}",
        override val processName: String = "system_server"
    ) : ProcessAgent {
        override val isPlatformIntegration: Boolean = true
    }
}

/**
 * Session tracking active exploration, agent context, and operation logs.
 */
data class RuntimeSession(
    val sessionId: String = "session_${UUID.randomUUID().toString().take(8)}",
    val startTimeMs: Long = System.currentTimeMillis(),
    val environment: RuntimeEnvironment,
    val agent: ProcessAgent,
    val operationLog: MutableList<OperationLogEntry> = mutableListOf()
) {
    fun log(action: String, target: String, status: String, details: String = "") {
        operationLog.add(
            OperationLogEntry(
                timestamp = System.currentTimeMillis(),
                action = action,
                target = target,
                status = status,
                details = details
            )
        )
    }
}

data class OperationLogEntry(
    val timestamp: Long,
    val action: String,
    val target: String,
    val status: String,
    val details: String
)

object SessionManager {
    private var currentSession: RuntimeSession? = null

    fun initializeSession(environment: RuntimeEnvironment): RuntimeSession {
        val agent = if (environment.runtimeType == RuntimeType.SYSTEM_SERVER) {
            ProcessAgent.PlatformAgent(processName = environment.processName ?: "system_server")
        } else {
            ProcessAgent.ApplicationAgent(processName = environment.processName ?: "app_process")
        }

        val session = RuntimeSession(
            environment = environment,
            agent = agent
        )
        currentSession = session
        return session
    }

    fun getSession(): RuntimeSession? = currentSession
}
