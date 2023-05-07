package dev.rubeen.java.config

import dev.rubeen.webapps.jooq.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jooq.DSLContext

class TestService {
    @Inject
    lateinit var dslContext: DSLContext

    fun hello(): String {
        dslContext.newRecord(USERS).let {
            it.username = "username"
            it.password = "password"
            it.insert()

            return it.id!!
        }
    }
}