package incident.server

import incident.server.auth.JwtConfig
import incident.server.incidents.FakeIncidentRepository
import incident.server.users.FakeUserRepository
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking

/**
 * Single JWT configuration shared by the test application and the test client helpers.
 */
val testJwtConfig = JwtConfig(
    secret = "my secret",
    issuer = "http://localhost",
    audience = "incident-demo",
    realm = "my realm"
)

/**
 * Creates isolated dependencies for testApplication executions.
 */
fun testDependencies(
    jwtConfig: JwtConfig = testJwtConfig,
    userRepository: FakeUserRepository = runBlocking { FakeUserRepository.withDemoData() },
    incidentRepository: FakeIncidentRepository = runBlocking { FakeIncidentRepository.withDemoData(userRepository) }
): Dependencies = dependencies(jwtConfig, userRepository, incidentRepository)


/**
 * Creates an HttpClient configured with JSON content negotiation for typed testing.
 */
fun ApplicationTestBuilder.createJsonClient(): HttpClient = createClient {
    install(ContentNegotiation) {
        json()
    }
}
