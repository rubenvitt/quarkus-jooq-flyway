import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jooq.meta.jaxb.Configuration
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.allopen") version "1.8.10"
    id("io.quarkus")
    id("nu.studer.jooq") version "8.2"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    testImplementation("io.quarkus:quarkus-junit5")

    implementation("io.quarkus:quarkus-hibernate-reactive")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkiverse.jooq:quarkus-jooq:2.0.0")
    implementation("io.github.thibaultmeyer:cuid:2.0.2")
    jooqGenerator("org.postgresql:postgresql:42.6.0")
}

group = "dev.rubeen.java"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    kotlinOptions.javaParameters = true
}

// region build with testcontainers, flyway and jooq
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("org.testcontainers:postgresql:1.18.0")
        classpath("org.postgresql:postgresql:42.6.0")
        classpath("org.flywaydb:flyway-core:9.17.0")
    }
}

jooq {
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)

            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    target.apply {
                        packageName = "dev.rubeen.webapps.jooq"
                    }

                    database.apply {
                        inputSchema = "public"
                        excludes = "flyway_schema_history"
                    }
                }
            }
        }
    }
}

fun startContainer(imageName: String): JdbcDatabaseContainer<*> {
    val container = PostgreSQLContainer<Nothing>(DockerImageName.parse(imageName))
    container.start()
    gradle.buildFinished {
        container.stop()
    }

    return container
}

fun flywayMigrate(container: JdbcDatabaseContainer<*>, migrationFilesLocation: String) {
    val configuration: FluentConfiguration = Flyway.configure()
        .dataSource(container.jdbcUrl, container.username, container.password)
        .locations("filesystem:$migrationFilesLocation")
    val flyway: Flyway = configuration.load()
    flyway.migrate()
}

fun modifyJooqConfiguration(jooqGenerate: JooqGenerate, container: JdbcDatabaseContainer<*>) {
    val jooqConfigurationField = JooqGenerate::class.java.getDeclaredField("jooqConfiguration")
    jooqConfigurationField.isAccessible = true
    val jooqConfiguration = jooqConfigurationField.get(jooqGenerate) as Configuration

    jooqConfiguration.jdbc.apply {
        url = container.jdbcUrl
        user = container.username
        password = container.password
    }
}

tasks.named<JooqGenerate>("generateJooq") {
    val migrationFilesLocation = "src/main/resources/db/migration"
    inputs.files(fileTree(migrationFilesLocation))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    allInputsDeclared.set(true)
    outputs.cacheIf {
        true
    }

    doFirst {
        val container = startContainer("postgres:15-alpine")
        flywayMigrate(container, migrationFilesLocation)
        modifyJooqConfiguration(this as JooqGenerate, container)
    }
}
// endregion
