package example.kotest

import com.ninjasquad.springmockk.MockkBean
import example.kotest.persistence.BookRecordRepository
import io.kotest.common.ExperimentalKotest
import io.kotest.core.names.TestName
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.AbstractContainerScope
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.test.TestType
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import kotestextensions.clearAllMocks
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import utils.InitializeWithContainerizedPostgresql
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@InitializeWithContainerizedPostgresql
@MockkBean(BookRecordRepository::class)
internal class ApplicationSecurityKoTests(
    @LocalServerPort val port: Int,
    val repository: BookRecordRepository
) : FunSpec({
    extension(SpringExtension)

    beforeSpec {
        RestAssured.port = port
    }

    beforeEach(clearAllMocks)

    context("BookApiEndpointSecurity") {
        // This would be the end-2-end testing alternative to the WebSecurityConfigurationTests.
        test("book API endpoints are not accessible without credentials") {
            assertThatUserOnPathReturnsStatus("/api/books/${UUID.randomUUID()}", HttpStatus.UNAUTHORIZED)
        }

        test("user with just scope BOOKS can access book API endpoints") {
            val randomUUID = UUID.randomUUID()
            every { repository.findByIdOrNull(randomUUID) } returns null
            assertThatUserOnPathReturnsStatus("/api/books/$randomUUID", HttpStatus.NO_CONTENT, "user")
        }

        test("user with just scope ACTUATOR cannot access book API endpoints") {
            assertThatUserOnPathReturnsStatus("/api/books/${UUID.randomUUID()}", HttpStatus.FORBIDDEN, "actuator")
        }
    }

    context("ActuatorSecurity") {

        val publicEndpoints = setOf("/actuator/info", "/actuator/health")
        val privateEndpoints = setOf("/actuator/beans", "/actuator/env", "/actuator/metrics")

        context("without credentials only public endpoints are available") {
            withData(
                buildData(publicEndpoints to HttpStatus.OK, privateEndpoints to HttpStatus.UNAUTHORIZED)
            ) { (endpoint, status) -> assertThatUserOnPathReturnsStatus(endpoint, status) }
        }

        context("with credentials of user with ACTUATOR scope all endpoints are available") {
            withData(
                buildData(
                    publicEndpoints to HttpStatus.OK, privateEndpoints to HttpStatus.OK
                )
            ) { (endpoint, status) -> assertThatUserOnPathReturnsStatus(endpoint, status, "actuator") }
        }

        context("with credentials of user without ACTUATOR scope only public endpoints are available") {
            withData(
                buildData(
                    publicEndpoints to HttpStatus.OK,
                    privateEndpoints to HttpStatus.FORBIDDEN
                )
            ) { (endpoint, status) -> assertThatUserOnPathReturnsStatus(endpoint, status, "user") }
        }
        context("flatmap example") {
            withFlatMappedData(
                EndpointStatus::class,
                publicEndpoints to HttpStatus.OK,
                privateEndpoints to HttpStatus.FORBIDDEN
            ) { (i, e) -> println("its $i and its $e") }
        }

    }
})

private fun assertThatUserOnPathReturnsStatus(path: String, status: HttpStatus, username: String? = null) {
    Given {
        applyIf(username != null) {
            auth().preemptive().basic(username, username!!.reversed())
        }
    } When {
        get(path)
    } Then { statusCode(status.value()) }
}

private fun buildData(vararg expectations: Pair<Set<String>, HttpStatus>) = expectations
    .flatMap { (endpoints, status) -> endpoints.map { EndpointStatus(it, status) } }

data class EndpointStatus(val endpoint: String, val status: HttpStatus) : WithDataTestName {
    override fun dataTestName() = "$endpoint >> $status"
}

@OptIn(ExperimentalKotest::class)
suspend fun <E : Any, I : Any, T : WithDataTestName> ContainerScope.withFlatMappedData(
    dataClass: KClass<T>,
    vararg data: Pair<Set<I>, E>,
    test: suspend ContainerScope.(T) -> Unit
) {
    if (!dataClass.isData) error("Only Dataclasses supportet")
    val cons = dataClass.primaryConstructor
    val flatMap = data.flatMap { (set, exp) -> set.map { cons?.javaConstructor?.newInstance(it, exp) } }
    flatMap.forEach { ite ->
        registerTest(TestName(ite!!.dataTestName()), false, null, TestType.Dynamic) {
            AbstractContainerScope(this).test(ite)
        }
    }
}

inline fun <T> T.applyIf(expression: Boolean, block: T.() -> Unit): T = if (expression) apply(block) else this
