package incident.server.auth

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.authModule(jwtService: JwtService) {
    routing {
        route("/api/auth") {
            authRoute(jwtService)
        }
    }
}
