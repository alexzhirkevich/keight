package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Uninitialized
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.findJsRoot
import kotlin.math.abs

internal class JsArrayWrapper(
    override val value : MutableList<JsAny?>
) : JsObjectImpl(), Wrapper<MutableList<JsAny?>>, MutableList<JsAny?> by value {

    override suspend fun values(runtime: ScriptRuntime): List<JsAny?> {
        return super.values(runtime) + value.toList()
    }

    override suspend fun entries(runtime: ScriptRuntime): List<List<JsAny?>> {
        return super.entries(runtime) + value.mapIndexed { index, v ->  listOf(index.js, v) }
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
        isValidIndex(property, runtime)?.let {
            if (it > this.value.lastIndex) {
                repeat(it - this.value.lastIndex) {
                    add(Uninitialized)
                }
            }
            this.value[it] = value
        } ?: super.set(property, value, runtime)
    }


    override suspend fun fallbackProto(runtime: ScriptRuntime): JsAny? {
        return runtime.findJsRoot().Array.get(PROTOTYPE,runtime)
    }

    override suspend fun contains(property: JsAny?, runtime: ScriptRuntime): Boolean {
        return super.contains(property, runtime) && get(property, runtime) !== Uninitialized
    }

    override suspend fun hasOwnProperty(name: JsAny?, runtime: ScriptRuntime): Boolean {
        val idx = isValidIndex(name, runtime)
            ?.takeIf { it in value.indices }
            ?: return super.hasOwnProperty(name, runtime)

        return value[idx] !== Uninitialized
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        if (property == length) {
            return value.size.js
        }

        isValidIndex(property, runtime)?.takeIf { it in value.indices }?.let {
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
            && nLong >= 0
        ) {
            return n.toInt()
        }
        return null
    }
}

private val length = "length".js

