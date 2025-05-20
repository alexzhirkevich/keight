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
            val arg = it.getOrNull(0) ?: return@func false.js()
            val num = toNumber(arg).toDouble()
            if (num.isNaN())
                return@func false.js()
            num.isFinite().js()
        },
        "isInteger".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js()
            val num = toNumber(arg)
            (num is Long || num is Int || num is Short || num is Byte).js()
        },
        "parseInt".func(
            FunctionParam("number"),
            "radix" defaults OpConstant(10L.js())
        ) {
            val arg = it.getOrNull(0) ?: return@func false.js()
            val radix = it.getOrNull(1)?.let { toNumber(it) }
                ?.takeIf { !it.toDouble().isNaN() && it.toDouble().isFinite() }
                ?.toInt() ?: 10

            JSStringFunction.toString(arg, this).trim().trimParseInt(radix)?.js()
        },
        "isSafeInteger".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js()
            (toNumber(arg) is Long).js()
        },
        "parseFloat".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js()

            var dotCnt = 0
            var eCount = 0
            val num = arg.toString().trim().takeWhile { c ->
                (c.isDigit() || c == '.' && dotCnt == 0 || c == 'e' && eCount == 0).also {
                    if (c == '.') dotCnt++
                    if (c == 'e') eCount++
                }
            }
            (num.toDoubleOrNull() ?: 0L).js()
        },
        "isNaN".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js()
            toNumber(arg).toDouble().isNaN().js()
        },
    )
}

internal class JSNumberFunction : JSFunction(
    name = "Number",
    prototype =  Object {
        "toFixed".js().func(
            "digits" defaults OpConstant(0.js())
        ) { args ->
            val digits = args.getOrNull(0)?.let { toNumber(it) }?.toInt() ?: 0

            toNumber(thisRef).toFixed(digits, this).js()
        }
        "toPrecision".js().func("digits" defaults OpArgOmitted) { args ->
            val value = toNumber(thisRef)
            val digits = args.argOrElse(0) { return@func value.js() }
            value.toDouble().roundTo(toNumber(digits).toInt() - 1).js()
        }
        "toString".js().func("radix" defaults OpConstant(10.js())) { args ->

            val radix = if (args.isEmpty())
                10
            else args.getOrNull(0)?.let { toNumber(it) }?.toInt() ?: 10

            when(val number = toNumber(thisRef)){
                is Long -> number.toString(radix).js()
                else -> number.toString().js()
            }
        }
        "valueOf".js().func {
            when (val t = thisRef) {
                is JsNumberWrapper -> t.value.js()
                is JsNumberObject -> t.value.js()
                else -> t
            }
        }
    },
    parameters = listOf(FunctionParam("num")),
    body = Expression {
        it.toNumber(it.get("num".js())).js()
    },
) {
    init {
        numberMethods().forEach {
            defineOwnProperty(
                property = it.name.js(),
                value = it,
                writable = false,
                configurable = false,
                enumerable = false
            )
        }
        defineOwnProperty(
            "EPSILON".js(),
            JsNumberWrapper(Double.MIN_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MAX_SAFE_INTEGER".js(),
            JsNumberWrapper(Long.MAX_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MAX_VALUE".js(),
            JsNumberWrapper(Double.MAX_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MIN_VALUE".js(),
            JsNumberWrapper(Double.MIN_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MIN_SAFE_INTEGER".js(),
            JsNumberWrapper(Long.MIN_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "NaN".js(),
            JsNumberWrapper(Double.NaN),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "NEGATIVE_INFINITY".js(),
            JsNumberWrapper(Double.NEGATIVE_INFINITY),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "POSITIVE_INFINITY".js(),
            JsNumberWrapper(Double.POSITIVE_INFINITY),
            writable = false,
            configurable = false,
            enumerable = false
        )
    }

    override suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
        return obj !is JsNumberWrapper && super.isInstance(obj, runtime)
    }

    override suspend fun constructObject(args: List<JsAny?>, runtime: ScriptRuntime): JSObject {
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
