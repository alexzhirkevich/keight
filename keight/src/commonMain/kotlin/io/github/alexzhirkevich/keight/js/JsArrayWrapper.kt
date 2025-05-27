package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline
import kotlin.math.abs

@JvmInline
internal value class JsArrayWrapper(
    override val value : MutableList<JsAny?>
) : JSObject, Wrapper<MutableList<JsAny?>>, MutableList<JsAny?> by value {

    override suspend fun values(runtime: ScriptRuntime): List<JsAny?> {
        return value.toList()
    }

    override suspend fun entries(runtime: ScriptRuntime): List<List<JsAny?>> {
        return value.mapIndexed { index, v ->  listOf(index.js(), v) }
    }

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> {
        return value.indices.toList().fastMap { it.js() }
    }

    override suspend fun set(
        property: JsAny?,
        value: JsAny?,
        runtime: ScriptRuntime
    ) {
        this.value[runtime.toNumber(property).toInt()] = value
    }


    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return (runtime.findRoot() as JSRuntime).Array.get(PROTOTYPE,runtime)
    }

    override suspend fun hasOwnProperty(name: JsAny?, runtime: ScriptRuntime): Boolean {
        val idx = (name?.toKotlin(runtime) as? String)?.toIntOrNull()
            ?: return super.hasOwnProperty(name, runtime)

        return idx in value.indices || super.hasOwnProperty(name, runtime)
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        if (property.toString() == "length") {
            return value.size.js()
        }

        val n = try {
            runtime.toNumber(property)
        } catch (t : Throwable){
            return super.get(property, runtime)
        }
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

    override fun toKotlin(runtime: ScriptRuntime): Any = value.map { it?.toKotlin(runtime) }
}

