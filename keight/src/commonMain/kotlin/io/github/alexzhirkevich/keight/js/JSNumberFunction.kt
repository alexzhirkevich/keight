package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.invoke

internal class JSNumberFunction : JSFunction(
    name = "Number",
    parameters = listOf(FunctionParam("number", default = OpConstant(0L))),
    body = OpConstant(Unit)
) {

    val isFinite by func("number") {
        val arg = it.getOrNull(0) ?: return@func false
        val num = toNumber(arg).toDouble()
        if (num.isNaN())
            return@func false
        num.isFinite()
    }

    val isInteger by func("number") {
        val arg = it.getOrNull(0) ?: return@func false
        val num = toNumber(arg)
        num is Long || num is Int || num is Short || num is Byte
    }

    val parseInt by func(
        FunctionParam("number"),
        "radix" defaults OpConstant(10L)
    ) {
        val arg = it.getOrNull(0) ?: return@func false
        val radix = it.getOrNull(1)?.let(::toNumber)
            ?.takeIf { !it.toDouble().isNaN() && it.toDouble().isFinite() }
            ?.toInt() ?: 10

        arg.toString().trim().trimParseInt(radix)
    }

    val isSafeInteger by func("number") {
        val arg = it.getOrNull(0) ?: return@func false
        val num = toNumber(arg)
        num is Long || num is Int || num is Short || num is Byte
    }

    val parseFloat by func("number") {
        val arg = it.getOrNull(0) ?: return@func false

        var dotCnt = 0
        val num = arg.toString().trim().takeWhile { c ->
            (c.isDigit() || c == '.' && dotCnt == 0).also {
                if (c == '.') dotCnt++
            }
        }
        num.toDoubleOrNull() ?: 0L
    }

    val isNan by func("number") {
        val arg = it.getOrNull(0) ?: return@func false
        toNumber(arg).toDouble().isNaN()
    }

    override fun get(variable: Any?): Any? {
        return when (variable) {
            "EPSILON" -> Double.MIN_VALUE
            "length" -> Double.MIN_VALUE
            "MAX_SAFE_INTEGER" -> Long.MAX_VALUE
            "MAX_VALUE" -> Double.MAX_VALUE
            "MIN_SAFE_INTEGER" -> Long.MIN_VALUE
            "NaN" -> Double.NaN
            "NEGATIVE_INFINITY" -> Double.NEGATIVE_INFINITY
            "POSITIVE_INFINITY" -> Double.POSITIVE_INFINITY
            else -> super.get(variable)
        }
    }

    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Number {
        return if (args.isEmpty()){
            runtime.toNumber(0)
        } else {
            runtime.toNumber(args.first().invoke(runtime))
        }
    }

    override fun construct(args: List<Expression>, runtime: ScriptRuntime): JsNumberObject {
        return JsNumberObject(JsNumberWrapper(invoke(args, runtime)))
    }
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
