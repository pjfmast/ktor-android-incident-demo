package incident.server.incidents

import incident.server.core.CrudRepository

interface IncidentRepository<ID>: CrudRepository<Incident, ID> {
    suspend fun findIncidentsForUser(userID: ID): List<Incident>
    suspend fun findIncidentsInBoundingBox(latMin: Double, latMax: Double, lngMin: Double, lngMax: Double): List<Incident>
}
