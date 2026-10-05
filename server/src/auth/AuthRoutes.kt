package incident.server.auth

import incident.shared.auth.LoginRequest
import incident.shared.auth.TokenResponse
import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoute(jwtService: JwtService) {
    rateLimit(RateLimitName("login")) {
        post("/login") {
            // note: call.receive<> consumes the request body so cannot be called twice.
            val loginRequest = call.receive<LoginRequest>()

            val token = jwtService.authenticate(loginRequest)

            token?.let {
                call.respond(TokenResponse(it))
            } ?: call.respond(HttpStatusCode.Unauthorized)
        }
    }
}
