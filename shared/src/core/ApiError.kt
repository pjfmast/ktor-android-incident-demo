package incident.shared.core

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val message: String
)
