package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsArrayWrapper(
    override val value : MutableList<Any?>
) : JsAny, JsWrapper<MutableList<Any?>>, MutableList<Any?> by value {

    override val keys: List<String>
        get() = value.indices.map(Int::toString)

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Array.get(PROTOTYPE,runtime)
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when (property) {
            "length" -> value.size
            else -> super.get(property, runtime)
        }
    }

    override fun toString(): String {
        return value.joinToString(separator = ",")
    }
}

