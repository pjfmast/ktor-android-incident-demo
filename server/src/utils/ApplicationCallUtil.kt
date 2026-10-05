package incident.server.utils

import incident.server.users.User
import incident.shared.users.Role
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun ApplicationCall.userName(): String? = user?.username
fun ApplicationCall.userId(): Long? = user?.id
fun ApplicationCall.userRole(): Role? = user?.role

// Used for ownership checks ("official OR reporter of this incident") that cannot be expressed
// as a route-level role requirement via authenticateWith(roleAuth, roles = ...).
fun RoutingContext.isQualifiedOfficial() =
    call.userRole()?.let { Role.OFFICIAL in it.implied } ?: false

private val ApplicationCall.user get() =
    principal<User>()
