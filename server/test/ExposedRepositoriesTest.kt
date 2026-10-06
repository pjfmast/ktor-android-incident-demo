package incident.server

import incident.server.core.DatabaseFactory
import incident.server.incidents.ExposedIncidentRepository
import incident.server.incidents.Incident
import incident.server.incidents.IncidentImagesTable
import incident.server.incidents.IncidentsTable
import incident.server.users.ExposedUserRepository
import incident.server.users.User
import incident.server.users.UsersTable
import incident.shared.incidents.Category
import incident.shared.incidents.Priority
import incident.shared.incidents.Status
import incident.shared.users.Role
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExposedRepositoriesTest {

    private val userRepository = ExposedUserRepository()
    private val incidentRepository = ExposedIncidentRepository()

    @BeforeTest
    fun setUp() {
        DatabaseFactory.init(listOf(UsersTable, IncidentsTable, IncidentImagesTable))
    }

    @Test
    fun `ExposedUserRepository save performs insert for new user and upsert for existing user`() = runBlocking {
        val newUser = User(
            username = "testuser",
            password = "pwd",
            email = "test@example.com",
            role = Role.USER
        )

        val inserted = userRepository.save(newUser)
        assertNotNull(inserted.id)
        assertEquals(true, inserted.id > User.NEW_USER_ID)

        val foundAfterInsert = userRepository.findById(inserted.id)
        assertNotNull(foundAfterInsert)
        assertEquals("testuser", foundAfterInsert.username)
        assertEquals("test@example.com", foundAfterInsert.email)

        // Update via upsert
        val updated = inserted.copy(email = "updated@example.com")
        val savedUpdate = userRepository.save(updated)
        assertEquals(inserted.id, savedUpdate.id)

        val foundAfterUpdate = userRepository.findById(inserted.id)
        assertNotNull(foundAfterUpdate)
        assertEquals("updated@example.com", foundAfterUpdate.email)
    }

    @Test
    fun `ExposedIncidentRepository save performs insert for new incident and upsert for existing incident`() = runBlocking {
        val user = userRepository.save(
            User(
                username = "incidentReporter",
                password = "pwd",
                email = "reporter@example.com",
                role = Role.USER
            )
        )

        val newIncident = Incident(
            reportedBy = user.id,
            category = Category.TRAFFIC,
            description = "Pothole in the street",
            latitude = 51.58,
            longitude = 4.77,
            priority = Priority.NORMAL,
            status = Status.REPORTED,
            images = listOf("image1.jpg", "image2.jpg")
        )

        val inserted = incidentRepository.save(newIncident)
        assertEquals(true, inserted.id > Incident.NEW_INCIDENT_ID)
        assertEquals(listOf("image1.jpg", "image2.jpg"), inserted.images)

        val foundAfterInsert = incidentRepository.findById(inserted.id)
        assertNotNull(foundAfterInsert)
        assertEquals("Pothole in the street", foundAfterInsert.description)
        assertEquals(listOf("image1.jpg", "image2.jpg"), foundAfterInsert.images)

        // Update via upsert
        val updated = inserted.copy(
            description = "Pothole repaired",
            status = Status.RESOLVED,
            images = listOf("image3.jpg")
        )
        val savedUpdate = incidentRepository.save(updated)
        assertEquals(inserted.id, savedUpdate.id)

        val foundAfterUpdate = incidentRepository.findById(inserted.id)
        assertNotNull(foundAfterUpdate)
        assertEquals("Pothole repaired", foundAfterUpdate.description)
        assertEquals(Status.RESOLVED, foundAfterUpdate.status)
        assertEquals(listOf("image3.jpg"), foundAfterUpdate.images)
    }
}
