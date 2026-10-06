package incident.dashboard

import incident.shared.auth.LoginRequest
import incident.shared.auth.TokenResponse
import incident.shared.incidents.IncidentStreamEvent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter

suspend fun main(args: Array<String>) {
    val username = args.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Ron"
    val password = args.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "pwd"

    val client = HttpClient(CIO) {
        install(SSE) {
            showCommentEvents()
            showRetryEvents()
        }
        install(ContentNegotiation) {
            json()
        }
    }

    println("==================================================")
    println(" 🚨 LIVE INCIDENT DISPATCH DASHBOARD (OFFICIAL: $username) ")
    println("==================================================")

    try {
        println("🔑 Inloggen als testgebruiker $username (OFFICIAL)...")
        val loginResponse = client.post("http://localhost:8080/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = username, password = password))
        }

        if (loginResponse.status != HttpStatusCode.OK) {
            println("❌ Inloggen mislukt: HTTP ${loginResponse.status}")
            return
        }

        val tokenResponse = loginResponse.body<TokenResponse>()
        val officialToken = tokenResponse.token
        println("✅ Succesvol ingelogd als official $username. Token verkregen.")

        println("Verbinden met backend stream op http://localhost:8080/api/incidents/stream ...")

        var heartbeatCount = 0
        var pulse = false
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        client.serverSentEvents(
            urlString = "http://localhost:8080/api/incidents/stream",
            request = {
                header(HttpHeaders.Authorization, "Bearer $officialToken")
            }
        ) {
            println("🟢 Verbonden met Ktor SSE stream als $username! Luisteren naar incidenten...")
            incoming.collect { event ->
                val eventType = event.event ?: "UNKNOWN"
                val data = event.data ?: ""

                if (eventType == "heartbeat") {
                    heartbeatCount++
                    pulse = !pulse
                    val heartIcon = if (pulse) "💓" else "💗"
                    val timestamp = LocalTime.now().format(timeFormatter)
                    val info = if (data.isNotBlank()) " ($data)" else ""
                    print("\r$heartIcon [LIVE] [$timestamp] [$username] Verbinding actief • Hartslag #$heartbeatCount$info   ")
                    System.out.flush()
                } else {
                    print("\r" + " ".repeat(100) + "\r")
                    println()
                    println("==================================================")
                    println("🚨 [INCIDENT EVENT: $eventType] ID: ${event.id ?: "-"}")
                    try {
                        val streamEvent = Json.decodeFromString<IncidentStreamEvent>(data)
                        val inc = streamEvent.incident
                        println("   Omschrijving: ${inc.description}")
                        println("   Categorie:    ${inc.category}")
                        println("   Prioriteit:   ${inc.priority}")
                        println("   Status:       ${inc.status}")
                        println("   Locatie:      lat=${inc.latitude}, lon=${inc.longitude}")
                    } catch (_: Exception) {
                        println("   Payload: $data")
                    }
                    println("==================================================")
                }
            }
        }
    } catch (e: Exception) {
        println("\n⚠️ Verbindingsfout: ${e.message}")
        println("Controleer of de Ktor server gestart is op http://localhost:8080.")
    } finally {
        client.close()
    }
}
