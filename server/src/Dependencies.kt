package incident.server

import incident.server.auth.JwtConfig
import incident.server.auth.JwtService
import incident.server.auth.RoleAuthScheme
import incident.server.incidents.ExposedIncidentRepository
import incident.server.incidents.IncidentRepository
import incident.server.incidents.IncidentService
import incident.server.users.ExposedUserRepository
import incident.server.users.UserRepository
import incident.server.users.UserService

data class Dependencies(
    val userService: UserService,
    val incidentService: IncidentService,
    val jwtService: JwtService
) {
    val roleAuth: RoleAuthScheme get() = jwtService.roleAuth
}

fun dependencies(
    jwtConfig: JwtConfig,
    userRepository: UserRepository<Long> = ExposedUserRepository(),
    incidentRepository: IncidentRepository<Long> = ExposedIncidentRepository()
): Dependencies {
    val userService = UserService(userRepository)
    val incidentService = IncidentService(incidentRepository)
    val jwtService = JwtService(jwtConfig, userService)
    return Dependencies(userService, incidentService, jwtService)
}
