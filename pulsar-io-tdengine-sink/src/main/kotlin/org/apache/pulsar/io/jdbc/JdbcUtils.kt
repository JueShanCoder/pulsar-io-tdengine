package org.apache.pulsar.io.jdbc

import com.alibaba.fastjson.JSON
import com.google.gson.JsonElement
import org.apache.commons.codec.binary.Base64
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.*
import java.time.*

enum class JdbcDriver(val value: String) {
    TDENGINE("com.taosdata.jdbc.TSDBDriver"),
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
    val nullable: Boolean,
    val unsigned: Boolean,
) {
    override fun toString(): String {
        return "JdbcColumn(name='$name', type=${JDBCType.valueOf(type).name}, nullable=$nullable, unsigned=$unsigned)"
    }

    fun parseField(field: JsonElement?): JdbcField {
        JdbcSink.LOGGER.info("field $field")
        JdbcSink.LOGGER.info("type $type")
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
            Types.LONGVARCHAR,
            Types.NCHAR -> field.asString
            Types.BINARY,
            Types.VARBINARY,
            Types.LONGVARBINARY -> Base64.decodeBase64(field.asString)
            Types.CLOB -> TODO()
            Types.BLOB -> TODO()
            Types.DATE -> LocalDate.parse(field.asString)
            Types.TIME -> LocalTime.parse(field.asString)
            Types.TIMESTAMP ->  field.asString
            Types.TIME_WITH_TIMEZONE -> TODO()
            Types.TIMESTAMP_WITH_TIMEZONE -> TODO()
            else -> TODO()
        }
        return JdbcField(name, type, value)
    }
}

data class JdbcTable(
    val catalog: String,
    val schema: String,
    val name: String,
    val columns: List<JdbcColumn>,
    val sColumns: List<JdbcColumn>,
) {
    fun parseFields(entity: JsonElement): List<JdbcField> = columns.filter { entity.asJsonObject.has(it.name) }.map {
        it.parseField(entity.asJsonObject.get(it.name))
    }

    fun parseSFields(entity: JsonElement): List<JdbcField> = sColumns.filter { entity.asJsonObject.has(it.name) }.map {
        it.parseField(entity.asJsonObject.get(it.name))
    }
}

data class JdbcField(
    val name: String,
    val type: Int,
    val value: Any?,
) {
    override fun toString(): String {
        return "JdbcField(name='$name', type=${JDBCType.valueOf(type).name}, value=$value)"
    }
}

fun DatabaseMetaData.loadTable(target: String): JdbcTable {
    val p = target.split('.')
    val c = if (p.size > 1) p[0] else null ?: connection.catalog
    val s = null
    val t = p.last()
    return getTables(c, null, t, arrayOf("TABLE")).use {
        if (it.next()) {
            val catalog = it.getString(1) ?: c
            val schema = it.getString(2) ?: s
            val name = it.getString(3)
            val columns = getColumns(catalog, schema, name, null)
            val cols =  mutableListOf<JdbcColumn>()
            val sCols =  mutableListOf<JdbcColumn>()
            columns.use {
                while (it.next()){
                    if (it.getString(12) == null) {
                        cols.add(
                            JdbcColumn(
                            it.getString(4),
                            it.getInt(5),
                            it.getString(18) == "YES",
                            it.getString(6).contains("UNSIGNED", true),
                            )
                        )
                    } else {
                        sCols.add(
                            JdbcColumn(
                            it.getString(4),
                            it.getInt(5),
                            it.getString(18) == "YES",
                            it.getString(6).contains("UNSIGNED", true),
                            )
                        )
                    }
                }
            }
            JdbcSink.LOGGER.info("cols: ${JSON.toJSONString(cols)}, sCols: ${JSON.toJSONString(sCols)} ")
            JdbcTable(catalog, schema ?: "", name, cols, sCols)
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

//endregion
fun Connection.buildSQL(target: String, action: JdbcAction, entity: JsonElement): String {
    val p = target.split('.')
    val stable = if (p.size > 2) p[1] else null
    JdbcSink.LOGGER.info("stable $stable")
    val table = metaData.loadTable(target)
    val fields = table.parseFields(entity)
    val sFields = table.parseSFields(entity)

    return when (action) {
        JdbcAction.INSERT -> {
            JdbcSink.LOGGER.info("metaData.driverName ${metaData.driverName}")
            when (metaData.driverName) {
                JdbcDriver.TDENGINE.value ->
                    if (stable != null)
                        "INSERT INTO ${t(table)} USING $stable (${sFields.joinToString { q(it.name) }}) TAGS (${sFields.joinToString { "?" }}) (${fields.joinToString { q(it.name) }}) VALUES (${fields.joinToString { "?" }})"
                    else
                        "INSERT INTO ${t(table)} (${fields.joinToString { q(it.name) }}) VALUES (${fields.joinToString { "?" }})"
                else ->
                    "INSERT INTO ${t(table)} (${fields.joinToString { q(it.name) }}) VALUES (${fields.joinToString { "?" }})"
            }
        }

        JdbcAction.UPSERT -> {
            ""
        }
        JdbcAction.UPDATE -> {
            ""
        }
        JdbcAction.DELETE -> {
            ""
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
    JdbcSink.LOGGER.info("=======  index $index ,field ${field.name}")
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
        Types.LONGVARCHAR,
        Types.NCHAR -> setString(index, field.value as String)
        Types.BINARY,
        Types.VARBINARY,
        Types.LONGVARBINARY -> setBytes(index, field.value as ByteArray)
        Types.CLOB -> TODO()
        Types.BLOB -> TODO()
        Types.DATE -> setDate(index, field.value as LocalDate)
        Types.TIME -> setTime(index, field.value as LocalTime)
        Types.TIMESTAMP ->  setString(index, field.value as String)
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
    val sFields = table.parseSFields(entity)
    when (action) {
        JdbcAction.INSERT -> {
            setParams(sFields)
            setParams(fields,sFields.size)
        }
        JdbcAction.UPSERT -> {

        }
        JdbcAction.UPDATE -> {

        }
        JdbcAction.DELETE -> {
        }
        JdbcAction.SCHEMA -> { // Never
        }
    }
}