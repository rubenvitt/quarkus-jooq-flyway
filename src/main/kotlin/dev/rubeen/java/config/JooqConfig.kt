package dev.rubeen.java.config

import io.quarkiverse.jooq.runtime.JooqCustomContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import org.jooq.Configuration
import org.jooq.RecordContext
import org.jooq.RecordListener
import org.jooq.impl.DefaultRecordListenerProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ApplicationScoped
class JooqConfig {
    private val logger: Logger = LoggerFactory.getLogger(JooqConfig::class.java)

    @ApplicationScoped
    @Produces
    @Named("myJooqConfiguration")
    fun createJooqConfig(): JooqCustomContext {
        logger.debug("Creating JooqCustomContext")
        return object : JooqCustomContext {
            override fun apply(configuration: Configuration) {
                configuration.set(DefaultRecordListenerProvider(MyRecordListener()))
            }
        }
    }
}

class MyRecordListener : RecordListener {
    private val logger: Logger = LoggerFactory.getLogger(MyRecordListener::class.java)
    override fun insertStart(ctx: RecordContext) {
//        if (ctx.recordType() == Users.USERS.recordType()) {
//            val record = ctx.record() as UsersRecord
//            generateId().let {
//                logger.warn("Set id of new user to {}", it)
//                record.id = it
//            }
//        }
    }
}
