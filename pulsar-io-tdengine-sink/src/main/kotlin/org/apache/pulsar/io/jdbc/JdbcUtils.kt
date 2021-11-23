package org.apache.pulsar.io.jdbc

import com.google.gson.JsonElement
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

enum class JdbcDriver(val value: String) {
    H2("H2 JDBC Driver"),
    MYSQL("MySQL Connector/J"),
    MARIADB("MariaDB Connector/J"),
    POSTGRESQL("PostgreSQL JDBC Driver"),
    TDENGINE("TDengine JDBC Driver"),
}

enum class JdbcAction {
    INSERT,
    UPSERT,
    UPDATE,
    DELETE,
    SCHEMA,
}

data class JdbcColumn(
    val name: String,
    val type: Int,
    val isKey: Boolean,
    val nullable: Boolean,
    val unsigned: Boolean,
) {
    override fun toString(): String {
        return "JdbcColumn(name='$name', type=${JDBCType.valueOf(type).name}, isKey=$isKey, nullable=$nullable, unsigned=$unsigned)"
    }

    fun parseField(field: JsonElement?): JdbcField {
        val value: Any? = if (field == null || field.isJsonNull) null else when (type) {
            Types.BIT,
            Types.BOOLEAN -> field.asBoolean
            Types.TINYINT -> if (unsigned) field.asShort else field.asByte
            Types.SMALLINT -> if (unsigned) field.asInt else field.asShort
            Types.INTEGER -> if (unsigned) field.asLong else field.asInt
            Types.BIGINT -> if (unsigned) field.asBigInteger else field.asLong
            Types.FLOAT -> field.asFloat
            Types.REAL,
            Types.DOUBLE -> field.asDouble
            Types.NUMERIC,
            Types.DECIMAL -> field.asBigDecimal
            Types.CHAR,
            Types.VARCHAR,
            Types.LONGVARCHAR -> field.asString
            Types.BINARY,
            Types.VARBINARY,
            Types.LONGVARBINARY -> Base64.getDecoder().decode(field.asString)
            Types.CLOB -> TODO()
            Types.BLOB -> TODO()
            Types.DATE -> LocalDate.parse(field.asString)
            Types.TIME -> LocalTime.parse(field.asString)
            Types.TIMESTAMP -> LocalDateTime.parse(field.asString)
            Types.TIME_WITH_TIMEZONE -> TODO()
            Types.TIMESTAMP_WITH_TIMEZONE -> TODO()
            else -> TODO()
        }
        return JdbcField(name, type, isKey, value)
    }
}

data class JdbcTable(
    val catalog: String,
    val schema: String,
    val name: String,
    val columns: List<JdbcColumn>,
) {
    val keys: List<JdbcColumn> by lazy { columns.filter(JdbcColumn::isKey) }

    fun hasKey(c: String): Boolean = keys.any { it.name == c }

    fun hasColumn(c: String): Boolean = columns.any { it.name == c }

    fun parseFields(entity: JsonElement): List<JdbcField> = columns.filter { entity.asJsonObject.has(it.name) }.map {
        it.parseField(entity.asJsonObject.get(it.name))
    }
}

data class JdbcField(
    val name: String,
    val type: Int,
    val isKey: Boolean,
    val value: Any?,
) {
    override fun toString(): String {
        return "JdbcField(name='$name', type=${JDBCType.valueOf(type).name}, isKey=$isKey, value=$value)"
    }
}

