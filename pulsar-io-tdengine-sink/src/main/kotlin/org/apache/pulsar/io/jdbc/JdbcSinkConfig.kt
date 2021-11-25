package org.apache.pulsar.io.jdbc

import com.google.gson.Gson
import org.apache.pulsar.io.core.annotations.FieldDoc

data class JdbcSinkConfig(
    @FieldDoc(
        required = true,
        defaultValue = "",
        sensitive = true,
        help = "Login username of the database"
    )
    val username: String,

    @FieldDoc(
        required = true,
        defaultValue = "",
        sensitive = true,
        help = "Login password of the database"
    )
    val password: String,

    @FieldDoc(
        required = true,
        defaultValue = "",
        help = "Fully qualified name of the JDBC driver"
    )
    val driver: String,

    @FieldDoc(
        required = true,
        defaultValue = "",
        help = "JDBC URL of the database"
    )
    val jdbcUrl: String,

    @FieldDoc(
        required = false,
        defaultValue = "GMT+8",
        help = "Default timezone of the JVM instance"
    )
    val timezone: String = "GMT+8",
)

fun JdbcSinkConfig.validate() {
    if (jdbcUrl.isEmpty()) {
        throw IllegalArgumentException("Required jdbc Url not set.")
    }
    if (driver.isEmpty()) {
        throw IllegalArgumentException("Required jdbc driver not set.")
    }
}

fun Map<String, Any?>.toJdbcSinkConfig(): JdbcSinkConfig {
    return Gson().let {
        it.fromJson(it.toJsonTree(this), JdbcSinkConfig::class.java)
    }
}