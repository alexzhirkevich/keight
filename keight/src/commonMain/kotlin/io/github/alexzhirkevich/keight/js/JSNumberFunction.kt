package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal fun numberMethods() : List<JSFunction> {
    return listOf(
        "isFinite".func("number") {
            val arg = it.getOrNull(0) ?: return@func false
            val num = toNumber(arg).toDouble()
            if (num.isNaN())
                return@func false
            num.isFinite()
        },
        "isInteger".func("number") {
            val arg = it.getOrNull(0) ?: return@func false
            val num = toNumber(arg)
            num is Long || num is Int || num is Short || num is Byte
        },
        "parseInt".func(
            FunctionParam("number"),
            "radix" defaults OpConstant(10L)
        ) {
            val arg = it.getOrNull(0) ?: return@func false
            val radix = it.getOrNull(1)?.let(::toNumber)
                ?.takeIf { !it.toDouble().isNaN() && it.toDouble().isFinite() }
                ?.toInt() ?: 10

            JSStringFunction.toString(arg, this).trim().trimParseInt(radix)
        },
        "isSafeInteger".func("number") {
            val arg = it.getOrNull(0) ?: return@func false
            toNumber(arg) is Long
        },
        "parseFloat".func("number") {
            val arg = it.getOrNull(0) ?: return@func false

            var dotCnt = 0
            var eCount = 0
            val num = arg.toString().trim().takeWhile { c ->
                (c.isDigit() || c == '.' && dotCnt == 0 || c == 'e' && eCount == 0).also {
                    if (c == '.') dotCnt++
                    if (c == 'e') eCount++
                }
            }
            num.toDoubleOrNull() ?: 0L
        },
        "isNaN".func("number") {
            val arg = it.getOrNull(0) ?: return@func false
            toNumber(arg).toDouble().isNaN()
        },
    )
}

internal class JSNumberFunction : JSFunction(
    name = "Number",
    prototype =  Object {
        "toFixed".func("digits" defaults OpConstant(0)) { args ->
            val digits = args.getOrNull(0)?.let(::toNumber)?.toInt() ?: 0
            toNumber(thisRef).toFixed(digits, this)
        }
        "toPrecision".func("digits" defaults OpArgOmitted) { args ->
            val value = toNumber(thisRef)
            val digits = args.argOrElse(0) { return@func value }
            value.toDouble().roundTo(toNumber(digits).toInt() - 1)
        }
        "toString".func("radix" defaults OpConstant(10)) { args ->

            val radix = if (args.isEmpty())
                10
            else args.getOrNull(0)?.let(::toNumber)?.toInt() ?: 10

            when(val number = toNumber(thisRef)){
                is Long -> number.toString(radix)
                else -> number.toString()
            }
        }
    },
    parameters = listOf(FunctionParam("num")),
    body = Expression {
        it.toNumber(it.get("num"))
    },
) {
    init {
        numberMethods().forEach {
            defineOwnProperty(
                property = it.name,
                value = it,
                writable = false,
                configurable = false,
                enumerable = false
            )
        }
        defineOwnProperty(
            "EPSILON",
            JsNumberWrapper(Double.MIN_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MAX_SAFE_INTEGER",
            JsNumberWrapper(Long.MAX_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MAX_VALUE",
            JsNumberWrapper(Double.MAX_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MIN_SAFE_INTEGER",
            JsNumberWrapper(Long.MIN_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "NaN",
            JsNumberWrapper(Double.NaN),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "NEGATIVE_INFINITY",
            JsNumberWrapper(Double.NEGATIVE_INFINITY),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "POSITIVE_INFINITY",
            JsNumberWrapper(Double.POSITIVE_INFINITY),
            writable = false,
            configurable = false,
            enumerable = false
        )
    }

    override suspend fun isInstance(obj: Any?, runtime: ScriptRuntime): Boolean {
        return obj !is JsNumberWrapper && super.isInstance(obj, runtime)
    }

    override suspend fun constructObject(args: List<Any?>, runtime: ScriptRuntime): JSObject {
        return JsNumberObject(JsNumberWrapper(runtime.toNumber(args.getOrNull(0))))
    }
}

private fun Number.toFixed(digits: Int, runtime: ScriptRuntime) : String {
    if (digits == 0) {
        return toDouble().roundToLong().toString()
    }

    val stringNumber = toDouble().roundTo(digits).toString()

    val intPart = stringNumber.substringBefore(".")
    val floatPart = stringNumber.substringAfter(".", "").take(digits)

    if (floatPart.isBlank()) {
        return intPart
    }

    return (intPart + "." + floatPart.padEnd(digits, '0'))
}

private fun String.trimParseInt(radix : Int) : Long? {

    return when (radix) {
        0 -> if (startsWith("0x",true)){
            trimParseInt(16)
        } else {
            trimParseInt(10)
        }
        10 -> takeWhile { it.isDigit() }.toLongOrNull()
        8 -> {
            takeWhile { it.isDigit() && it in '0'..'7' }
                .toLongOrNull(radix)
        }

        2 -> {
            takeWhile { it.isDigit() && it in '0'..'1' }
                .toLongOrNull(radix)
        }

        16 -> {
            if (!startsWith("0x") && !startsWith("0X"))
                null
            else {
                drop(2)
                    .takeWhile { (it in '0'..'9' || it.lowercaseChar() in 'a'..'f') }
                    .toLongOrNull(radix)
            }
        }
        else -> null
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
