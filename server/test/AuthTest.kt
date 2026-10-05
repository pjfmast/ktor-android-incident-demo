package incident.server

import incident.shared.auth.LoginRequest
import incident.shared.auth.TokenResponse
import incident.shared.core.ApiError
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthTest {
    @Test
    fun `login bad password`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("Henk", "pwd0"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val error = response.body<ApiError>()
        assertEquals("Authentication is required to access this resource.", error.message)
    }

    @Test
    fun `login happy path`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("Henk", "pwd"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val tokenResponse = response.body<TokenResponse>()
        assertNotNull(tokenResponse.token)
    }

    @Test
    fun `login endpoint is rate limited after 10 attempts`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        repeat(10) {
            client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest("Henk", "wrong"))
            }
        }

        val rateLimitedResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("Henk", "wrong"))
        }

        assertEquals(HttpStatusCode.TooManyRequests, rateLimitedResponse.status)
    }
}
