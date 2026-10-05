package incident.server.plugins

import incident.shared.core.ApiError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        // Exception handlers
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError(cause.message ?: "Invalid request."))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiError(cause.message ?: "Resource not found."))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError(cause.message ?: "Invalid request."))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error processing request", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("An unexpected error occurred."))
        }

        // Status code handlers for body-less responses
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ApiError("Authentication is required to access this resource."))
        }
        status(HttpStatusCode.Forbidden) { call, status ->
            call.respond(status, ApiError("You do not have permission to access this resource."))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiError("Resource not found."))
        }
    }
}
