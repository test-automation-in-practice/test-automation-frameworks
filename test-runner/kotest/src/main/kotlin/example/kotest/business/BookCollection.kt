package example.kotest.business

import example.kotest.persistence.BookRecord
import example.kotest.persistence.BookRecordRepository
import example.kotest.security.Authorities.ROLE_CURATOR
import example.kotest.security.Roles.CURATOR
import example.kotest.security.Roles.USER
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator
import java.util.*
import javax.annotation.security.RolesAllowed

@Component
class BookCollection(
    private val repository: BookRecordRepository,
    private val idGenerator: IdGenerator
) {

    @RolesAllowed(CURATOR)
    fun addBook(book: Book): BookRecord {
        val entity = BookRecord(
            id = idGenerator.generateId(),
            title = book.title,
            isbn = book.isbn
        )
        return repository.save(entity)
    }

    @PreAuthorize("hasAnyRole('$USER', '$CURATOR')")
    fun getBookById(id: UUID): BookRecord? {
        return repository.findByIdOrNull(id)
    }

    @Secured(ROLE_CURATOR)
    fun deleteBookById(id: UUID): Boolean {
        return try {
            repository.deleteById(id)
            true
        } catch (e: Exception) {
            println(e)
            false
        }
    }

}
