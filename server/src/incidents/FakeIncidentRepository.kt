package incident.server.incidents

import incident.server.core.BaseInMemoryRepository
import incident.server.users.UserRepository

/**
 * In-memory [IncidentRepository], meant for demos and tests only.
 *
 * This is a class (not an object): every application and every test creates its own instance, so
 * there is no global mutable state leaking between tests. Demo data is not created in an `init`
 * block but by the suspending [seedDemoData] extension, which works for any implementation.
 */
class FakeIncidentRepository : BaseInMemoryRepository<Incident>(), IncidentRepository<Long> {
    override val items = mutableListOf<Incident>()
    override var currentId: Long = Incident.NEW_INCIDENT_ID

    override fun copyWithNewId(entity: Incident, id: Long): Incident = entity.copy(id = id)
    override fun getId(entity: Incident): Long = entity.id

    // IncidentRepository-specific functionality:
    override suspend fun findIncidentsForUser(userID: Long): List<Incident> =
        items.filter { it.reportedBy == userID }

    override suspend fun findIncidentsInBoundingBox(
        latMin: Double,
        latMax: Double,
        lngMin: Double,
        lngMax: Double
    ): List<Incident> = items.filter { it.isCoordinateInArea(latMin, latMax, lngMin, lngMax) }

    companion object {
        suspend fun withDemoData(userRepository: UserRepository<Long>): FakeIncidentRepository =
            FakeIncidentRepository().also { it.seedDemoData(userRepository) }
    }
}
