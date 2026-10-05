package incident.server.incidents

import incident.server.auth.RoleAuthScheme
import incident.server.users.UserService
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

const val INCIDENT_IMAGES_DIR = "uploads/incidentsImages"

fun getImageUploadPath(imageFile: String): String = "$INCIDENT_IMAGES_DIR/$imageFile"

fun Application.incidentsModule(
    incidentService: IncidentService,
    userService: UserService,
    roleAuth: RoleAuthScheme
) {

    // Ensure the upload directory exists at application startup
    val uploadsDir = File(INCIDENT_IMAGES_DIR)
    if (!uploadsDir.exists()) {
        uploadsDir.mkdirs()
    }

    routing {
        staticFiles(
            remotePath = "/api/incidents/images",
            dir = uploadsDir,
            index = "incident.png"
        ) {
            default("incident.png")
        }
        route("/api/incidents") {
            incidentRoutes(incidentService, userService, roleAuth)
        }
    }
}
