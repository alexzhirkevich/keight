package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.fastSumBy
import io.github.alexzhirkevich.keight.js.interpreter.checkArgs

import kotlin.math.*
import kotlin.random.Random

internal fun JSMath() : JSObject = Object("Math") {
    "PI".eq(JsNumberWrapper(PI), writable = false, configurable = false)
    "E".eq(JsNumberWrapper(E), writable = false, configurable = false)
    "LN10".eq(JsNumberWrapper(2.302585092994046), writable = false, configurable = false)
    "LN2".eq(JsNumberWrapper(0.6931471805599453), writable = false, configurable = false)
    "LOG10E".eq(JsNumberWrapper(0.4342944819032518), writable = false, configurable = false)
    "LOG2E".eq(JsNumberWrapper(1.4426950408889634), writable = false, configurable = false)
    "SQRT1_2".eq(JsNumberWrapper(0.7071067811865476), writable = false, configurable = false)
    "SQRT2".eq(JsNumberWrapper(1.4142135623730951), writable = false, configurable = false)

    "abs".func("x") { op1(it, ::acos) }
    "asoc".func("x") { op1(it, ::acos) }
    "asoch".func("x") { op1(it, ::acosh) }
    "asin".func("x") { op1(it, ::asin) }
    "asinh".func("x") { op1(it, ::asinh) }
    "atan".func("x") { op1(it, ::atan) }
    "atan2".func("y", "x") { op2(it, ::atan2) }
    "atanh".func("x") { op1(it, ::atanh) }
    "cbrt".func("x") { op1(it, ::cbrt) }
    "ceil".func("x") { op1(it, ::ceil) }
    "cos".func("x") { op1(it, ::cos) }
    "cosh".func("x") { op1(it, ::cosh) }
    "exp".func("x") { op1(it, ::exp) }
    "expm1".func("x") { op1(it, ::expm1) }
    "floor".func("x") { op1(it, ::floor) }
    "hypot".func(
        "values",
        params = { FunctionParam(it, isVararg = true) }
    ) { opVararg(it, { 0 }, ::hypotN) }
    "imul".func("x", "y",) { op2(it, ::imul) }
    "log".func("x") { op1(it, ::ln) }
    "log10".func("x") { op1(it, ::log10) }
    "log1p ".func("x") { op1(it, ::ln1p) }
    "log2".func("x") { op1(it, ::log2) }
    "max".func(
        "values",
        params = { FunctionParam(it, isVararg = true) }
    ) {
        opVararg(it, { Double.NEGATIVE_INFINITY }, List<Double>::max)
    }
    "min".func(
        "values",
        params = { FunctionParam(it, isVararg = true) }
    ) { opVararg(it, { Double.POSITIVE_INFINITY }, List<Double>::min) }
    "pow".func("x", "y",) { op2(it, Double::pow) }
    "random".func("x") { Expression { Random.nextDouble() } }
    "round".func("x") { op1(it, ::round) }
    "sign".func("x") { op1(it, ::sign) }
    "sin".func("x") { Sin(it.firstOrNull() ?: Unit) }
    "sinh".func("x") { op1(it, ::sinh) }
    "sqrt".func("x") { op1(it, ::sqrt) }
    "tan".func("x") { op1(it, ::tan) }
    "tanh".func("x") { op1(it, ::tanh) }
    "trunc".func("x") { op1(it, ::truncate) }
}

/**
 * 21.3.2.30
 *
 * [Math.sin(x)](https://tc39.es/ecma262/#sec-math.sin)
 *
 * This function returns the sine of x. The argument is expressed in radians.
 **
 * It performs the following steps when called:
 *
 * 1. Let n be ? ToNumber(x).
 * 2. If n is one of NaN, +0𝔽, or -0𝔽, return n.
 * 3. If n is either +∞𝔽 or -∞𝔽, return NaN.
 * 4. Return an implementation-approximated Number value representing the sine of ℝ(n).
 * */
private suspend inline fun ScriptRuntime.Sin(value : Any?) : Number {
    return sin(toNumber(value).toDouble())
}

private suspend fun ScriptRuntime.op1(
    args: List<Any?>,
    func: (Double) -> Number
): Any? {
    return func(toNumber(args.getOrNull(0) ?: 0).toDouble())
}

private suspend fun  ScriptRuntime.op2(
    args: List<Any?>,
    func: (Double, Double) -> Number,
): Any? {
    checkArgs(args, 2, "")

    val a = toNumber(args[0]).toDouble()
    val b = toNumber(args[1]).toDouble()
    return func(a, b)
}

private suspend fun ScriptRuntime.opVararg(
    args: List<Any?>,
    onEmpty : () -> Number,
    func: (List<Double>) -> Number,
): Any? {

    if (args.isEmpty() || (args[0] as List<*>).isEmpty()){
        return onEmpty()
    }
    val a = (args[0] as List<*>).fastMap {
        toNumber(it).toDouble()
    }
    return func(a)
}

private fun hypotN(args : List<Double>): Double {
    return sqrt(args.fastSumBy { it * it })
}

private fun imul(x : Double, y : Double) : Long {
    val a = x.toLong().toInt()
    val b = y.toLong().toInt()

    val ah = (a ushr 16) and 0xffff
    val al = a and 0xffff
    val bh = (b ushr 16) and 0xffff
    val bl = b and 0xffff

    return ((al * bl) + ((((ah * bl) + (al * bh)) shl 16) ushr 0) or 0).toLong()
}
