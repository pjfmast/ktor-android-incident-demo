package incident.server

import incident.server.auth.JwtConfig
import incident.server.auth.authModule
import incident.server.core.DatabaseFactory
import incident.server.incidents.*
import incident.server.plugins.configureStatusPages
import incident.server.users.ExposedUserRepository
import incident.server.users.UsersTable
import incident.server.users.seedDemoData
import incident.server.users.usersModule
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.minutes

@Serializable
data class ServerConfig(
    val port: Int = 8080,
    val host: String = "0.0.0.0"
)

@Serializable
data class AppConfig(
    val server: ServerConfig = ServerConfig(),
    val jwt: JwtConfig
)

fun main() {
    val appConfig = ApplicationConfig("application.conf").getAs<AppConfig>()

    DatabaseFactory.init(listOf(UsersTable, IncidentsTable, IncidentImagesTable))

    val userRepository = ExposedUserRepository()
    val incidentRepository = ExposedIncidentRepository()
    runBlocking {
        userRepository.seedDemoData()
        incidentRepository.seedDemoData(userRepository)
    }

    val dependencies = dependencies(appConfig.jwt, userRepository, incidentRepository)

    embeddedServer(Netty, port = appConfig.server.port, host = appConfig.server.host) {
        module(dependencies)
    }.start(wait = true)
}

fun Application.module(dependencies: Dependencies) {
    // Configure error handling (e.g., custom error pages)
    configureStatusPages()

    // Automatically respond to HEAD requests for all defined GET routes
    install(AutoHeadResponse)

    // Log HTTP requests for observability during local development and testing
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/static") }
    }

    // Rate limiting to protect sensitive endpoints (e.g., brute-force protection on /login)
    install(RateLimit) {
        register(RateLimitName("login")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(SSE)

    // Install route modules with explicit dependencies (no DI container)
    authModule(dependencies.jwtService)
    incidentsModule(dependencies.incidentService, dependencies.userService, dependencies.roleAuth)
    usersModule(dependencies.userService, dependencies.incidentService, dependencies.roleAuth)

    routing {
        get("/") {
            call.respondText("Incident API is running")
        }
    }
}
