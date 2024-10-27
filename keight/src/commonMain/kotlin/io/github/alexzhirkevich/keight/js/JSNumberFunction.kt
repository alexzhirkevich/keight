package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.argAtOrNull
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.invoke
import kotlin.jvm.JvmInline
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal fun JSObject.setupNumberMethods() {
    func("isFinite", "number") {
        val arg = it.getOrNull(0) ?: return@func false
        val num = toNumber(arg).toDouble()
        if (num.isNaN())
            return@func false
        num.isFinite()
    }

    func("isInteger", "number") {
        val arg = it.getOrNull(0) ?: return@func false
        val num = toNumber(arg)
        num is Long || num is Int || num is Short || num is Byte
    }

    func(
        "parseInt",
        FunctionParam("number"),
        "radix" defaults OpConstant(10L),
    ) {
        val arg = it.getOrNull(0) ?: return@func false
        val radix = it.getOrNull(1)?.let(::toNumber)
            ?.takeIf { !it.toDouble().isNaN() && it.toDouble().isFinite() }
            ?.toInt() ?: 10

        arg.toString().trim().trimParseInt(radix)
    }

    func(
        "parseInt",
        FunctionParam("number"),
        "radix" defaults OpConstant(10L),
    ) {
        val arg = it.getOrNull(0) ?: return@func false
        val radix = it.getOrNull(1)?.let(::toNumber)
            ?.takeIf { !it.toDouble().isNaN() && it.toDouble().isFinite() }
            ?.toInt() ?: 10

        arg.toString().trim().trimParseInt(radix)
    }

    func("isSafeInteger", "number") {
        val arg = it.getOrNull(0) ?: return@func false
        val num = toNumber(arg)
        num is Long || num is Int || num is Short || num is Byte
    }

    func("parseFloat","number") {
        val arg = it.getOrNull(0) ?: return@func false

        var dotCnt = 0
        val num = arg.toString().trim().takeWhile { c ->
            (c.isDigit() || c == '.' && dotCnt == 0).also {
                if (c == '.') dotCnt++
            }
        }
        num.toDoubleOrNull() ?: 0L
    }
    func("isNaN","number") {
        val arg = it.getOrNull(0) ?: return@func false
        toNumber(arg).toDouble().isNaN()
    }
}

internal class JSNumberFunction : JSFunction(
    name = "Number",
    parameters = listOf(FunctionParam("number", default = OpConstant(0L))),
    body = OpConstant(Unit)
) {

    init {
        val proto = JSObjectImpl().apply {
            this["toFixed"] = ToFixed(0)
            this["toPrecision"] = ToPrecision(0)
        }

        setPrototype(proto)

        set("EPSILON", Double.MIN_VALUE)
        set("MAX_SAFE_INTEGER", Long.MAX_VALUE)
        set("MAX_VALUE", Double.MAX_VALUE)
        set("MIN_SAFE_INTEGER", Long.MIN_VALUE)
        set("NaN", Double.NaN)
        set("NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY)
        set("POSITIVE_INFINITY", Double.POSITIVE_INFINITY)

        setupNumberMethods()
    }

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Number {
        return if (args.isEmpty()){
            runtime.toNumber(0)
        } else {
            runtime.toNumber(args.first().invoke(runtime))
        }
    }

    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): JsNumberObject {
        return JsNumberObject(JsNumberWrapper(invoke(args, runtime)))
    }


    @JvmInline
    private value class ToPrecision(val value: Number) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Double {
            val digits = args.argAtOrNull(0)?.invoke(runtime) ?: return value.toDouble()
            return value.toDouble().roundTo(runtime.toNumber(digits).toInt() - 1)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return ToPrecision(args.firstNumberOrZero(runtime))
        }
    }

    @JvmInline
    private value class ToFixed(val value: Number) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
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

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return ToFixed(args.firstNumberOrZero(runtime))
        }
    }
}

private suspend fun List<Expression>.firstNumberOrZero(runtime: ScriptRuntime) : Number {
    return getOrNull(0)?.invoke(runtime)?.let(runtime::toNumber) ?: 0
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
