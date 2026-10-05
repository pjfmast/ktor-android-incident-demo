package incident.server.users

import incident.server.core.BaseInMemoryRepository

/**
 * In-memory [UserRepository], meant for demos and tests only.
 *
 * This is a class (not an object): every application and every test creates its own instance, so
 * there is no global mutable state leaking between tests. Demo data is not created in an `init`
 * block but by the suspending [seedDemoData] extension, which works for any implementation.
 */
class FakeUserRepository : BaseInMemoryRepository<User>(), UserRepository<Long> {
    override val items = mutableListOf<User>()
    override var currentId: Long = User.NEW_USER_ID

    override fun copyWithNewId(entity: User, id: Long): User = entity.copy(id = id)
    override fun getId(entity: User): Long = entity.id

    // UserRepository-specific functionality
    override suspend fun findByUsername(username: String): User? = items.find { it.username == username }

    companion object {
        suspend fun withDemoData(users: List<User> = demoUsers): FakeUserRepository =
            FakeUserRepository().also { it.seedDemoData(users) }
    }
}
