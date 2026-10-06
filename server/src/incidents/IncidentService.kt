package incident.server.incidents

import incident.server.utils.currentInstant
import incident.shared.incidents.IncidentStreamEvent
import incident.shared.incidents.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

class IncidentService(
    private val incidentRepository: IncidentRepository<Long>,
) {
    private val _events = MutableSharedFlow<IncidentStreamEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<IncidentStreamEvent> = _events.asSharedFlow()

    suspend fun notifyChange(event: IncidentStreamEvent) {
        _events.emit(event)
    }
    suspend fun findAll(): List<Incident> =
        incidentRepository.findAll()

    suspend fun findAllPaginated(page: Int, pageSize: Int): Pair<List<Incident>, Long> {
        return incidentRepository.findAllPaginated(page, pageSize)
    }

    suspend fun findById(id: Long): Incident? =
        incidentRepository.findById(id)

    suspend fun findIncidentsReportedByUser(userId: Long): List<Incident> =
        incidentRepository.findIncidentsForUser(userId)

    suspend fun save(incident: Incident): Incident =
        incidentRepository.save(incident)

    suspend fun delete(incidentId: Long): Boolean {
        val foundIncident = incidentRepository.findById(incidentId)
        return if (foundIncident != null) {
            incidentRepository.delete(incidentId)
            // also remove all images of this incident
            withContext(Dispatchers.IO) {
                for (imageFile in foundIncident.images) {
                    val imageToDelete = Path(getImageUploadPath(imageFile))
                    imageToDelete.deleteIfExists()
                }
            }
            true
        } else false
    }

    suspend fun changeStatus(incident: Incident, status: Status): Incident {
        val updatedIncident = if (status == Status.RESOLVED) {
            incident.copy(
                status = status,
                completedAt = currentInstant()
            )
        } else {
            incident.copy(status = status)
        }

        return incidentRepository.save(updatedIncident)
    }

    suspend fun addImage(incidentId: Long, imageFileName: String): Incident {
        val incident = incidentRepository.findById(incidentId)
            ?: throw IllegalArgumentException("Incident not found: $incidentId")

        // Business logic here: updating the updatedAt timestamp
        val updatedIncident = incident.copy(
            images = incident.images + imageFileName,
            updatedAt = currentInstant()
        )

        // Use specialized repository method with prepared entity
        return incidentRepository.save(updatedIncident)
    }
}
