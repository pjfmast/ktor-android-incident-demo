package incident.server.users

import incident.server.auth.RoleAuthScheme
import incident.server.incidents.IncidentService
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Application.usersModule(
    userService: UserService,
    incidentService: IncidentService,
    roleAuth: RoleAuthScheme,
) {
    // Ensure the upload directory exists at application startup
    val uploadsDir = File("uploads/usersImages")
    if (!uploadsDir.exists()) {
        uploadsDir.mkdirs()
    }

    routing {
        staticFiles(
            remotePath = "/api/users/images",
            dir = File("uploads/usersImages"),
            index = "kodee.png"
        ) {
            default("kodee.png")
        }
        route("/api/users") {
            userRoutes(userService, incidentService, roleAuth)
        }
    }
}
