package incident.android.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import incident.shared.incidents.IncidentResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.incidentDataStore: DataStore<Preferences> by preferencesDataStore(name = "incident_data")

class IncidentDataStore(private val context: Context) {

    companion object {
        private val SELECTED_INCIDENT_KEY = stringPreferencesKey("selected_incident")
        private val RECENT_INCIDENTS_KEY = stringPreferencesKey("recent_incidents")
    }

    // Save selected incident
    suspend fun saveSelectedIncident(incident: IncidentResponse) {
        context.incidentDataStore.edit { preferences ->
            val incidentJson = Json.encodeToString(incident)
            preferences[SELECTED_INCIDENT_KEY] = incidentJson
        }
    }

    // Get selected incident flow
    val selectedIncident: Flow<IncidentResponse?> = context.incidentDataStore.data
        .map { preferences ->
            preferences[SELECTED_INCIDENT_KEY]?.let { incidentJson ->
                try {
                    Json.decodeFromString<IncidentResponse>(incidentJson)
                } catch (e: Exception) {
                    null
                }
            }
        }

    // Clear selected incident
    suspend fun clearSelectedIncident() {
        context.incidentDataStore.edit { preferences ->
            preferences.remove(SELECTED_INCIDENT_KEY)
        }
    }

    // Save multiple recent incidents for future use
    suspend fun saveRecentIncidents(incidents: List<IncidentResponse>) {
        context.incidentDataStore.edit { preferences ->
            val incidentsJson = Json.encodeToString(incidents)
            preferences[RECENT_INCIDENTS_KEY] = incidentsJson
        }
    }

    // Get recent incidents
    val recentIncidents: Flow<List<IncidentResponse>> = context.incidentDataStore.data
        .map { preferences ->
            preferences[RECENT_INCIDENTS_KEY]?.let { incidentsJson ->
                try {
                    Json.decodeFromString<List<IncidentResponse>>(incidentsJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
}