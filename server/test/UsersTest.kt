package incident.server

import incident.shared.users.Role
import incident.shared.users.UpdateUserRequest
import incident.shared.users.UserResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UsersTest {
    @Test
    fun `get me - happy path`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/users/me") {
            authenticate(Role.USER)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<UserResponse>()
        assertEquals("Henk", user.username)
    }

    @Test
    fun `get me - no access`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/users/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `put me - happy path`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.put("/api/users/me") {
            authenticate(Role.USER)
            contentType(ContentType.Application.Json)
            setBody(UpdateUserRequest(username = "updatedUser"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<UserResponse>()
        assertEquals("updatedUser", updated.username)
    }

    @Test
    fun `put me - no access`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.put("/api/users/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserRequest(username = "updatedUser"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `get user by id - ADMIN is allowed`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/users/1") {
            authenticate(Role.ADMIN)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<UserResponse>()
        assertEquals("1", user.id)
    }

    @Test
    fun `get user by id - OFFICIAL is forbidden`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/users/1") {
            authenticate(Role.OFFICIAL)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `get user by id - USER is forbidden`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/users/1") {
            authenticate(Role.USER)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
