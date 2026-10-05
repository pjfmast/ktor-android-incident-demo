package incident.server.incidents

import incident.server.core.dbQuery
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Persistent [IncidentRepository] on top of Exposed (DSL), see [IncidentsTable].
 *
 * The images live in [IncidentImagesTable], so reading incidents is always a two-step operation:
 * first the incident rows, then the images belonging to those rows.
 */
class ExposedIncidentRepository : IncidentRepository<Long> {

    override suspend fun findAll(): List<Incident> = dbQuery {
        IncidentsTable.selectAll()
            .orderBy(IncidentsTable.id)
            .toIncidents()
    }

    override suspend fun findAllPaginated(page: Int, pageSize: Int): Pair<List<Incident>, Long> {
        require(page > 0) { "Page number must be positive" }
        require(pageSize > 0) { "Page size must be positive" }

        return dbQuery {
            val totalSize = IncidentsTable.selectAll().count()
            val incidents = IncidentsTable.selectAll()
                .orderBy(IncidentsTable.id)
                .limit(pageSize)
                .offset((page - 1).toLong() * pageSize)
                .toIncidents()

            incidents to totalSize
        }
    }

    override suspend fun findById(id: Long): Incident? = dbQuery {
        IncidentsTable.selectAll()
            .where { IncidentsTable.id eq id }
            .toIncidents()
            .singleOrNull()
    }

    override suspend fun save(entity: Incident): Incident = dbQuery { saveInTransaction(entity) }

    override suspend fun saveAll(entities: Iterable<Incident>): List<Incident> = dbQuery {
        entities.map { saveInTransaction(it) }
    }

    override suspend fun delete(id: Long): Boolean = dbQuery {
        // The images are removed by the ON DELETE CASCADE of IncidentImagesTable.
        IncidentsTable.deleteWhere { IncidentsTable.id eq id } > 0
    }

    // IncidentRepository-specific functionality:
    override suspend fun findIncidentsForUser(userID: Long): List<Incident> = dbQuery {
        IncidentsTable.selectAll()
            .where { IncidentsTable.reportedBy eq userID }
            .orderBy(IncidentsTable.id)
            .toIncidents()
    }

    override suspend fun findIncidentsInBoundingBox(
        latMin: Double,
        latMax: Double,
        lngMin: Double,
        lngMax: Double
    ): List<Incident> = dbQuery {
        // The same condition as Incident.isCoordinateInArea, but evaluated by the database.
        IncidentsTable.selectAll()
            .where {
                (IncidentsTable.latitude greaterEq latMin) and
                        (IncidentsTable.latitude lessEq latMax) and
                        (IncidentsTable.longitude greaterEq lngMin) and
                        (IncidentsTable.longitude lessEq lngMax)
            }
            .orderBy(IncidentsTable.id)
            .toIncidents()
    }

    /**
     * Insert or update, without starting a transaction of its own, so callers decide how many
     * incidents are saved together. Must be called from within a transaction.
     */
    private fun saveInTransaction(incident: Incident): Incident {
        val exists = incident.id > Incident.NEW_INCIDENT_ID &&
                IncidentsTable.selectAll().where { IncidentsTable.id eq incident.id }.count() > 0

        return if (exists) update(incident) else insert(incident)
    }

    private fun insert(incident: Incident): Incident {
        val generatedId = IncidentsTable.insertAndGetId { row ->
            row.write(incident)
        }
        if (incident.images.isNotEmpty()) saveImages(generatedId.value, incident.images)
        return incident.copy(id = generatedId.value)
    }

    private fun update(incident: Incident): Incident {
        IncidentsTable.update({ IncidentsTable.id eq incident.id }) { row ->
            row.write(incident)
        }
        saveImages(incident.id, incident.images)
        return incident
    }

    /**
     * Replaces the image rows of one incident: simpler (and correct) compared to figuring out
     * which images were added or removed.
     */
    private fun saveImages(incidentId: Long, images: List<String>) {
        IncidentImagesTable.deleteWhere { IncidentImagesTable.incident eq incidentId }
        images.forEachIndexed { index, fileName ->
            IncidentImagesTable.insert { row ->
                row[IncidentImagesTable.incident] = incidentId
                row[IncidentImagesTable.position] = index
                row[IncidentImagesTable.fileName] = fileName
            }
        }
    }
}

/**
 * Writes all columns except the generated `id`; shared by insert and update.
 */
private fun UpdateBuilder<*>.write(incident: Incident) {
    this[IncidentsTable.reportedBy] = incident.reportedBy
    this[IncidentsTable.category] = incident.category
    this[IncidentsTable.description] = incident.description
    this[IncidentsTable.latitude] = incident.latitude
    this[IncidentsTable.longitude] = incident.longitude
    this[IncidentsTable.priority] = incident.priority
    this[IncidentsTable.status] = incident.status
    this[IncidentsTable.createdAt] = incident.createdAt
    this[IncidentsTable.updatedAt] = incident.updatedAt
    this[IncidentsTable.completedAt] = incident.completedAt
}

/**
 * Maps incident rows to domain objects and adds their images with one extra query, instead of one
 * query per incident.
 */
private fun Iterable<ResultRow>.toIncidents(): List<Incident> {
    val rows = toList()
    if (rows.isEmpty()) return emptyList()

    val incidentIds = rows.map { it[IncidentsTable.id].value }
    val imagesByIncident = IncidentImagesTable.selectAll()
        .where { IncidentImagesTable.incident inList incidentIds }
        .orderBy(IncidentImagesTable.position)
        .groupBy(
            keySelector = { it[IncidentImagesTable.incident].value },
            valueTransform = { it[IncidentImagesTable.fileName] }
        )

    return rows.map { row ->
        val id = row[IncidentsTable.id].value
        row.toIncident(imagesByIncident[id] ?: emptyList())
    }
}

private fun ResultRow.toIncident(images: List<String>): Incident = Incident(
    reportedBy = this[IncidentsTable.reportedBy]?.value,
    category = this[IncidentsTable.category],
    description = this[IncidentsTable.description],
    latitude = this[IncidentsTable.latitude],
    longitude = this[IncidentsTable.longitude],
    priority = this[IncidentsTable.priority],
    status = this[IncidentsTable.status],
    images = images,
    createdAt = this[IncidentsTable.createdAt],
    updatedAt = this[IncidentsTable.updatedAt],
    completedAt = this[IncidentsTable.completedAt],
    id = this[IncidentsTable.id].value
)
