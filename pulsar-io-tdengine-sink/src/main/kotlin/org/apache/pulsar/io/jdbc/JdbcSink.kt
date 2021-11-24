package org.apache.pulsar.io.jdbc

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.zaxxer.hikari.HikariDataSource
import org.apache.pulsar.functions.api.Record
import org.apache.pulsar.io.core.Sink
import org.apache.pulsar.io.core.SinkContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.*

@Suppress("unused")
class JdbcSink : Sink<ByteArray> {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(JdbcSink::class.java)
    }

    private lateinit var dataSource: HikariDataSource
    private lateinit var sinkConfig: JdbcSinkConfig

    override fun open(config: MutableMap<String, Any?>, context: SinkContext) {
        sinkConfig = config.toJdbcSinkConfig().also { it.validate() }
        Class.forName(sinkConfig.driver)
        dataSource = HikariDataSource().also {
            it.driverClassName = sinkConfig.driver
            it.jdbcUrl = sinkConfig.jdbcUrl
            it.username = sinkConfig.username
            it.password = sinkConfig.password
            it.isAutoCommit = false
        }
        System.setProperty("user.timezone", sinkConfig.timezone)
        TimeZone.setDefault(TimeZone.getTimeZone(sinkConfig.timezone))
        LOGGER.info("Instance {} connected to {}", context.instanceId, sinkConfig.jdbcUrl)
    }

    override fun close() {
        dataSource.close()
    }

    override fun write(record: Record<ByteArray>) {
        LOGGER.info(
            "ACTION: {}, TARGET: {}, ENTITY: {}, RECORD: {}",
            record.action,
            record.target,
            record.entity,
            record::class
        )
        dataSource.connection.use { conn ->
            val sql = conn.buildSQL(record.target, record.action, record.entity)
            val statement = conn.prepareStatement(sql)
            LOGGER.info("STATEMENT: {}", sql)
            statement.bindValue(record.target, record.action, record.entity)
            try {
                statement.execute()
                conn.commit()
                record.ack()
            } catch (e: SQLException) {
                LOGGER.warn("Caught SQLException when execute statement", e)
//                conn.rollback()
                throw e
            }
        }
    }
}

inline val Record<ByteArray>.action: JdbcAction
    get() = JdbcAction.valueOf(properties.getOrDefault("ACTION", "INSERT"))

inline val Record<ByteArray>.target: String
    get() = properties["TARGET"]
        ?: throw IllegalArgumentException("Required property 'TARGET' for action '$action' not set.")

inline val Record<ByteArray>.entity: JsonElement
    get() = JsonParser().parse(String(value))