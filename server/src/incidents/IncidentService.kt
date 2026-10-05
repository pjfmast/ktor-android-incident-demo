package incident.server.incidents

import incident.server.utils.currentInstant
import incident.shared.incidents.Status
import java.nio.file.Files.deleteIfExists
import kotlin.io.path.Path

class IncidentService(
    private val incidentRepository: IncidentRepository<Long>,
) {
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
            foundIncident.images.forEach { imageFile ->
                val imageToDelete = Path(getImageUploadPath(imageFile))
                deleteIfExists(imageToDelete)
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
