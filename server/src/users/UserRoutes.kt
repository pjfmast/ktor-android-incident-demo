package incident.server.users

import incident.server.auth.RoleAuthScheme
import incident.server.auth.authenticateWith
import incident.server.incidents.Incident
import incident.server.incidents.IncidentService
import incident.server.incidents.toResponse
import incident.server.utils.isQualifiedOfficial
import incident.server.utils.userId
import incident.shared.users.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Route.userRoutes(
    userService: UserService,
    incidentService: IncidentService,
    roleAuth: RoleAuthScheme
) {
    // anyone can register as a user:
    route("/register") {
        post {
            val createUserRequest = call.receive<CreateUserRequest>()
            val createdUser = userService.save(
                user = createUserRequest.toModel()
            )
            val userResponse = createdUser.toResponse()
            call.respond(HttpStatusCode.Created, userResponse)
        }
    }

    // User requests for ADMIN only (Ktor responds with 403 Forbidden for other roles)
    authenticateWith(roleAuth, roles = setOf(Role.ADMIN)) {
        // Get all Users
        get {
            val users = userService.findAll()
            call.respond(users.map(User::toResponse))
        }

        // Get a specific user by ID (ADMIN only)
        get("/{id}") {
            val id: Long by call.pathParameters

            val foundUser = userService.findById(id)
                ?: throw NotFoundException("User with id $id not found")

            call.respond(foundUser.toResponse())
        }

        put("/{id}/role") {
            val id: Long by call.pathParameters

            val roleRequest = call.receive<UpdateRoleRequest>()
            val user = userService.findById(id)
                ?: throw NotFoundException("User with id $id not found")

            val updatedUser = user.copy(role = roleRequest.role)
            val savedUser = userService.save(updatedUser)
            call.respond(savedUser.toResponse())
        }

        delete("/{id}") {
            val id: Long by call.pathParameters

            val deleted = userService.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                throw NotFoundException("User with id $id not found")
            }
        }
    }

    // requests available for authenticated users:
    authenticateWith(roleAuth) {
        // any authenticated user can retrieve the current user information
        get("/me") {
            // Get the current authenticated user directly from the typed principal
            val currentUser: User = call.principal

            // Return the user information (excluding sensitive data like password)
            call.respond(currentUser.toResponse())
        }

        // Allow users to update their own details
        put("/me") {
            // Get the current authenticated user directly from the typed principal
            val principalUser: User = call.principal

            // Get the update request
            val updateRequest = call.receive<UpdateUserRequest>()
            val currentUser = userService.findById(principalUser.id)
                ?: throw NotFoundException("User not found")

            val updatedUser = currentUser.copy(
                username = updateRequest.username ?: currentUser.username,
                // Only update password if provided and not empty
                password = updateRequest.password?.takeIf { it.isNotBlank() } ?: currentUser.password,
                email = updateRequest.email ?: currentUser.email,
                // Don't allow users to change their own role
                avatar = updateRequest.avatar ?: currentUser.avatar
            )

            // Save the updated user
            val savedUser = userService.save(updatedUser)

            // Return the updated user
            call.respond(savedUser.toResponse())
        }

        // any qualified official can retrieve incidents reported by a user,
        // other users can only retrieve their own reported incidents.
        get("/{id}/incidents") {
            val id: Long by call.pathParameters

            val foundUser = userService.findById(id)
                ?: throw NotFoundException("User with id $id not found")

            if (!isQualifiedOfficial() && foundUser.id != call.userId())
                throw NotFoundException()

            val foundIncidentsOfUser = incidentService.findIncidentsReportedByUser(foundUser.id)
            call.respond(foundIncidentsOfUser.map(Incident::toResponse))
        }
    }
}

fun User.toResponse(): UserResponse =
    UserResponse(
        username = this.username,
        email = this.email,
        role = this.role,
        avatar = this.avatar ?: "kodee.png",
        id = this.id.toString(),
    )

fun CreateUserRequest.toModel(): User =
    User(
        id = User.NEW_USER_ID,
        username = this.username,
        password = this.password,
        email = this.email,
        avatar = this.avatar,
        role = Role.USER,
    )
