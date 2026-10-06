package incident.shared.incidents

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class Priority {
    LOW, NORMAL, HIGH, CRITICAL
}

@Serializable
enum class Status {
    REPORTED, ASSIGNED, RESOLVED
}

@Serializable
enum class Category {
    CRIME, ENVIRONMENT, COMMUNAL, TRAFFIC, OTHER
}

@Serializable
data class CreateIncidentRequest(
    val category: Category,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val priority: Priority = Priority.LOW
)

@Serializable
data class UpdateIncidentRequest(
    val category: Category? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class ChangePriorityRequest(
    val priority: Priority
)

@Serializable
data class ChangeStatusRequest(
    val status: Status
)

@Serializable
data class ReporterResponse(
    val id: Long,
    val username: String,
    val email: String,
    val avatar: String? = null
)

@Serializable
data class IncidentResponse(
    val reportedBy: Long?,
    val reporter: ReporterResponse? = null,
    val category: Category,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val images: List<String>,
    val priority: Priority,
    val status: Status,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val dueAt: LocalDateTime,
    val isAnonymous: Boolean,
    val id: Long = 0L
)

// TODO: [SSE Stap 3] Gedeeld datamodel definiëren voor stream-events (IncidentStreamEvent)
@Serializable
data class IncidentStreamEvent(
    val eventType: String,
    val incident: IncidentResponse
)
