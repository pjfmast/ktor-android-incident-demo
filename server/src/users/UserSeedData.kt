package incident.server.users

import incident.shared.users.Role

/**
 * Demo users, expressed as plain domain objects: they carry [User.NEW_USER_ID],
 * so every repository implementation is free to generate its own ids.
 */
val demoUsers: List<User> = listOf(
    User(
        username = "admin",
        password = "password",
        email = "admin@avans.nl",
        role = Role.ADMIN
    ),
    User(
        username = "Henk",
        password = "pwd",
        email = "henk@heusdenhout.nl",
        role = Role.USER
    ),
    User(
        username = "Anne",
        password = "pwd",
        email = "anne@brabantpark.nl",
        role = Role.USER
    ),
    User(
        username = "Kees",
        password = "pwd",
        email = "kees@boeimeer.nl",
        role = Role.USER
    ),
    User(
        username = "Sophie",
        password = "pwd",
        email = "sophie@gemeentebreda.nl",
        role = Role.OFFICIAL
    ),
    User(
        username = "Ron",
        password = "pwd",
        email = "ron@gemeentebreda.nl",
        role = Role.OFFICIAL
    ),
    User(
        username = "Bram",
        password = "pwd",
        email = "bram@ginneken.nl",
        role = Role.USER
    ),
    User(
        username = "Fatima",
        password = "pwd",
        email = "fatima@princenhage.nl",
        role = Role.USER
    ),
    User(
        username = "Lotte",
        password = "pwd",
        email = "lotte@wierickerschans.nl",
        role = Role.USER
    )
)

/**
 * Seeds demo data through the [UserRepository] interface, so the very same function works for the
 * in-memory fake and for a persistent (Exposed) implementation.
 *
 * Schema creation, transaction handling and id generation are deliberately left to the
 * implementation. Seeding is idempotent: an already populated repository is left untouched.
 */
suspend fun UserRepository<Long>.seedDemoData(users: List<User> = demoUsers): List<User> {
    if (findAll().isNotEmpty()) return emptyList()
    return saveAll(users)
}
