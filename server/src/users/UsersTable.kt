package incident.server.users

import incident.shared.users.Role
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * Exposed table for [User]. [LongIdTable] adds the auto-incrementing `id` column, so the database
 * generates the ids that the in-memory fake generates with a counter.
 */
object UsersTable : LongIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 100)
    val email = varchar("email", 100)

    // Enums are stored by name: readable in the database and independent of the enum ordering.
    val role = enumerationByName<Role>("role", 20)
    val avatar = varchar("avatar", 255).nullable()
}
