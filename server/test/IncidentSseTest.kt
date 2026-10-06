package incident.server

import incident.shared.incidents.Category
import incident.shared.incidents.CreateIncidentRequest
import incident.shared.incidents.Priority
import incident.shared.users.Role
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// TODO: [SSE Stap 8] SSE integratietests uitvoeren via testApplication (autorisatie 401/403 en live events)
class IncidentSseTest {

    @Test
    fun `stream endpoint - unauthenticated is 401 Unauthorized`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents/stream").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `stream endpoint - normal USER is 403 Forbidden`() = testApplication {
        application {
            module(testDependencies())
        }

        client.get("/api/incidents/stream") {
            authenticate(Role.USER)
        }.apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun `creating incident emits stream event to subscribers`() = testApplication {
        val deps = testDependencies()
        application {
            module(deps)
        }

        val emittedEvents = mutableListOf<incident.shared.incidents.IncidentStreamEvent>()
        val job = CoroutineScope(Dispatchers.Default).launch {
            deps.incidentService.events.collect { emittedEvents.add(it) }
        }

        val jsonClient = createJsonClient()
        val createResponse = jsonClient.post("/api/incidents") {
            authenticate(Role.USER)
            contentType(ContentType.Application.Json)
            setBody(
                CreateIncidentRequest(
                    category = Category.COMMUNAL,
                    description = "Live SSE Incident Test Melding",
                    latitude = 52.37,
                    longitude = 4.89,
                    priority = Priority.HIGH
                )
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        withTimeoutOrNull(2.seconds) {
            while (emittedEvents.isEmpty()) {
                delay(50.milliseconds)
            }
        }
        job.cancel()

        assertEquals(1, emittedEvents.size)
        assertEquals("INCIDENT_CREATED", emittedEvents[0].eventType)
        assertEquals("Live SSE Incident Test Melding", emittedEvents[0].incident.description)
    }

    @Test
    fun `changing incident status emits INCIDENT_STATUS_CHANGED to subscribers`() = testApplication {
        val deps = testDependencies()
        application {
            module(deps)
        }

        val emittedEvents = mutableListOf<incident.shared.incidents.IncidentStreamEvent>()
        val job = CoroutineScope(Dispatchers.Default).launch {
            deps.incidentService.events.collect { emittedEvents.add(it) }
        }

        val firstIncident = deps.incidentService.findAll().first()
        val incidentId = firstIncident.id

        val jsonClient = createJsonClient()
        val patchResponse = jsonClient.patch("/api/incidents/$incidentId/status") {
            authenticate(Role.OFFICIAL)
            contentType(ContentType.Application.Json)
            setBody(incident.shared.incidents.ChangeStatusRequest(incident.shared.incidents.Status.ASSIGNED))
        }
        assertEquals(HttpStatusCode.OK, patchResponse.status)

        withTimeoutOrNull(2.seconds) {
            while (emittedEvents.isEmpty()) {
                delay(50.milliseconds)
            }
        }
        job.cancel()

        assertEquals(1, emittedEvents.size)
        assertEquals("INCIDENT_STATUS_CHANGED", emittedEvents[0].eventType)
    }
}
