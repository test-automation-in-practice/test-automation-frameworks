package example.kotest.persistence

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveCauseInstanceOf
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.test.context.ActiveProfiles
import utils.InitializeWithContainerizedPostgresql
import java.util.*

@DataJdbcTest
@ActiveProfiles("test", "in-memory")
class BookRecordRepositoryH2Test(val cut: BookRecordRepository) : FunSpec({
    extensions(SpringExtension)
    include(bookRecordRepositoryContract(cut))
})

@DataJdbcTest
@ActiveProfiles("test", "docker")
@InitializeWithContainerizedPostgresql
class BookRecordRepositoryDockerizedTest(val cut: BookRecordRepository) : FunSpec({
    extensions(SpringExtension)
    include(bookRecordRepositoryContract(cut))
})

private fun bookRecordRepositoryContract(cut: BookRecordRepository) = funSpec {
    test("entity can be saved") {
        val entity = bookRecordEntity()
        val savedEntity = cut.save(entity)
        savedEntity shouldBe entity
    }

    test("entity version is increased with every save") {
        val entity = bookRecordEntity()

        cut.save(entity).version shouldBe 1
        cut.save(entity).version shouldBe 2
        cut.save(entity).version shouldBe 3

        entity.version shouldBe 3
    }

    test("entity can not be saved in lower than current version") {
        val entity = bookRecordEntity()
            .also(cut::save)
            .also(cut::save)
        val entityWithLowerVersion = entity.copy(version = entity.version - 1)

        val exception = shouldThrow<DbActionExecutionException> {
            cut.save(entityWithLowerVersion)
        }
        exception.shouldHaveCauseInstanceOf<OptimisticLockingFailureException>()
    }

    test("entity can be found by id") {
        val savedEntity = cut.save(bookRecordEntity())
        val foundEntity = cut.findById(savedEntity.id)
        foundEntity.shouldBePresent().shouldBe(savedEntity)
    }

    test("entity can be found by title") {
        val e1 = cut.save(bookRecordEntity("Clean Code"))
        cut.save(bookRecordEntity("Clean Architecture"))
        val e3 = cut.save(bookRecordEntity("Clean Code"))

        val foundEntities = cut.findByTitle("Clean Code")

        foundEntities.shouldContainExactly(e1, e3)
    }
}

private fun bookRecordEntity(title: String = "Clean Code") =
    BookRecord(UUID.randomUUID(), title, "9780123456789")

