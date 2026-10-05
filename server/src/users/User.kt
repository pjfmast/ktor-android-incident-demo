package incident.server.users

import incident.shared.users.Role

data class User(
    val username: String,
    val password: String,
    val email: String,
    val avatar: String? = null,
    val role: Role = Role.USER,
    val id: Long = NEW_USER_ID,
) {
    companion object {
        const val NEW_USER_ID = 0L
    }
}
