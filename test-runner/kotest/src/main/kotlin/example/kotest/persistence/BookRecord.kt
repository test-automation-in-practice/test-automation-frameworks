package example.kotest.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("book_records")
data class BookRecord(
    @Id
    val id: UUID,
    val title: String,
    val isbn: String,
    @Version
    var version: Long = 0
)
