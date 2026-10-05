package incident.server.users

import incident.server.core.CrudRepository

interface UserRepository<ID>: CrudRepository<User, ID> {
    suspend fun findByUsername(username: String): User?
}
