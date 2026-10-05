package incident.shared.users

import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    USER,
    OFFICIAL,
    ADMIN;

    /** All roles this role implies, including itself. */
    val implied: Set<Role>
        get() = entries.filter { it.ordinal <= ordinal }.toSet()
}
