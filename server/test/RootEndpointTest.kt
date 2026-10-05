package incident.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RootEndpointTest {

    @Test
    fun `root endpoint returns running message`() = testApplication {
        application {
            module(testDependencies())
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Incident API is running", response.bodyAsText())
    }

    @Test
    fun `root endpoint supports HEAD request via AutoHeadResponse`() = testApplication {
        application {
            module(testDependencies())
        }

        val response = client.head("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("", response.bodyAsText())
    }
}
