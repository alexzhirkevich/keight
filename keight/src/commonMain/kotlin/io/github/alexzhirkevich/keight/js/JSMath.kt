package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.fastSumBy
import io.github.alexzhirkevich.keight.js.interpreter.checkArgs

import kotlin.math.*
import kotlin.random.Random

internal fun JSMath() : JsObject = Object("Math") {
    "PI".js.eq(JsNumberWrapper(PI), enumerable = true, writable = false, configurable = false)
    "E".js.eq(JsNumberWrapper(E), enumerable = true, writable = false, configurable = false)
    "LN10".js.eq(2.302585092994046.js, enumerable = true, writable = false, configurable = false)
    "LN2".js.eq(0.6931471805599453.js, enumerable = true, writable = false, configurable = false)
    "LOG10E".js.eq(0.4342944819032518.js, enumerable = true, writable = false, configurable = false)
    "LOG2E".js.eq(1.4426950408889634.js, enumerable = true, writable = false, configurable = false)
    "SQRT1_2".js.eq(0.7071067811865476.js,enumerable = true, writable = false, configurable = false)
    "SQRT2".js.eq(1.4142135623730951.js, enumerable = true, writable = false, configurable = false)

    "abs".js.func("x") { op1(it, ::acos) }
    "asoc".js.func("x") { op1(it, ::acos) }
    "asoch".js.func("x") { op1(it, ::acosh) }
    "asin".js.func("x") { op1(it, ::asin) }
    "asinh".js.func("x") { op1(it, ::asinh) }
    "atan".js.func("x") { op1(it, ::atan) }
    "atan2".js.func("y", "x") { op2(it, ::atan2) }
    "atanh".js.func("x") { op1(it, ::atanh) }
    "cbrt".js.func("x") { op1(it, ::cbrt) }
    "ceil".js.func("x") { op1(it, ::ceil) }
    "cos".js.func("x") { op1(it, ::cos) }
    "cosh".js.func("x") { op1(it, ::cosh) }
    "exp".js.func("x") { op1(it, ::exp) }
    "expm1".js.func("x") { op1(it, ::expm1) }
    "floor".js.func("x") { op1(it, ::floor) }
    "hypot".js.func(
        "values",
        params = { it.vararg() }
    ) { opVararg(it, { 0 }, ::hypotN) }
    "imul".js.func("x", "y",) { op2(it, ::imul) }
    "log".js.func("x") { op1(it, ::ln) }
    "log10".js.func("x") { op1(it, ::log10) }
    "log1p ".js.func("x") { op1(it, ::ln1p) }
    "log2".js.func("x") { op1(it, ::log2) }
    "max".js.func(
        "values",
        params = { it.vararg() }
    ) {
        opVararg(it, { Double.NEGATIVE_INFINITY }, List<Double>::max)
    }
    "min".js.func(
        "values",
        params = { it.vararg() }
    ) { opVararg(it, { Double.POSITIVE_INFINITY }, List<Double>::min) }
    "pow".js.func("x", "y",) { op2(it, Double::pow) }
    "random".js.func("x") {  Random.nextDouble().js }
    "round".js.func("x") { op1(it, ::round) }
    "sign".js.func("x") { op1(it, ::sign) }
    "sin".js.func("x") { Sin(it.firstOrNull() ?: Undefined).js }
    "sinh".js.func("x") { op1(it, ::sinh) }
    "sqrt".js.func("x") { op1(it, ::sqrt) }
    "tan".js.func("x") { op1(it, ::tan) }
    "tanh".js.func("x") { op1(it, ::tanh) }
    "trunc".js.func("x") { op1(it, ::truncate) }
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
private suspend inline fun ScriptRuntime.Sin(value : JsAny?) : Number {
    return sin(toNumber(value).toDouble())
}

private suspend fun ScriptRuntime.op1(
    args: List<JsAny?>,
    func: (Double) -> Number
): JsAny? {
    return func(toNumber(args.getOrElse(0) { 0.js }).toDouble()).js
}

private suspend fun  ScriptRuntime.op2(
    args: List<JsAny?>,
    func: (Double, Double) -> Number,
): JsAny? {
    checkArgs(args, 2, "")

    val a = toNumber(args[0]).toDouble()
    val b = toNumber(args[1]).toDouble()
    return func(a, b).js
}

private suspend fun ScriptRuntime.opVararg(
    args: List<JsAny?>,
    onEmpty : () -> Number,
    func: (List<Double>) -> Number,
): JsAny {

    if (args.isEmpty() || (args[0] as List<*>).isEmpty()){
        return onEmpty().js
    }
    val a = (args[0] as List<JsAny?>).fastMap {
        toNumber(it).toDouble()
    }
    return func(a).js
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
