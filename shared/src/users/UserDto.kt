package incident.shared.users

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val email: String,
    val avatar: String? = null
)

@Serializable
data class UpdateUserRequest(
    val username: String? = null,
    val password: String? = null,
    val email: String? = null,
    val avatar: String? = null
)

@Serializable
data class UpdateRoleRequest(
    val role: Role
)

@Serializable
data class UserResponse(
    val username: String,
    val email: String,
    val role: Role,
    val avatar: String = "kodee.png",
    val id: String
)
