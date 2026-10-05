package incident.server.core

interface CrudRepository<T, ID> {
    suspend fun findAll(): List<T>
    suspend fun findAllPaginated(page: Int, pageSize: Int): Pair<List<T>, Long>

    suspend fun findById(id: ID): T?
    suspend fun save(entity: T): T
    suspend fun saveAll(entities: Iterable<T>): List<T>
    suspend fun delete(id: ID): Boolean
}

/**
 * Base class for in-memory fakes, intended for demos and tests only.
 *
 * Not thread-safe: [items] is a plain list and [currentId] is incremented without synchronization,
 * so concurrent requests can corrupt the state. Instantiate one fake per application or test.
 */
abstract class BaseInMemoryRepository<T> : CrudRepository<T, Long> {
    protected abstract val items: MutableList<T>
    protected abstract var currentId: Long

    // Abstract functions for entity manipulation
    protected abstract fun copyWithNewId(entity: T, id: Long): T
    protected abstract fun getId(entity: T): Long

    override suspend fun findAll(): List<T> = items.toList()

    override suspend fun findById(id: Long): T? = items.find { getId(it) == id }

    override suspend fun findAllPaginated(page: Int, pageSize: Int): Pair<List<T>, Long> {
        require(page > 0) { "Page number must be positive" }
        require(pageSize > 0) { "Page size must be positive" }

        val totalSize = items.size.toLong()

        if (items.isEmpty()) {
            return Pair(emptyList(), totalSize)
        }

        val startIndex = (page - 1) * pageSize
        if (startIndex >= items.size) {
            return Pair(emptyList(), totalSize)
        }

        val endIndex = minOf(startIndex + pageSize, items.size)
        val pageItems = items.subList(startIndex, endIndex).toList()

        return Pair(pageItems, totalSize)
    }

    override suspend fun save(entity: T): T {
        return if (getId(entity) > 0 && items.any { getId(it) == getId(entity) }) {
            update(entity)
        } else {
            create(entity)
        }
    }

    override suspend fun saveAll(entities: Iterable<T>): List<T> = entities.map { save(it) }

    override suspend fun delete(id: Long): Boolean = items.removeIf { getId(it) == id }

    protected fun create(entity: T): T {
        currentId++
        val newEntity = copyWithNewId(entity, currentId)
        items.add(newEntity)
        return newEntity
    }

    protected fun update(entity: T): T {
        check(getId(entity) > 0) { "Id must be greater than 0" }
        require(items.any { getId(it) == getId(entity) }) { "Entity with id ${getId(entity)} does not exist" }
        items.removeIf { getId(it) == getId(entity) }
        items.add(entity)
        return entity
    }
}
