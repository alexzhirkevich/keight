package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline
import kotlin.math.abs

@JvmInline
internal value class JsArrayWrapper(
    override val value : MutableList<Any?>
) : JSObject, Wrapper<MutableList<Any?>>, MutableList<Any?> by value {

    override suspend fun values(runtime: ScriptRuntime): List<Any?> {
        return value.toList()
    }

    override suspend fun entries(runtime: ScriptRuntime): List<List<Any?>> {
        return value.mapIndexed { index, v ->  listOf(index,v) }
    }

    override suspend fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime
    ) {
        this.value[runtime.toNumber(value).toInt()] = property
    }

    override fun descriptor(property: Any?): JSObject? = null

    override suspend fun keys(runtime: ScriptRuntime): List<String> {
        return value.indices.map(Int::toString)
    }

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Array.get(PROTOTYPE,runtime)
    }

    override suspend fun hasOwnProperty(name: Any?, runtime: ScriptRuntime): Boolean {
        val idx = (runtime.toKotlin(name) as? String)?.toIntOrNull()
            ?: return super.hasOwnProperty(name, runtime)

        return idx in value.indices || super.hasOwnProperty(name, runtime)
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

