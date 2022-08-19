package example.kotest.persistence

import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@Configuration
@EnableJdbcRepositories
class PersistenceConfig
