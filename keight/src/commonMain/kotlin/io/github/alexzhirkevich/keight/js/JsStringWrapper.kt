package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

internal class JsStringObject(
    override val value : JsStringWrapper
) : JSObjectImpl("Number"), Wrapper<Wrapper<String>>, CharSequence by value {

    override fun toString(): String  = value.value
}


@JvmInline
internal value class JsStringWrapper(
    override val value : String
) : JsAny, Wrapper<String>, Comparable<JsStringWrapper>, CharSequence by value {

    override val type: String
        get() = "string"

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).String.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return value
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when(property){
            "length" -> value.length.toLong()
            else -> super.get(property, runtime)
        }
    }

    override fun compareTo(other: JsStringWrapper): Int {
        return value.compareTo(other.value)
    }
}




