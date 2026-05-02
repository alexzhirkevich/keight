package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal fun numberMethods() : List<JSFunction> {
    return listOf(
        "isFinite".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js
            val num = toNumber(arg).toDouble()
            if (num.isNaN())
                return@func false.js
            num.isFinite().js
        },
        "isInteger".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js
            val num = toNumber(arg).toDouble()
            (num.isFinite() && num % 1.0 == 0.0).js
        },
        "parseInt".func(
            FunctionParam("number"),
            "radix" defaults OpConstant(10L.js)
        ) {
            val arg = it.getOrNull(0) ?: return@func false.js
            val radix = it.getOrNull(1)?.let { toNumber(it) }
                ?.takeIf { it.toDouble().isFinite() }
                ?.toInt() ?: 10

            toString(arg).trim().trimParseInt(radix)?.js
        },
        "isSafeInteger".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js
            val num = toNumber(arg).toDouble()
            (num.isFinite() && num % 1.0 == 0.0 &&
                abs(num) <= Constants.MAX_SAFE_INTEGER).js
        },
        "parseFloat".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js

            var dotCnt = 0
            var eCount = 0
            var prev : Char? = null
            val num = arg.toString().trim().takeWhile { c ->
                when {
                    c.isDigit() -> true
                    c == '.' && dotCnt == 0 -> {
                        dotCnt++
                        true
                    }
                    (c == 'e' || c == 'E') && eCount == 0 -> {
                        eCount++
                        true
                    }
                    (c == '-' || c == '+') && (prev == null || prev == 'e' || prev == 'E') -> true
                    else -> false
                }.also { prev = c }
            }
            (num.toDoubleOrNull() ?: 0L).js
        },
        "isNaN".func("number") {
            val arg = it.getOrNull(0) ?: return@func false.js
            toNumber(arg).toDouble().isNaN().js
        },
    )
}

internal class JSNumberFunction : JSFunction(
    name = "Number",
    prototype =  Object {
        "toFixed".js.func(
            "digits" defaults OpConstant(0.js)
        ) { args ->
            val digits = args.getOrNull(0)?.let { toNumber(it) }?.toInt() ?: 0

            toNumber(thisRef).toFixed(digits, this).js
        }
        "toPrecision".js.func("digits" defaults OpArgOmitted) { args ->
            val value = toNumber(thisRef)
            val digits = args.argOrElse(0) { return@func value.js }
            value.toDouble().roundTo(toNumber(digits).toInt() - 1).js
        }
        Constants.toString.js.func("radix" defaults OpConstant(10.js)) { args ->

            val radix = if (args.isEmpty())
                10
            else args.getOrNull(0)?.let { toNumber(it) }?.toInt() ?: 10

            when(val number = toNumber(thisRef)){
                is Long -> number.toString(radix).js
                else -> number.toString().js
            }
        }
        Constants.valueOf.js.func {
            when (val t = thisRef) {
                is JsNumberWrapper -> t.value.js
                is JsNumberObject -> t.value.js
                else -> t
            }
        }
    },
    parameters = listOf(FunctionParam("num")),
    body = Expression {
        it.toNumber(it.get("num".js)).js
    },
) {
    init {
        numberMethods().forEach {
            defineOwnProperty(
                property = it.name.js,
                value = it,
                writable = false,
                configurable = false,
                enumerable = false
            )
        }
        defineOwnProperty(
            "EPSILON".js,
            JsNumberWrapper(Double.MIN_VALUE),
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MAX_SAFE_INTEGER".js,
            Constants.MAX_SAFE_INTEGER.js,
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MAX_VALUE".js,
            Double.MAX_VALUE.js,
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MIN_VALUE".js,
            Double.MIN_VALUE.js,
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "MIN_SAFE_INTEGER".js,
            Long.MIN_VALUE.js,
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "NaN".js,
            Double.NaN.js,
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "NEGATIVE_INFINITY".js,
            Double.NEGATIVE_INFINITY.js,
            writable = false,
            configurable = false,
            enumerable = false
        )
        defineOwnProperty(
            "POSITIVE_INFINITY".js,
            Double.POSITIVE_INFINITY.js,
            writable = false,
            configurable = false,
            enumerable = false
        )
    }

    override suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
        return obj !is JsNumberWrapper && super.isInstance(obj, runtime)
    }

    override suspend fun constructObject(args: List<JsAny?>, runtime: ScriptRuntime): JsObject {
        return JsNumberObject(JsNumberWrapper(runtime.toNumber(args.getOrNull(0))))
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return constructObject(args, runtime).also {
            it.setProto(runtime, get(PROTOTYPE, runtime))
        }
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

internal fun Number.sameValueZero(other : Number) : Boolean {
    if (this is Double && isNaN() && other is Double && other.isNaN()){
        return true
    }

    if (this == 0.0 && other == -0.0 || this == -0.0 && other == 0.0){
        return true
    }

    return this == other || toDouble() == other.toDouble()
}
