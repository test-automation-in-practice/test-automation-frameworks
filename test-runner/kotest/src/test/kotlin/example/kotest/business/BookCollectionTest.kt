package example.kotest.business

import example.kotest.persistence.BookRecordRepository
import example.kotest.security.Authorities.ROLE_CURATOR
import example.kotest.security.Authorities.ROLE_USER
import example.kotest.security.MethodSecurityConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotestextensions.clearAllMocks
import kotestextensions.saveUserInSecurityContext
import kotestextensions.setupMockUser
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.util.IdGenerator

val bookRecord = BookRecordExamples.REFACTORING
val book = BookExamples.REFACTORING
val uuid = bookRecord.id

internal class BookCollectionFnTest : FunSpec({

    val repository: BookRecordRepository = mockk()
    val idGenerator: IdGenerator = mockk()
    val cut = BookCollection(repository, idGenerator)

    beforeEach(clearAllMocks)

    context("AddBook") {
        test("creates a BookRecord and saves it in the repository") {
            every { idGenerator.generateId() } returns uuid
            every { repository.save(any()) } answers { firstArg() }

            cut.addBook(book) shouldBe bookRecord
            verify { repository.save(bookRecord) }
        }
    }

    context("GetBookById") {
        test("returns null if book was not found in the repository") {
            every { repository.findByIdOrNull(uuid) } returns null
            cut.getBookById(uuid).shouldBeNull()
        }
        test("returns a BookRecord if one was found in the repository") {
            every { repository.findByIdOrNull(uuid) } returns bookRecord
            cut.getBookById(uuid) shouldBe bookRecord
        }
    }

    context("DeleteBookById") {
        test("returns true on successful delete") {
            justRun { cut.deleteBookById(uuid) }
            cut.deleteBookById(uuid).shouldBeTrue()
        }
        test("returns false on error during delete") {
            every { repository.deleteById(uuid) } throws RuntimeException()
            cut.deleteBookById(uuid).shouldBeFalse()
        }
    }
})

@SpringBootTest(classes = [SecurityTestsConfiguration::class])
internal class BookCollectionSecurityTests(
    @Autowired val cut: BookCollection
) : FunSpec({
    extension(SpringExtension)

    context("users with the USER role") {
        withAuthorities(ROLE_USER)
        test("cannot add books") { assertAccessDenied { cut.addBook(book) } }
        test("can get books by ID") { assertAccessGranted { cut.getBookById(uuid) } }
        test("cannot delete books by ID") { assertAccessDenied { cut.deleteBookById(uuid) } }
    }

    context("users with the CURATOR role") {
        beforeEach {
            println("first before")
        }
        withAuthorities(ROLE_CURATOR)
        beforeEach {
            println("second before")
        }
        afterEach {
            println("after stuff")
        }
        test("can add books") { assertAccessGranted { cut.addBook(book) } }
        test("can get books by ID") { assertAccessGranted { cut.getBookById(uuid) } }
        test("can delete books by ID") { assertAccessGranted { cut.deleteBookById(uuid) } }
    }

    context("without a user context") {
        test("books cannot be added") { assertNotAuthenticated { cut.addBook(book) } }
        test("books cannot be got by ID") { assertNotAuthenticated { cut.getBookById(uuid) } }
        test("books cannot be deleted by ID") { assertNotAuthenticated { cut.deleteBookById(uuid) } }
    }
})

fun ContainerScope.withAuthorities(vararg authorities: String) {
    beforeEach {
        println("in authority")
        val user = setupMockUser(authorities = authorities)
        saveUserInSecurityContext(user)
    }

    afterEach {
        println("after test")
        TestSecurityContextHolder.clearContext()
    }
}

private fun assertAccessGranted(block: () -> Unit) = assertDoesNotThrow(block)
private fun assertAccessDenied(block: () -> Unit) = assertThrows<AccessDeniedException>(block)
private fun assertNotAuthenticated(block: () -> Unit) = assertThrows<AuthenticationCredentialsNotFoundException>(block)

@Import(MethodSecurityConfiguration::class)
class SecurityTestsConfiguration {
    @Bean
    fun bookCollection(): BookCollection = mockk(relaxed = true)
}
