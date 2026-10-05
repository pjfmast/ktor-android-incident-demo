package incident.server

import incident.server.auth.JwtService
import incident.server.users.FakeUserRepository
import incident.server.users.UserService
import incident.shared.auth.LoginRequest
import incident.shared.users.Role
import io.ktor.client.request.*

suspend fun HttpRequestBuilder.authenticate(role: Role) {
    // A separate, identically seeded fake: the token only depends on the demo data and the secret,
    // which testJwtConfig shares with the test application.
    val userService = UserService(FakeUserRepository.withDemoData())
    val jwtService = JwtService(testJwtConfig, userService)
    val user = userService.findAll().find { it.role == role }
        ?: throw AssertionError("No user in repository for role: ${role.name}")
    val token = jwtService.authenticate(LoginRequest(user.username, user.password))
        ?: throw AssertionError("Failed to authenticate: ${user.username}")
    bearerAuth(token)
}
