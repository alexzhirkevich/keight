package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot

import kotlin.jvm.JvmInline

internal class JsNumberObject(
    val number: JsNumberWrapper
) : JSObjectImpl("Number"),
    JsWrapper<Number> by number,
    Comparable<JsWrapper<Number>> by number {

    override fun toString(): String {
        return number.toString()
    }

    override val name: String
        get() = "Number"

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return number.get(property, runtime) ?: super.get(property, runtime)
    }
}

@JvmInline
internal value class JsNumberWrapper(
    override val value : Number
) : JsAny, JsWrapper<Number>, Comparable<JsWrapper<Number>> {

    override val type: String get() = "number"

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Number.get(PROTOTYPE, runtime)
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when (property){
            "toString" -> ToString(value)
            else -> super.get(property, runtime)
        }
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(other: JsWrapper<Number>): Int {
        return value.toDouble().compareTo(other.value.toDouble())
    }

    @JvmInline
    private value class ToString(val number: Number) : Callable {

        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val radix = if (args.isEmpty())
                10
            else args.getOrNull(0)?.let(runtime::toNumber)?.toInt() ?: 10

            return when(number){
                is Long -> number.toString(radix)
                else -> number.toString()
            }
        }

        override suspend fun bind(
            thisArg: Any?,
            args: List<Any?>,
            runtime: ScriptRuntime
        ): Callable {
            return ToString(runtime.toNumber(thisArg))
        }
    }
}