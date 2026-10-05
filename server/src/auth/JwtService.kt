@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package incident.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import incident.server.users.User
import incident.server.users.UserService
import incident.shared.auth.LoginRequest
import incident.shared.core.ApiError
import incident.shared.users.Role
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

@JvmInline
value class AuthRole(val role: Role) : AuthenticationRole {
    override val name: String
        get() = role.name
}

/** JWT authentication scheme extended with role-based authorization on [Role]. */
typealias RoleAuthScheme = AuthenticationSchemeWithRoles<User, AuthRole, Unit, SimpleAuthenticationScheme<User>>

fun Route.authenticateWith(
    roleAuth: RoleAuthScheme,
    roles: Set<Role>,
    build: Route.() -> Unit
): Route = authenticateWith(
    scheme = roleAuth,
    roles = roles.map { AuthRole(it) }.toSet(),
    build = { build() }
)

@Serializable
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)

class JwtService(
    private val jwtConfig: JwtConfig,
    private val userService: UserService
) {
    val jwtRealm: String = jwtConfig.realm

    val jwtVerifier: JWTVerifier =
        JWT
            .require(Algorithm.HMAC256(jwtConfig.secret))
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .build()

    val authScheme: SimpleAuthenticationScheme<User> = jwt<User>("jwt-auth") {
        realm = jwtRealm
        verifier(jwtVerifier)

        validate { credential ->
            customValidator(credential)
        }
    }

    // Role-based authorization on top of the JWT scheme. Ktor requires ALL roles passed to
    // authenticateWith(roles = ...) to be present, so a principal resolves to every role its own role implies.
    val roleAuth: RoleAuthScheme = authScheme.withRoles(
        onForbidden = {
            call.respond(
                HttpStatusCode.Forbidden,
                ApiError("You do not have permission to access this resource.")
            )
        }
    ) { user ->
        user.role.implied.map { AuthRole(it) }.toSet()
    }

    suspend fun authenticate(loginRequest: LoginRequest): String? {
        val foundUser = userService.findByUsername(loginRequest.username)

        return if (foundUser != null && foundUser.password == loginRequest.password) {
            createAccessToken(foundUser)
        } else null
    }

    private fun createAccessToken(foundUser: User): String = JWT
        .create()
        .withAudience(jwtConfig.audience)
        .withIssuer(jwtConfig.issuer)
        .withClaim("id", foundUser.id)
        .withClaim("role", foundUser.role.toString())
        .withExpiresAt(Date(System.currentTimeMillis() + 3600_000))
        .sign(Algorithm.HMAC256(jwtConfig.secret))

    suspend fun customValidator(credential: JWTCredential): User? {
        val id = credential.payload.getClaim("id").asLong()
        return if (audienceMatches(credential) && id != null) {
            userService.findById(id)
        } else null
    }

    private fun audienceMatches(credential: JWTCredential): Boolean =
        credential.payload.audience.contains(jwtConfig.audience)
}
