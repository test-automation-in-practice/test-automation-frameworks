package example.kotest.api

import com.ninjasquad.springmockk.MockkBean
import example.kotest.business.BookCollection
import example.kotest.business.BookExamples
import example.kotest.business.BookRecordExamples
import example.kotest.security.Authorities.SCOPE_BOOKS
import example.kotest.security.WebSecurityConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import kotestextensions.clearAllMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(BooksRestController::class)
@Import(WebSecurityConfiguration::class)
@WithMockUser(authorities = [SCOPE_BOOKS])
@MockkBean(BookCollection::class)
internal class BooksRestControllerTest(
    @Autowired val bookCollection: BookCollection,
    @Autowired val mockMvc: MockMvc
) : FunSpec({
    extension(SpringExtension)

    val bookRecord = BookRecordExamples.REFACTORING
    val book = BookExamples.REFACTORING
    val id = bookRecord.id

    beforeEach(clearAllMocks)

    test("adding a new book responds with a 201 Created") {
        every { bookCollection.addBook(book) } returns bookRecord

        mockMvc
            .post("/api/books") {
                contentType = APPLICATION_JSON
                content = """
                    {
                      "isbn": "978-0134757599",
                      "title": "Refactoring: Improving the Design of Existing Code"
                    }
                    """
            }
            .andExpect {
                status { isCreated() }
                content {
                    contentTypeCompatibleWith(APPLICATION_JSON)
                    json(
                        jsonContent = """
                            {
                              "id": "cd690768-74d4-48a8-8443-664975dd46b5",
                              "isbn": "978-0134757599",
                              "title": "Refactoring: Improving the Design of Existing Code"
                            }
                            """,
                        strict = true
                    )
                }
            }
    }

    test("getting a non-existing book by ID responds with a 204 No Content") {
        every { bookCollection.getBookById(id) } returns null

        mockMvc
            .get("/api/books/$id")
            .andExpect {
                status { isNoContent() }
                content { string("") }
            }
    }

    test("getting an existing book by ID responds with a 200 Ok") {
        every { bookCollection.getBookById(id) } returns bookRecord

        mockMvc
            .get("/api/books/$id")
            .andExpect {
                status { isOk() }
                content {
                    contentTypeCompatibleWith(APPLICATION_JSON)
                    json(
                        jsonContent = """
                            {
                              "id": "cd690768-74d4-48a8-8443-664975dd46b5",
                              "isbn": "978-0134757599",
                              "title": "Refactoring: Improving the Design of Existing Code"
                            }
                            """,
                        strict = true
                    )
                }
            }
    }

    context("deleting book by ID responds with a 204 No Content") {
        listOf(true, false).forEach {
            test("was deleted = $it") {
                every { bookCollection.deleteBookById(id) } returns it

                mockMvc
                    .delete("/api/books/$id")
                    .andExpect {
                        status { isNoContent() }
                        content { string("") }
                    }
            }
        }
    }
})
