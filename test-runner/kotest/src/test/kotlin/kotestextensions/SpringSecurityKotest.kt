package kotestextensions

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.util.Assert

//class SpringSecurityExtension(var securityContext: SecurityContext) : BeforeEachListener, AfterEachListener {
//
//    override suspend fun beforeEach(testCase: TestCase) {
//
//        super.beforeEach(testCase)
//    }
//
//    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
//        super.afterEach(testCase, result)
//    }
//}

inline fun <T> withAuthorities(vararg authorities: String, block: () -> T) =
    withMockUser(authorities = authorities, block = block)

inline fun <T> withRoles(vararg roles: String, block: () -> T) =
    withMockUser(roles = roles, block = block)

inline fun <T> withMockUser(
    username: String = "user",
    password: String = "password",
    roles: Array<out String> = arrayOf("USER"),
    authorities: Array<out String> = emptyArray(),
    block: () -> T
): T {
    val principal = setupMockUser(username, password, roles, authorities)
    saveUserInSecurityContext(principal)

    try {
        return block()
    } finally {
        TestSecurityContextHolder.clearContext()
    }
}

fun saveUserInSecurityContext(principal: User) {
    val authentication =
        UsernamePasswordAuthenticationToken.authenticated(principal, principal.password, principal.authorities)
    val context = SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
    TestSecurityContextHolder.setContext(context)
}

fun setupMockUser(
    username: String = "user",
    password: String = "password",
    roles: Array<out String> = arrayOf("USER"),
    authorities: Array<out String> = emptyArray()
): User {
    Assert.hasText(username) { "cannot have empty username property" }

    val grantedAuthorities = authorities.map(::SimpleGrantedAuthority).toMutableList()
    if (grantedAuthorities.isEmpty()) {
        for (role in roles) {
            Assert.isTrue(!role.startsWith("ROLE_")) { "roles cannot start with ROLE_ Got $role" }
            grantedAuthorities.add(SimpleGrantedAuthority("ROLE_$role"))
        }
    } else check(roles.size == 1 && "USER" == roles[0]) {
        ("You cannot define roles attribute $roles with authorities attribute $authorities")
    }
    return User(username, password, true, true, true, true, grantedAuthorities)
}