fun DatabaseMetaData.loadTable(target: String): JdbcTable {
    val p = target.split('.')
    val c = if (p.size > 1) p[0] else null ?: connection.catalog
    val s = if (p.size > 2) p[1] else null ?: connection.schema
    val t = p.last()
    return getTables(c, s, t, arrayOf("TABLE")).use {
        if (it.next()) {
            if (!it.isLast) {
                throw IllegalArgumentException("Implicit table of target \"$target\"")
            }
            val catalog = it.getString(1) ?: c
            val schema = it.getString(2) ?: s
            val name = it.getString(3)
            val keys = getPrimaryKeys(catalog, schema, name).use {
                generateSequence { if (it.next()) it.getString(4) else null }.toList()
            }
            val cols = getColumns(catalog, schema, name, null).use {
                generateSequence {
                    if (it.next()) JdbcColumn(
                        it.getString(4),
                        it.getInt(5),
                        keys.contains(it.getString(4)),
                        it.getString(18) == "YES",
                        it.getString(6).contains("UNSIGNED", true)
                    ) else null
                }.toList()
            }
            JdbcTable(catalog, schema ?: "", name, cols)
        } else {
            throw IllegalArgumentException("Implicit table of target \"$target\"")
        }
    }
}

//region SQL Helpers

fun DatabaseMetaData.quoting(vararg args: String): String {
    val quote = identifierQuoteString.orEmpty().ifBlank { "" }
    return args.filter(String::isNotEmpty).joinToString(separator = catalogSeparator) { "$quote$it$quote" }
}

// quoting
private fun Connection.q(vararg args: String): String = metaData.quoting(*args)

// table identifier
private fun Connection.t(table: JdbcTable): String = q(table.catalog, table.schema, table.name)

// key list
private fun Connection.k(table: JdbcTable): String = table.keys.joinToString { q(it.name) }

// key predicate
private fun Connection.p(table: JdbcTable): String = table.keys.joinToString { "${q(it.name)} = ?" }

//endregion

