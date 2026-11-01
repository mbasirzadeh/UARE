package com.example.uare.sample

enum class UserRole {
    VIEWER,
    DEVELOPER,
    OPERATOR,
    ADMIN
}

class SampleUserProfile(
    var username: String = "Alex Rivers",
    var email: String = "alex@enterprise.internal",
    var age: Int = 32,
    var credits: Double = 1250.0,
    var isVerified: Boolean = true,
    var role: UserRole = UserRole.ADMIN
) {
    fun getSummary(): String {
        return "User: $username ($email), Age: $age, Role: $role, Credits: $credits, Verified: $isVerified"
    }

    fun upgradeRole(newRole: UserRole): String {
        this.role = newRole
        return "Role updated to $role"
    }

    fun addCredits(amount: Double): Double {
        this.credits += amount
        return this.credits
    }

    override fun toString(): String = getSummary()
}

class SampleRepository(
    var endpointUrl: String = "https://api.internal.runtime.local/v1",
    var cacheTtlSeconds: Long = 3600L,
    var maxRetries: Int = 3
) {
    fun fetchStatus(): String {
        return "Repository active at $endpointUrl (TTL: ${cacheTtlSeconds}s, Retries: $maxRetries)"
    }

    fun ping(customUrl: String): String {
        return "Pinged $customUrl successfully in 14ms (HTTP 200 OK)"
    }

    override fun toString(): String = fetchStatus()
}

class SampleUserManager(
    var managerId: String = "mgr_alpha_01",
    var activeSessionCount: Int = 4,
    var currentUser: SampleUserProfile = SampleUserProfile(),
    var repository: SampleRepository = SampleRepository()
) {
    fun createProfile(name: String, email: String, age: Int): SampleUserProfile {
        val newProfile = SampleUserProfile(
            username = name,
            email = email,
            age = age,
            credits = 500.0,
            isVerified = false,
            role = UserRole.DEVELOPER
        )
        return newProfile
    }

    fun updateUserCredits(addedCredits: Double): Double {
        return currentUser.addCredits(addedCredits)
    }

    fun verifyCurrentUser(verified: Boolean): String {
        currentUser.isVerified = verified
        return "User ${currentUser.username} verification set to $verified"
    }

    fun pingEndpoint(endpoint: String): String {
        return repository.ping(endpoint)
    }

    fun resetSession(): String {
        currentUser = SampleUserProfile()
        activeSessionCount = 1
        return "Session reset to default user"
    }

    override fun toString(): String {
        return "SampleUserManager(id=$managerId, activeSessions=$activeSessionCount, user=${currentUser.username})"
    }
}
