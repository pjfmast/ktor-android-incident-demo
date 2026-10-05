package incident.server.users

import incident.server.core.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

/**
 * Persistent [UserRepository] on top of Exposed (DSL), see [UsersTable].
 *
 * Every function runs in its own transaction ([dbQuery]), except [saveAll], which uses a single
 * transaction so that seeding is all-or-nothing.
 */
class ExposedUserRepository : UserRepository<Long> {

    override suspend fun findAll(): List<User> = dbQuery {
        UsersTable.selectAll()
            .orderBy(UsersTable.id)
            .map { it.toUser() }
    }

    override suspend fun findAllPaginated(page: Int, pageSize: Int): Pair<List<User>, Long> {
        require(page > 0) { "Page number must be positive" }
        require(pageSize > 0) { "Page size must be positive" }

        return dbQuery {
            val totalSize = UsersTable.selectAll().count()
            val users = UsersTable.selectAll()
                .orderBy(UsersTable.id)
                .limit(pageSize)
                .offset((page - 1).toLong() * pageSize)
                .map { it.toUser() }

            users to totalSize
        }
    }

    override suspend fun findById(id: Long): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun save(entity: User): User = dbQuery { saveInTransaction(entity) }

    override suspend fun saveAll(entities: Iterable<User>): List<User> = dbQuery {
        entities.map { saveInTransaction(it) }
    }

    override suspend fun delete(id: Long): Boolean = dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    // UserRepository-specific functionality
    override suspend fun findByUsername(username: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.username eq username }
            .singleOrNull()
            ?.toUser()
    }

    /**
     * Insert or update, without starting a transaction of its own, so callers decide how many
     * users are saved together. Must be called from within a transaction.
     */
    private fun saveInTransaction(user: User): User {
        return if (user.id == User.NEW_USER_ID) insert(user) else upsert(user)
    }

    private fun insert(user: User): User {
        val generatedId = UsersTable.insertAndGetId { row ->
            row.write(user)
        }
        return user.copy(id = generatedId.value)
    }

    private fun upsert(user: User): User {
        UsersTable.upsert(UsersTable.id) { row ->
            row[UsersTable.id] = user.id
            row.write(user)
        }
        return user
    }
}

/**
 * Writes all columns except the generated `id`; shared by insert and update.
 */
private fun UpdateBuilder<*>.write(user: User) {
    this[UsersTable.username] = user.username
    this[UsersTable.password] = user.password
    this[UsersTable.email] = user.email
    this[UsersTable.role] = user.role
    this[UsersTable.avatar] = user.avatar
}

/**
 * Maps a database row back to the domain object.
 */
private fun ResultRow.toUser(): User = User(
    username = this[UsersTable.username],
    password = this[UsersTable.password],
    email = this[UsersTable.email],
    role = this[UsersTable.role],
    avatar = this[UsersTable.avatar],
    id = this[UsersTable.id].value
)
