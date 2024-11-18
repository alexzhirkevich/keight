package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.get

public interface JsAny {

    public val type : String get() = "object"

    public suspend fun keys(runtime: ScriptRuntime) : List<String> = emptyList()

    public suspend fun proto(runtime: ScriptRuntime) : Any? = Unit

    public suspend fun delete(property: Any?, runtime: ScriptRuntime): Boolean = true

    public suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
         return when (property) {
            "__proto__" -> proto(runtime)
            else -> {
                when(val proto = proto(runtime)) {
                    is JsAny -> proto.get(property, runtime)
                    else -> Unit
                }
            }
        }?.get(runtime)
    }

    public suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean =
        get(property, runtime) != Unit
}

internal suspend fun JsAny.isPrototypeOf(obj : Any?, runtime: ScriptRuntime) : Boolean {
    return isPrototypeOf(obj, runtime, true)
}

private suspend fun JsAny.isPrototypeOf(obj : Any?, runtime: ScriptRuntime, isFirst : Boolean = true) : Boolean {
    return when {
        !isFirst && obj === this -> true
        obj !is JsAny -> false
        else -> isPrototypeOf(obj.proto(runtime), runtime, false)
    }
}

