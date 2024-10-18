package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.es.ESObject
import io.github.alexzhirkevich.keight.es.Object
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsJSON() : ESObject {

    return Object("JSON") {
        "parse".func("text") {
            Json.parseToJsonElement(it[0].toString()).toJs()
        }
        "stringify".func("value") {
            stringify(it[0], this)
        }
    }
}

private fun stringify(value : Any?, runtime: ScriptRuntime) : String {
    return when(value){
        is String -> Json.encodeToString<String>(value)
        is Number -> Json.encodeToString<Number>(value)
        is List<*> -> Json.encodeToString<List<*>>(value)
        is JsWrapper<*> -> stringify(value.value, runtime)
        is ESObject -> Json.encodeToString<JsonElement>(value.toJsonObject(runtime))
        else -> ""
    }
}

private fun Any?.toJsonObject(runtime: ScriptRuntime) : JsonElement {
    return when (this) {
        null -> JsonNull
        is JsWrapper<*> -> value.toJsonObject(runtime)
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is List<*> -> JsonArray(map { it.toJsonObject(runtime) })
        is ESObject -> JsonObject(keys.associateWith {
            runtime.toKotlin(get(it)).toJsonObject(runtime)
        })

        else -> JsonNull
    }
}

private fun JsonElement.toJs() : Any? {

    return when (this) {
        is JsonPrimitive -> longOrNull ?: doubleOrNull ?: content
        is JsonArray -> map { it.toJs() }
        is JsonObject -> Object("", mapValues { it.value.toJs() }.toMutableMap())
        JsonNull -> null
        else -> Unit
    }
}