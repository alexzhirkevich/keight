package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
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

        if (property == runtime.fromKotlin("__proto__")){
            return proto(runtime)?.get(runtime)
        }

        // When property is found in prototype chain, the result is JsPropertyAccessor
        // which needs to be resolved with thisArg (the original object, not the prototype)
        return when (val result = protoChainGet(property, runtime, this)) {
            is JsPropertyAccessor -> result.getAccessor(this, runtime)
            else -> result
        }
    }

    /**
     * Internal method for prototype chain lookup with explicit thisArg.
     * The thisArg should always be the original object (receiver) from which
     * the property access was initiated, not the prototype object.
     */
    public suspend fun protoChainGet(property: JsAny?, runtime: ScriptRuntime, thisArg: JsAny?): JsAny? {

        if (property == runtime.fromKotlin("__proto__")){
            return proto(runtime)?.get(runtime)
        }

        val result = when(val proto = proto(runtime)) {
            is Undefined -> Undefined
            is JsAny -> proto.protoChainGet(property, runtime, thisArg)
            else -> Undefined
        }

        // When property is found in prototype chain, the result is JsPropertyAccessor
        // which needs to be resolved with thisArg (the original object, not the prototype)
        return when (result) {
            is JsPropertyAccessor -> result.getAccessor(thisArg, runtime)
            else -> result
        }
    }

    public suspend fun contains(property: JsAny?, runtime: ScriptRuntime): Boolean =
        get(property, runtime) != Undefined

    public fun toKotlin(runtime: ScriptRuntime): Any = this
}

public suspend fun JsAny.isPrototypeOf(obj : Any?, runtime: ScriptRuntime) : Boolean {
    return isPrototypeOf(obj, runtime, true)
}

private suspend fun JsAny.isPrototypeOf(obj : Any?, runtime: ScriptRuntime, isFirst : Boolean) : Boolean {
    return when {
        !isFirst && obj === this -> true
        obj !is JsAny -> false
        else -> {
            val proto = obj.proto(runtime)
            proto !== obj && isPrototypeOf(proto, runtime, false)
        }
    }
}
