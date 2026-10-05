package incident.server.users

class UserService(
    private val userRepository: UserRepository<Long>
) {
    suspend fun findAll(): List<User> =
        userRepository.findAll()

    suspend fun findById(id: Long): User? =
        userRepository.findById(id)

    suspend fun findByUsername(username: String): User? =
        userRepository.findByUsername(username)

    suspend fun save(user: User): User =
        userRepository.save(user)

    suspend fun delete(userId: Long): Boolean =
        userRepository.delete(userId)
}
