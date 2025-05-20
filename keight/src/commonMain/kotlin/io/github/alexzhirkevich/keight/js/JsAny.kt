package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.get

public interface JsAny {

    public val type : String get() = "object"

    public suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean = true,
        excludeNonEnumerables: Boolean = true
    ): List<JsAny?> = emptyList()

    public suspend fun proto(runtime: ScriptRuntime): JsAny? = Undefined

    public suspend fun delete(property: JsAny?, runtime: ScriptRuntime): Boolean = true

    public suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {

        if (property is JsStringWrapper && property.toString() == "__proto__"){
            return proto(runtime)?.get(runtime)
        }
        return when(val proto = proto(runtime)) {
            is Undefined -> Undefined
            is JsAny -> proto.get(property, runtime)
            else -> Undefined
        }?.get(runtime)

    }

    public suspend fun contains(property: JsAny?, runtime: ScriptRuntime): Boolean =
        get(property, runtime) != Undefined
}

internal suspend fun JsAny.isPrototypeOf(obj : Any?, runtime: ScriptRuntime) : Boolean {
    return isPrototypeOf(obj, runtime, true)
}

private suspend fun JsAny.isPrototypeOf(obj : Any?, runtime: ScriptRuntime, isFirst : Boolean) : Boolean {
    return when {
        !isFirst && obj === this -> true
        obj !is JsAny -> false
        else -> isPrototypeOf(obj.proto(runtime), runtime, false)
    }
}

