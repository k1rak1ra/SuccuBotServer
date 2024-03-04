package net.k1ra.succubotserver.feature.api

import com.google.gson.*
import java.time.*
import java.time.format.DateTimeFormatter

object GsonProvider {
    val gson: Gson = GsonBuilder().registerTypeAdapter(
        LocalDateTime::class.java,
        JsonDeserializer { json, _, _ ->
            val instant = Instant.ofEpochMilli(json.asJsonPrimitive.asLong)
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        }).registerTypeAdapter(
        LocalDateTime::class.java,
        JsonSerializer { date: LocalDateTime, _, _ ->
            JsonPrimitive(date.toInstant(ZoneOffset.UTC).toEpochMilli())
        }).registerTypeAdapter(
        LocalTime::class.java,
        JsonDeserializer { json, _, _ ->
            val tacc = DateTimeFormatter.ISO_TIME.parse(json.asJsonPrimitive.asString)
            LocalTime.from(tacc)
        }).registerTypeAdapter(
        LocalTime::class.java,
        JsonSerializer { time: LocalTime, _, _ ->
            JsonPrimitive(time.format(DateTimeFormatter.ISO_TIME))
        }).create()
}