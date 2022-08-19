package example.kotest.security

import example.kotest.security.Authorities.SCOPE_ACTUATOR
import example.kotest.security.Authorities.SCOPE_BOOKS
import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.datatest.withData
import io.kotest.extensions.spring.SpringExtension
import kotestextensions.withAuthorities
import kotestextensions.withRoles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestComponent
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@TestComponent
class SecurityTestController {
    @GetMapping("/api/books", "/api/books/foo", "/api/books/bar")
    fun getBooks(@AuthenticationPrincipal user: Any) = user
}

@WebMvcTest(SecurityTestController::class)
@Import(SecurityTestController::class, WebSecurityConfiguration::class)
internal class WebSecurityConfigurationKoTests(
    @Autowired private val mockMvc: MockMvc
) : FunSpec() {
    init {
        extension(SpringExtension)

        include(allBookPathsReturnStatus("users with just the BOOKS scope can access any books endpoints")
        { path -> assertPathStatus(path, OK, SCOPE_BOOKS) })

        test("users with just the ACTUATOR scope cannot access any books endpoints")
        { path -> assertPathStatus(path, FORBIDDEN, SCOPE_ACTUATOR) }

        test("users with the BOOKS and ACTUATOR scopes can access any books endpoints")
        { path -> assertPathStatus(path, OK, SCOPE_BOOKS, SCOPE_ACTUATOR) }

        test("custom ->users with just the BOOKS scope can access any books endpoints<-") {
            withAuthorities(SCOPE_BOOKS) {
                customAssertPath("/api/books", OK)
            }
            withRoles(Roles.USER) {
                customAssertPath("/api/books/foo", FORBIDDEN)
            }
        }
    }

    private fun customAssertPath(path: String, status: HttpStatus) {
        mockMvc.get(path)
            .andExpect {
                status { isEqualTo(status.value()) }
            }
    }

    private fun assertPathStatus(path: String, status: HttpStatus, vararg authorities: String) {
        mockMvc.get(path) {
            with(user(User("a", "b", authorities.map(::SimpleGrantedAuthority))))
        }.andExpect {
            status { isEqualTo(status.value()) }
        }
    }
}

private fun DslDrivenSpec.test(desc: String, block: (String) -> Unit) = include(allBookPathsReturnStatus(desc, block))

private fun allBookPathsReturnStatus(name: String, block: (String) -> Unit) = funSpec {
    context(name) {
        withData("/api/books", "/api/books/foo", "/api/books/bar") {
            block(it)
        }
    }
}



