package incident.server

import incident.server.incidents.getImageUploadPath
import incident.shared.core.PaginatedItemResponse
import incident.shared.incidents.IncidentResponse
import incident.shared.users.Role
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IncidentsTest {
    @Test
    fun `list of incidents - happy path`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents") {
            authenticate(Role.ADMIN)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `list of incidents - no access`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `list of incidents - OFFICIAL is allowed through role hierarchy`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents") {
            authenticate(Role.OFFICIAL)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `list of incidents - USER is forbidden`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents") {
            authenticate(Role.USER)
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun `list of incidents paginated - happy path default parameters`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/incidents/paginated") {
            authenticate(Role.OFFICIAL)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val paginated = response.body<PaginatedItemResponse<IncidentResponse>>()
        assertTrue(paginated.data.isNotEmpty())
        assertTrue(paginated.totalCount > 0)
    }

    @Test
    fun `list of incidents paginated - valid custom query parameters`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/incidents/paginated?page=1&pageSize=2") {
            authenticate(Role.OFFICIAL)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val paginated = response.body<PaginatedItemResponse<IncidentResponse>>()
        assertEquals(2, paginated.data.size)
    }

    @Test
    fun `list of incidents paginated - invalid non-numeric parameter returns 400 Bad Request`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents/paginated?page=invalid") {
            authenticate(Role.OFFICIAL)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `list of incidents paginated - non-positive parameter returns 400 Bad Request`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents/paginated?page=-1") {
            authenticate(Role.OFFICIAL)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `my incidents - happy path`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents/my-incidents") {
            authenticate(Role.USER)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `my incidents - no access`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents/my-incidents").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `get incident by id - OFFICIAL receives nested reporter details`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/incidents/1") {
            authenticate(Role.OFFICIAL)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val incident = response.body<IncidentResponse>()
        assertEquals("Anne", incident.reporter?.username)
    }

    @Test
    fun `get incident by id - USER can view own incident with reporter details`() = testApplication {
        application {
            module(testDependencies())
        }
        val client = createJsonClient()

        val response = client.get("/api/incidents/2") {
            authenticate(Role.USER)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val incident = response.body<IncidentResponse>()
        assertEquals("Henk", incident.reporter?.username)
    }

    @Test
    fun `get incident by id - USER cannot view other user's incident (404 Not Found)`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents/1") {
            authenticate(Role.USER)
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `upload image - valid png upload succeeds`() = testApplication {
        application {
            module(testDependencies())
        }

        val testFile = File(getImageUploadPath("incident1-image1.png"))
        try {
            val response = client.submitFormWithBinaryData(
                url = "/api/incidents/1/images",
                formData = formData {
                    append("description", "A photo of the sinkhole")
                    append("image", byteArrayOf(1, 2, 3), Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"sinkhole.png\"")
                    })
                }
            )

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(testFile.exists())
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun `upload image - invalid extension is rejected with 400 Bad Request`() = testApplication {
        application {
            module(testDependencies())
        }

        val response = client.submitFormWithBinaryData(
            url = "/api/incidents/1/images",
            formData = formData {
                append("image", byteArrayOf(1, 2, 3), Headers.build {
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append(HttpHeaders.ContentDisposition, "filename=\"script.exe\"")
                })
            }
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid file type 'exe'"))
    }

    @Test
    fun `upload image - skips existing file on disk and numbers sequentially`() = testApplication {
        application {
            module(testDependencies())
        }

        val file1 = File(getImageUploadPath("incident1-image1.png"))
        val file2 = File(getImageUploadPath("incident1-image2.png"))
        file1.parentFile?.mkdirs()
        file1.writeBytes(byteArrayOf(9, 9, 9))

        try {
            val response = client.submitFormWithBinaryData(
                url = "/api/incidents/1/images",
                formData = formData {
                    append("image", byteArrayOf(1, 2, 3), Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"photo.png\"")
                    })
                }
            )

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(file2.exists())
        } finally {
            file1.delete()
            file2.delete()
        }
    }
}