fun Connection.buildSQL(target: String, action: JdbcAction, entity: JsonElement, sqlMode: Set<String>): String {
    if (action == JdbcAction.SCHEMA) {
        return entity.asJsonObject.get("ddl").asString
    }
    val table = metaData.loadTable(target)
    val fields = entity.asJsonObject.entrySet().filter { table.hasColumn(it.key) }
    val nonKeys = fields.filterNot { table.hasKey(it.key) }
    val ignoreInvalid = sqlMode.contains("INSERT_IGNORE_INVALID")
    return when (action) {
        JdbcAction.INSERT -> {
            when (metaData.driverName) {
                JdbcDriver.MYSQL.value,
                JdbcDriver.MARIADB.value,
                JdbcDriver.TDENGINE.value ->
                    "INSERT${if (ignoreInvalid) " IGNORE" else ""} INTO ${t(table)} (${fields.joinToString { q(it.key) }}) VALUES (${fields.joinToString { "?" }})"
                else ->
                    "INSERT INTO ${t(table)} (${fields.joinToString { q(it.key) }}) VALUES (${fields.joinToString { "?" }})"
            }
        }

        JdbcAction.UPSERT -> {
            when (metaData.driverName) {
                JdbcDriver.H2.value -> {
                    "MERGE INTO ${t(table)} (${fields.joinToString { q(it.key) }}) VALUES (${fields.joinToString { "?" }})"
                }
                JdbcDriver.MYSQL.value,
                JdbcDriver.MARIADB.value -> {
                    "INSERT INTO ${t(table)} (${fields.joinToString { q(it.key) }}) VALUES (${fields.joinToString { "?" }}) ON DUPLICATE KEY UPDATE ${
                        nonKeys.joinToString {
                            "${
                                q(
                                    it.key
                                )
                            }=?"
                        }
                    }"
                }
                JdbcDriver.POSTGRESQL.value -> {
                    "INSERT INTO ${t(table)} (${fields.joinToString { q(it.key) }}) VALUES (${fields.joinToString { "?" }}) ON CONFLICT(${
                        k(
                            table
                        )
                    }) DO UPDATE SET ${nonKeys.joinToString { "${q(it.key)}=?" }}"
                }
                else -> {
                    throw java.lang.IllegalArgumentException("Unsupported action $action with JDBC driver ${metaData.driverName}")
                }
            }
        }
        JdbcAction.UPDATE -> {
            "UPDATE ${t(table)} SET ${nonKeys.joinToString { "${q(it.key)} = ?" }} WHERE ${p(table)}"
        }
        JdbcAction.DELETE -> {
            "DELETE FROM ${t(table)} WHERE ${p(table)}"
        }
        JdbcAction.SCHEMA -> { // Never
            ""
        }
    }
}
//region Param Binders
fun PreparedStatement.setDate(index: Int, date: LocalDate) = setDate(index, java.sql.Date.valueOf(date))
fun PreparedStatement.setTime(index: Int, time: LocalTime) = setTime(index, Time.valueOf(time))
fun PreparedStatement.setTimestamp(index: Int, datetime: LocalDateTime) = setTimestamp(index, Timestamp.valueOf(datetime))
fun PreparedStatement.setParam(index: Int, field: JdbcField) {
    if (field.value == null) setNull(index, field.type) else when (field.type) {
        Types.BIT,
        Types.BOOLEAN -> setBoolean(index, field.value as Boolean)
        Types.TINYINT -> if (field.value is Short) setShort(index, field.value) else setByte(index, field.value as Byte)
        Types.SMALLINT -> if (field.value is Int) setInt(index, field.value) else setShort(index, field.value as Short)
        Types.INTEGER -> if (field.value is Long) setLong(index, field.value) else setInt(index, field.value as Int)
        Types.BIGINT -> if (field.value is BigInteger) setBigDecimal(index, field.value.toBigDecimal()) else setLong(
            index,
            field.value as Long
        )
        Types.FLOAT -> setFloat(index, field.value as Float)
        Types.REAL,
        Types.DOUBLE -> setDouble(index, field.value as Double)
        Types.NUMERIC,
        Types.DECIMAL -> setBigDecimal(index, field.value as BigDecimal)
        Types.CHAR,
        Types.VARCHAR,
        Types.LONGVARCHAR -> setString(index, field.value as String)
        Types.BINARY,
        Types.VARBINARY,
        Types.LONGVARBINARY -> setBytes(index, field.value as ByteArray)
        Types.CLOB -> TODO()
        Types.BLOB -> TODO()
        Types.DATE -> setDate(index, field.value as LocalDate)
        Types.TIME -> setTime(index, field.value as LocalTime)
        Types.TIMESTAMP -> setTimestamp(index, field.value as LocalDateTime)
        Types.TIME_WITH_TIMEZONE -> TODO()
        Types.TIMESTAMP_WITH_TIMEZONE -> TODO()
        else -> TODO()
    }
}
fun PreparedStatement.setParams(args: List<JdbcField>, offset: Int = 0) = args.forEachIndexed { index, field ->
    setParam(1 + index + offset, field)
}
//endregion
fun PreparedStatement.bindValue(target: String, action: JdbcAction, entity: JsonElement) {
    if (action == JdbcAction.SCHEMA) {
        return
    }
    val table = connection.metaData.loadTable(target)
    val fields = table.parseFields(entity)
    val keys = fields.filter(JdbcField::isKey)
    val nonKeys = fields.filterNot(JdbcField::isKey)
    when (action) {
        JdbcAction.INSERT -> {
            setParams(fields)
        }
        JdbcAction.UPSERT -> {
            when (connection.metaData.driverName) {
                JdbcDriver.H2.value -> {
                    setParams(fields)
                }
                JdbcDriver.MYSQL.value,
                JdbcDriver.MARIADB.value,
                JdbcDriver.POSTGRESQL.value -> {
                    setParams(fields)
                    setParams(nonKeys, fields.size)
                }
                else -> {
                    throw java.lang.IllegalArgumentException("Unsupported action $action with JDBC driver ${connection.metaData.driverName}")
                }
            }
        }
        JdbcAction.UPDATE -> {
            setParams(nonKeys)
            setParams(keys, nonKeys.size)
        }
        JdbcAction.DELETE -> {
            setParams(keys)
        }
        JdbcAction.SCHEMA -> { // Never
        }
    }
}