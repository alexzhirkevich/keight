package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

internal class JsStringObject(
    override val value : JsStringWrapper
) : JsObjectImpl("String"), Wrapper<Wrapper<String>>, CharSequence by value {

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is CharSequence && other.toString() == toString()
    }

    override fun toString(): String = value.toString()

    override fun toKotlin(runtime: ScriptRuntime): Any = value.toKotlin(runtime)
}

@JvmInline
internal value class JsStringWrapper(
    override val value : String
) : JsAny, Wrapper<String>, Comparable<JsStringWrapper>, CharSequence by value {

    override val type: String
        get() = "string"

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return runtime.findJsRoot().String.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return value
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when(property) {
            "length".js -> value.length.toLong().js
            CONSTRUCTOR -> runtime.findJsRoot().String
            else -> super.get(property, runtime)
        }
    }

    override fun compareTo(other: JsStringWrapper): Int {
        return value.compareTo(other.value)
    }

    override fun toKotlin(runtime: ScriptRuntime): Any = value
}

internal fun JsAny.isString() : Boolean {
    return this is JsStringWrapper || this is JSStringFunction
}

