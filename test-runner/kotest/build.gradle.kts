plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")

    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    //General
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    //Kotest
    testImplementation("io.kotest:kotest-runner-junit5:5.4.2")
    testImplementation("io.kotest:kotest-framework-datatest:5.4.2")
    testImplementation("io.kotest:kotest-extensions-junit5:5.4.2")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2")
    //Mock
    testImplementation("io.mockk:mockk")
    testImplementation("com.ninja-squad:springmockk")
    //General Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.assertj")
    }
    //Security
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
    //Data Jdbc
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:postgresql")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
