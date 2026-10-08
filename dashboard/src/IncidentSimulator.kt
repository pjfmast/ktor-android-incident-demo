package incident.dashboard

import incident.shared.auth.LoginRequest
import incident.shared.auth.TokenResponse
import incident.shared.incidents.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun log(icon: String, message: String) {
    val timestamp = LocalTime.now().format(timeFormatter)
    println("[$timestamp] $icon $message")
}

private suspend fun waitRandomDelay(minMs: Long = 500L, maxMs: Long = 3000L) {
    val waitTime = Random.nextLong(minMs, maxMs + 1)
    log("⏱️", "Wachten ${(waitTime / 1000.0).format(2)}s voor volgende simulatiestap...")
    delay(waitTime.milliseconds)
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

suspend fun main(args: Array<String>) {
    val baseUrl = args.getOrNull(0)?.takeIf { it.startsWith("http") } ?: "http://localhost:8080"
    val username = args.getOrNull(1) ?: "Sophie"
    val password = args.getOrNull(2) ?: "pwd"
    val continuousMode = args.contains("--continuous") || args.contains("-c")

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    println("==================================================================")
    println(" 🚓 INCIDENT SIMULATOR (OFFICIAL SCENARIO RUNNER) ")
    println(" Server: $baseUrl | Gebruiker: $username (OFFICIAL)")
    println("==================================================================")

    try {
        // 1. Inloggen als OFFICIAL
        log("🔑", "Aanmelden bij incident server als '$username'...")
        val loginResponse = client.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = username, password = password))
        }

        if (loginResponse.status != HttpStatusCode.OK) {
            log("❌", "Inloggen mislukt: HTTP ${loginResponse.status}")
            return
        }

        val token = loginResponse.body<TokenResponse>().token
        log("✅", "Succesvol aangemeld. Bearer token ontvangen.")

        val authHeaders: HttpRequestBuilder.() -> Unit = {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
        }

        // 2. Bestaande incidenten ophalen
        waitRandomDelay(500, 1500)
        log("📋", "Bestaande incidenten ophalen via GET /api/incidents...")
        val incidentsResponse = client.get("$baseUrl/api/incidents") { authHeaders() }
        val existingIncidents = if (incidentsResponse.status == HttpStatusCode.OK) {
            incidentsResponse.body<List<IncidentResponse>>()
        } else {
            emptyList()
        }
        log("ℹ️", "${existingIncidents.size} bestaande incidenten gevonden in database.")

        // Lijst van dynamisch aangemaakte incident IDs tijdens deze sessie
        val createdIncidentIds = mutableListOf<Long>()

        // 3. Scenario stappen uitvoeren
        val scenarioEvents = listOf(
            // Stap A: Nieuw incident - Verkeerslichten defect
            suspend {
                log("🚨", "Melding aanmaken: 'Verkeerslichten kruispunt defect' (HIGH, TRAFFIC)...")
                val response = client.post("$baseUrl/api/incidents") {
                    authHeaders()
                    setBody(
                        CreateIncidentRequest(
                            category = Category.TRAFFIC,
                            description = "Verkeerslichten kruispunt Ringbaan-West defect en knipperen oranje",
                            latitude = 51.5606,
                            longitude = 5.0628,
                            priority = Priority.HIGH
                        )
                    )
                }
                if (response.status == HttpStatusCode.Created) {
                    val created = response.body<IncidentResponse>()
                    createdIncidentIds.add(created.id)
                    log("✅", "Incident #${created.id} aangemaakt: '${created.description}' (Status: ${created.status})")
                } else {
                    log("⚠️", "Aanmaken incident mislukt: HTTP ${response.status}")
                }
            },

            // Stap B: Nieuw incident - Omgewaaide boom
            suspend {
                log("🚨", "Melding aanmaken: 'Omgewaaide boom blokkeert fietspad' (NORMAL, ENVIRONMENT)...")
                val response = client.post("$baseUrl/api/incidents") {
                    authHeaders()
                    setBody(
                        CreateIncidentRequest(
                            category = Category.ENVIRONMENT,
                            description = "Omgewaaide boom blokkeert fietspad langs Wilhelminakanaal",
                            latitude = 51.5721,
                            longitude = 5.0912,
                            priority = Priority.NORMAL
                        )
                    )
                }
                if (response.status == HttpStatusCode.Created) {
                    val created = response.body<IncidentResponse>()
                    createdIncidentIds.add(created.id)
                    log("✅", "Incident #${created.id} aangemaakt: '${created.description}' (Status: ${created.status})")
                } else {
                    log("⚠️", "Aanmaken incident mislukt: HTTP ${response.status}")
                }
            },

            // Stap C: Prioriteit verhogen van incident 1 naar CRITICAL
            suspend {
                val targetId = createdIncidentIds.firstOrNull() ?: existingIncidents.firstOrNull()?.id
                if (targetId != null) {
                    log("⚡", "Prioriteit opschalen naar CRITICAL voor incident #$targetId...")
                    val response = client.patch("$baseUrl/api/incidents/$targetId/priority") {
                        authHeaders()
                        setBody(ChangePriorityRequest(priority = Priority.CRITICAL))
                    }
                    if (response.status == HttpStatusCode.OK) {
                        val updated = response.body<IncidentResponse>()
                        log("✅", "Incident #$targetId prioriteit gewijzigd naar '${updated.priority}'")
                    } else {
                        log("⚠️", "Prioriteit wijzigen mislukt: HTTP ${response.status}")
                    }
                }
            },

            // Stap D: Status wijzigen naar ASSIGNED voor incident 1
            suspend {
                val targetId = createdIncidentIds.firstOrNull() ?: existingIncidents.firstOrNull()?.id
                if (targetId != null) {
                    log("🔄", "Incident #$targetId toewijzen aan hulpdienst (Status -> ASSIGNED)...")
                    val response = client.patch("$baseUrl/api/incidents/$targetId/status") {
                        authHeaders()
                        setBody(ChangeStatusRequest(status = Status.ASSIGNED))
                    }
                    if (response.status == HttpStatusCode.OK) {
                        val updated = response.body<IncidentResponse>()
                        log("✅", "Incident #$targetId status gewijzigd naar '${updated.status}'")
                    } else {
                        log("⚠️", "Status wijzigen mislukt: HTTP ${response.status}")
                    }
                }
            },

            // Stap E: Nieuw incident - Wateroverlast
            suspend {
                log("🚨", "Melding aanmaken: 'Waterleidingbreuk op de Markt' (CRITICAL, COMMUNAL)...")
                val response = client.post("$baseUrl/api/incidents") {
                    authHeaders()
                    setBody(
                        CreateIncidentRequest(
                            category = Category.COMMUNAL,
                            description = "Grote waterleidingbreuk op de Markt, straat loopt onder",
                            latitude = 51.5555,
                            longitude = 5.0850,
                            priority = Priority.CRITICAL
                        )
                    )
                }
                if (response.status == HttpStatusCode.Created) {
                    val created = response.body<IncidentResponse>()
                    createdIncidentIds.add(created.id)
                    log("✅", "Incident #${created.id} aangemaakt: '${created.description}' (Status: ${created.status})")
                } else {
                    log("⚠️", "Aanmaken incident mislukt: HTTP ${response.status}")
                }
            },

            // Stap F: Status wijzigen van tweede incident naar ASSIGNED
            suspend {
                val targetId = if (createdIncidentIds.size >= 2) createdIncidentIds[1] else existingIncidents.getOrNull(1)?.id
                if (targetId != null) {
                    log("🔄", "Incident #$targetId toewijzen aan groenvoorziening (Status -> ASSIGNED)...")
                    val response = client.patch("$baseUrl/api/incidents/$targetId/status") {
                        authHeaders()
                        setBody(ChangeStatusRequest(status = Status.ASSIGNED))
                    }
                    if (response.status == HttpStatusCode.OK) {
                        val updated = response.body<IncidentResponse>()
                        log("✅", "Incident #$targetId status gewijzigd naar '${updated.status}'")
                    } else {
                        log("⚠️", "Status wijzigen mislukt: HTTP ${response.status}")
                    }
                }
            },

            // Stap G: Status van eerste incident naar RESOLVED
            suspend {
                val targetId = createdIncidentIds.firstOrNull() ?: existingIncidents.firstOrNull()?.id
                if (targetId != null) {
                    log("🎉", "Incident #$targetId afhandelen en sluiten (Status -> RESOLVED)...")
                    val response = client.patch("$baseUrl/api/incidents/$targetId/status") {
                        authHeaders()
                        setBody(ChangeStatusRequest(status = Status.RESOLVED))
                    }
                    if (response.status == HttpStatusCode.OK) {
                        val updated = response.body<IncidentResponse>()
                        log("✅", "Incident #$targetId status gewijzigd naar '${updated.status}'")
                    } else {
                        log("⚠️", "Status wijzigen mislukt: HTTP ${response.status}")
                    }
                }
            },

            // Stap H: Status van tweede incident naar RESOLVED
            suspend {
                val targetId = if (createdIncidentIds.size >= 2) createdIncidentIds[1] else existingIncidents.getOrNull(1)?.id
                if (targetId != null) {
                    log("🎉", "Incident #$targetId afhandelen en sluiten (Status -> RESOLVED)...")
                    val response = client.patch("$baseUrl/api/incidents/$targetId/status") {
                        authHeaders()
                        setBody(ChangeStatusRequest(status = Status.RESOLVED))
                    }
                    if (response.status == HttpStatusCode.OK) {
                        val updated = response.body<IncidentResponse>()
                        log("✅", "Incident #$targetId status gewijzigd naar '${updated.status}'")
                    } else {
                        log("⚠️", "Status wijzigen mislukt: HTTP ${response.status}")
                    }
                }
            }
        )

        // Uitvoeren van de initiële scenario stappen
        log("🎬", "Starten van demonstratiescenario met ${scenarioEvents.size} stappen...")
        for ((index, step) in scenarioEvents.withIndex()) {
            waitRandomDelay(500, 3000)
            log("▶️", "--- Stap ${index + 1}/${scenarioEvents.size} ---")
            step()
        }

        log("🏁", "Demonstratiescenario succesvol voltooid!")

        // Continue simulatiemodus indien gewenst
        if (continuousMode) {
            log("🔁", "Continue simulatiemodus ingeschakeld (Druk Ctrl+C om te stoppen)...")
            var cycle = 1
            val sampleDescriptions = listOf(
                "Losliggende stoeptegel bij bushalte Centrum",
                "Verkeersongeval tussen fietser en scooter op kruispunt",
                "Afvaldumping aangetroffen in park",
                "Straatverlichting buiten werking in woonwijk",
                "Geluidsoverlast door werkzaamheden buiten vastgestelde tijden",
                "Olievlek op het wegdek nabij rotonde"
            )

            while (true) {
                waitRandomDelay(800, 3000)
                val actionType = Random.nextInt(3)
                when (actionType) {
                    0 -> {
                        // Nieuw incident aanmaken
                        val cat = Category.entries.random()
                        val desc = sampleDescriptions.random() + " (#$cycle)"
                        val prio = Priority.entries.random()
                        val lat = 51.55 + Random.nextDouble(0.01, 0.05)
                        val lon = 5.05 + Random.nextDouble(0.01, 0.05)

                        log("🚨", "[Loop #$cycle] Nieuw incident melden: '$desc' ($prio, $cat)...")
                        val response = client.post("$baseUrl/api/incidents") {
                            authHeaders()
                            setBody(CreateIncidentRequest(cat, desc, lat, lon, prio))
                        }
                        if (response.status == HttpStatusCode.Created) {
                            val created = response.body<IncidentResponse>()
                            createdIncidentIds.add(created.id)
                            log("✅", "Incident #${created.id} aangemaakt.")
                        }
                    }
                    1 -> {
                        // Prioriteit wijzigen
                        val targetId = createdIncidentIds.randomOrNull() ?: existingIncidents.randomOrNull()?.id
                        if (targetId != null) {
                            val newPrio = Priority.entries.random()
                            log("⚡", "[Loop #$cycle] Prioriteit incident #$targetId wijzigen naar $newPrio...")
                            client.patch("$baseUrl/api/incidents/$targetId/priority") {
                                authHeaders()
                                setBody(ChangePriorityRequest(newPrio))
                            }
                        }
                    }
                    2 -> {
                        // Status wijzigen
                        val targetId = createdIncidentIds.randomOrNull() ?: existingIncidents.randomOrNull()?.id
                        if (targetId != null) {
                            val newStatus = listOf(Status.ASSIGNED, Status.RESOLVED).random()
                            log("🔄", "[Loop #$cycle] Status incident #$targetId wijzigen naar $newStatus...")
                            client.patch("$baseUrl/api/incidents/$targetId/status") {
                                authHeaders()
                                setBody(ChangeStatusRequest(newStatus))
                            }
                        }
                    }
                }
                cycle++
            }
        }

    } catch (e: Exception) {
        log("💥", "Fout opgetreden tijdens simulatie: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
