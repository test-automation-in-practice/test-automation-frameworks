package example.kotest.security

import example.kotest.security.Authorities.ROLE_CURATOR
import example.kotest.security.Authorities.ROLE_USER
import example.kotest.security.Authorities.SCOPE_ACTUATOR
import example.kotest.security.Authorities.SCOPE_BOOKS
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {

    private val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors { disable() }
            csrf { disable() }

            sessionManagement {
                sessionCreationPolicy = STATELESS
            }

            httpBasic {}
            authorizeRequests {
                authorize(EndpointRequest.to(InfoEndpoint::class.java, HealthEndpoint::class.java), permitAll)
                authorize(EndpointRequest.toAnyEndpoint(), hasAuthority(SCOPE_ACTUATOR))

                authorize("/api/books/**", hasAuthority(SCOPE_BOOKS))
                authorize(anyRequest, denyAll)
            }
        }
        return http.build()
    }

    @Bean
    fun userDetailService(): UserDetailsService =
        InMemoryUserDetailsManager(
            user("user", SCOPE_BOOKS, ROLE_USER),
            user("curator", SCOPE_BOOKS, ROLE_USER, ROLE_CURATOR),
            user("actuator", SCOPE_ACTUATOR)
        )

    private fun user(username: String, vararg authorities: String) = User.withUsername(username)
        .password(passwordEncoder.encode(username.reversed()))
        .authorities(*authorities)
        .build()

}
