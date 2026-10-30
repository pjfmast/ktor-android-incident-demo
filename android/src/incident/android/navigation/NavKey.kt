package incident.android.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object LoginKey : NavKey

@Serializable
object MyIncidentListKey : NavKey

@Serializable
object RegisterKey : NavKey

@Serializable
object MyIncidentDetailKey : NavKey

@Serializable
object ReportIncidentKey : NavKey

@Serializable
object IncidentListKey : NavKey

@Serializable
object IncidentMapKey : NavKey

@Serializable
object UserManagementKey : NavKey

@Serializable
data class UserProfileKey(val userJson: String) : NavKey

@Serializable
data class IncidentDetailKey(val incidentId: Long) : NavKey
