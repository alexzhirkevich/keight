package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline
import kotlin.math.pow

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

    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(other: JsWrapper<Number>): Int {
        return value.toDouble().compareTo(other.value.toDouble())
    }
}
