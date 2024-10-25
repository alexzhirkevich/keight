package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.argAtOrNull
import io.github.alexzhirkevich.keight.common.Callable
import io.github.alexzhirkevich.keight.common.Function
import io.github.alexzhirkevich.keight.es.ESAny
import io.github.alexzhirkevich.keight.es.ESClass
import io.github.alexzhirkevich.keight.es.ESObjectBase
import io.github.alexzhirkevich.keight.es.checkArgsNotNull
import io.github.alexzhirkevich.keight.es.unresolvedReference
import io.github.alexzhirkevich.keight.invoke
import kotlin.jvm.JvmInline
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal class JsNumberClass(
    val number: JsNumber
) : ESObjectBase("Number"),
    ESClass,
    JsWrapper<Number> by number,
    Comparable<JsWrapper<Number>> by number {

    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? = Unit

    override fun toString(): String {
        return number.toString()
    }

    override val name: String
        get() = "Number"

    override fun get(variable: Any?): Any? {
        return number[variable] ?: super<ESObjectBase>.get(variable)
    }

    override val functions: List<Function> get() = emptyList()

    override val construct: Function? get() = null

    override val extends: Expression = Expression {
        it["Number"]
    }

    override val constructorClass: Expression? get() = extends
}

@JvmInline
internal value class JsNumber(
    override val value : Number
) : ESAny, JsWrapper<Number>, Comparable<JsWrapper<Number>> {

    override val type: String get() = "number"

    override fun get(variable: Any?): Any? {
        return when(variable){
            "toFixed" -> ToFixed(value)
            "toPrecision" -> ToPrecision(value)
            else -> super.get(variable)
        }
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(other: JsWrapper<Number>): Int {
        return value.toDouble().compareTo(other.value.toDouble())
    }
}

@JvmInline
private value class ToPrecision(val value: Number) : Callable {
    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Double {
        val digits = args.argAtOrNull(0)?.invoke(runtime) ?: return value.toDouble()
        return value.toDouble().roundTo(runtime.toNumber(digits).toInt() - 1)
    }
}

@JvmInline
private value class ToFixed(val value: Number) : Callable {
    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
        val digits = args.argAtOrNull(0)?.invoke(runtime)?.let(runtime::toNumber)?.toInt() ?: 0

        if (digits == 0) {
            return value.toDouble().roundToLong().toString()
        }

        val stringNumber = value.toDouble().roundTo(digits).toString()

        val intPart = stringNumber.substringBefore(".")
        val floatPart = stringNumber.substringAfter(".", "").take(digits)

        if (floatPart.isBlank()) {
            return intPart
        }

        return (intPart + "." + floatPart.padEnd(digits, '0'))
    }
}

private val pow10 by lazy {
    (1..10).mapIndexed { i, it -> i to 10.0.pow(it) }.toMap()
}

private fun Double.roundTo(digit : Int) : Double {
    if(digit <= 0)
        return roundToLong().toDouble()

    val pow = pow10[digit-1] ?: return this
    return ((this * pow).roundToInt() / pow)
}
