package incident.server.incidents

import incident.server.users.UsersTable
import incident.shared.incidents.Category
import incident.shared.incidents.Priority
import incident.shared.incidents.Status
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Exposed table for [Incident]. The reporter is a nullable foreign key to [UsersTable]: `null`
 * means an anonymous report, exactly like [Incident.reportedBy].
 */
object IncidentsTable : LongIdTable("incidents") {
    val reportedBy = optReference("reported_by", UsersTable, onDelete = ReferenceOption.SET_NULL)

    val category = enumerationByName<Category>("category", 20)
    val description = text("description")

    val latitude = double("latitude")
    val longitude = double("longitude")

    val priority = enumerationByName<Priority>("priority", 20)
    val status = enumerationByName<Status>("status", 20)

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val completedAt = timestamp("completed_at").nullable()
}

/**
 * The images of an incident in a separate table: a relational database has no list column, and this
 * keeps the schema portable (a PostgreSQL array or a JSON column would not work on H2).
 * [position] preserves the order in which the images were uploaded.
 */
object IncidentImagesTable : Table("incident_images") {
    val incident = reference("incident_id", IncidentsTable, onDelete = ReferenceOption.CASCADE)
    val position = integer("position")
    val fileName = varchar("file_name", 255)

    override val primaryKey = PrimaryKey(incident, position)
}
