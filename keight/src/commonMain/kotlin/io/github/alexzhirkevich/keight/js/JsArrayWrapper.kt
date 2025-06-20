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
        return value.mapIndexed { index, v ->  listOf(index.js, v) }
    }

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> {
        return value.indices.toList().fastMap { it.js }
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
        return isValidIndex(name, runtime) != null || super.hasOwnProperty(name, runtime)
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        if (property == length) {
            return value.size.js
        }

        isValidIndex(property, runtime)?.let {
            return value[it]
        }

        return super.get(property, runtime)
    }

    override fun toString(): String {
        return value.joinToString(separator = ",")
    }

    override fun toKotlin(runtime: ScriptRuntime): Any = value.map { it?.toKotlin(runtime) }

    private suspend fun isValidIndex(index : JsAny?, runtime: ScriptRuntime) : Int? {
        if (index == null)
            return null

        val n = try {
            runtime.toNumber(index)
        } catch (t : Throwable){
            return null
        }

        val nDouble = n.toDouble()
        val nLong = n.toLong()
        if (
            nDouble.isNaN().not()
            && (abs(nLong - nDouble) < Double.MIN_VALUE)
            && nLong in value.indices
        ) {
            return n.toInt()
        }
        return null
    }
}

private val length = "length".js

