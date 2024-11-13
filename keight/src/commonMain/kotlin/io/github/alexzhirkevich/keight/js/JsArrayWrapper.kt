package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline
import kotlin.math.abs

@JvmInline
internal value class JsArrayWrapper(
    override val value : MutableList<Any?>
) : JSObject, JsWrapper<MutableList<Any?>>, MutableList<Any?> by value {

    override val values: List<Any?>
        get() = value.toList()

    override val entries: List<List<Any?>>
        get() = value.mapIndexed { index, v ->  kotlin.collections.listOf(index,v) }

    override fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
    }

    override fun descriptor(property: Any?): JSPropertyDescriptor? {
        TODO("Not yet implemented")
    }

    override val keys: List<String>
        get() = value.indices.map(Int::toString)

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Array.get(PROTOTYPE,runtime)
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        if (property == "length") {
            return value.size
        }

        val n = runtime.toNumber(property)

        if (!n.toDouble().isNaN()
            && (abs(n.toLong().toDouble() - n.toDouble()) < Float.MIN_VALUE)
            && n.toInt() in value.indices
        ) {
            return value[n.toInt()]
        }

        return super.get(property, runtime)
    }

    override fun toString(): String {
        return value.joinToString(separator = ",")
    }
}

