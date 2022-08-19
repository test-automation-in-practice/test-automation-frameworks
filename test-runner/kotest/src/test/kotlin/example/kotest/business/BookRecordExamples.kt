package example.kotest.business

import example.kotest.persistence.BookRecord
import java.util.*

object BookRecordExamples {
    val REFACTORING = BookRecord(
        id = UUID.fromString("cd690768-74d4-48a8-8443-664975dd46b5"),
        title = BookExamples.REFACTORING.title,
        isbn = BookExamples.REFACTORING.isbn
    )
}
