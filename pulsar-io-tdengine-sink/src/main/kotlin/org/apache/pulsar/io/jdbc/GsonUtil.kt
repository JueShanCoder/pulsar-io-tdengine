package org.apache.pulsar.io.jdbc

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.Temporal

class TemporalAdapter : JsonSerializer<Temporal>, JsonDeserializer<Temporal> {
    override fun serialize(temporal: Temporal, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(temporal.toString())
    }

    override fun deserialize(element: JsonElement, type: Type, context: JsonDeserializationContext): Temporal {
        return when (type) {
            LocalTime::class.java -> LocalTime.parse(element.asString)
            LocalDate::class.java -> LocalDate.parse(element.asString)
            LocalDateTime::class.java -> LocalDateTime.parse(element.asString)
            else -> throw JsonParseException("Unsupported temporal type: $type")
        }
    }
}

fun GsonBuilder.registerTemporalAdapter(): GsonBuilder =
    this.registerTypeHierarchyAdapter(Temporal::class.java, TemporalAdapter())