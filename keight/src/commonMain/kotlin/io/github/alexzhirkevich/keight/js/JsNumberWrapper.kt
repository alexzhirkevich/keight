package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

internal class JsNumberObject(
    val number: JsNumberWrapper
) : JsObjectImpl("Number"), Wrapper<Number> by number {

    override fun toString(): String {
        return number.toString()
    }

    override fun hashCode(): Int {
        return number.hashCode()
    }

    override fun toKotlin(runtime: ScriptRuntime): Any = number.toKotlin(runtime)
}

@JvmInline
internal value class JsNumberWrapper(
    override val value : Number
) : JsAny, Wrapper<Number>, Comparable<Wrapper<Number>> {

    override val type: String get() = "number"

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return (runtime.findRoot() as JSRuntime).Number.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(other: Wrapper<Number>): Int {
        return value.toDouble().compareTo(other.value.toDouble())
    }

    override fun toKotlin(runtime: ScriptRuntime): Any = value
}