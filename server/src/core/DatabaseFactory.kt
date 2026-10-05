package incident.server.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Single place where the JDBC connection is configured and the schema is created.
 *
 * Only the four [init] parameters are database-specific, so moving from H2 to a real database
 * (PostgreSQL) is a matter of passing another url/driver/credentials; the tables and the
 * repositories stay exactly the same.
 */
object DatabaseFactory {

    // --- H2, in memory --------------------------------------------------------------------------
    // Everything is lost when the connection closes, so DB_CLOSE_DELAY=-1 keeps the database alive
    // as long as the JVM lives. Ideal for demos and tests: every start begins with an empty schema.
    const val H2_IN_MEMORY_URL = "jdbc:h2:mem:incidents;DB_CLOSE_DELAY=-1"

    // --- H2, stored in a file -------------------------------------------------------------------
    // Survives restarts: H2 creates ./data/incidents.mv.db relative to the working directory.
    // Use it by passing this url to init(), e.g. DatabaseFactory.init(tables, H2_FILE_URL).
    // AUTO_SERVER=TRUE additionally allows a second process (a database tool) to connect.
    const val H2_FILE_URL = "jdbc:h2:file:./data/incidents;AUTO_SERVER=TRUE"

    const val H2_DRIVER = "org.h2.Driver"

    // --- PostgreSQL -----------------------------------------------------------------------------
    // Add the driver dependency (org.postgresql:postgresql) and call, for example:
    //   DatabaseFactory.init(
    //       tables = tables,
    //       url = "jdbc:postgresql://localhost:5432/incidents",
    //       driver = "org.postgresql.Driver",
    //       user = "postgres",
    //       password = "secret"
    //   )
    // In production the schema is normally managed by migrations (Flyway/Liquibase) instead of
    // SchemaUtils.create, and the credentials come from the configuration or the environment.

    /**
     * Connects to the database and creates the [tables] that do not exist yet.
     */
    fun init(
        tables: List<Table>,
        url: String = H2_IN_MEMORY_URL,
        driver: String = H2_DRIVER,
        user: String = "",
        password: String = ""
    ): Database {
        val database = Database.connect(url = url, driver = driver, user = user, password = password)
        transaction(database) {
            SchemaUtils.create(*tables.toTypedArray())
        }
        return database
    }
}

/**
 * Runs [block] in a suspending transaction on the IO dispatcher: JDBC calls are blocking, so they
 * must not occupy the threads that Ktor uses to handle requests.
 */
suspend fun <T> dbQuery(block: suspend JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction(statement = block)
    }
