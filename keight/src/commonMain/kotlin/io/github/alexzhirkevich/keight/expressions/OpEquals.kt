package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.js.JSBooleanWrapper
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JSSymbol
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.js.JsStringWrapper
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js

internal fun OpEquals(
    a : Expression,
    b : Expression,
    isTyped : Boolean
) = Expression {
    OpEqualsImpl(a(it), b(it), isTyped, it).js()
}

internal fun OpNotEquals(
    a : Expression,
    b : Expression,
    isTyped : Boolean
) = Expression {
    if (isTyped){
        OpEqualsImpl(a(it), b(it), isTyped, it).not().js()
    } else {
        OpEqualsImpl(a(it), b(it), isTyped, it).not().js()
    }
}

private suspend fun OpStrictEqualsImpl(a : JsAny?, b : JsAny?, runtime: ScriptRuntime) : Boolean {
    if (a?.type != b?.type) {
        return false
    }

    if (a is JSObject && b is JSObject) {
        return a === b
    }

    if (a == null && b == null || a == Undefined && b == Undefined) {
        return true
    }

    if (a is JsNumberWrapper) {
        val ad = runtime.toNumber(a).toDouble()
        val bd = runtime.toNumber(b as JsAny).toDouble()
        return (ad == bd && !ad.isNaN())
    }

    return a == b
}

private tailrec suspend fun OpLooselyEqualsImpl(a : JsAny?, b : JsAny?,  runtime: ScriptRuntime) : Boolean {

    return when {
        // If one of the operands is null or undefined, the other must also be null or undefined
        // to return true. Otherwise return false.
        a == null || b == null || a == Undefined || b == Undefined ->
            a == b || a == null && b == Undefined || a == Undefined && b == null

        // If the operands have the same type, they are compared as follows:
        a::class == b::class -> when (a) {
            // String: return true only if both operands have the same characters in the same order.
            is JsStringWrapper -> a.toString() == b.toString()
            // Number: return true only if both operands have the same value. +0 and -0 are treated
            // as the same value. If either operand is NaN, return false; so, NaN is never equal to NaN.
            is JsNumberWrapper -> {
                val ad = runtime.toNumber(a).toDouble()
                val bd = runtime.toNumber(b as JsAny).toDouble()
                (ad == bd && !ad.isNaN())
            }
            // Boolean: return true only if operands are both true or both false.
            is JSBooleanWrapper -> a == b
            // Symbol: return true only if both operands reference the same symbol.
            is JSSymbol -> a.value == (b as JSSymbol).value
            // Object: return true only if both operands reference the same object.
            else -> a === b
        }
        // If one of the operands is an object and the other is a primitive, convert the object to a primitive.
        a is JSObject || b is JSObject -> with(runtime.findRoot() as JSRuntime) {
            if (a is JSObject) {
                OpLooselyEqualsImpl(a.ToPrimitive(), b, runtime)
            } else {
                OpLooselyEqualsImpl(a, b.ToPrimitive(), runtime)
            }
        }

        // At this step, both operands are converted to primitives (one of String, Number, Boolean,
        // Symbol, and BigInt). The rest of the conversion is done case-by-case.

        // If one of the operands is a Symbol but the other is not, return false.
        a is JSSymbol != b is JSSymbol -> false

        //If one of the operands is a Boolean but the other is not, convert the boolean to a number:
        // true is converted to 1, and false is converted to 0. Then compare the two operands loosely again.
        a is JSBooleanWrapper -> OpLooselyEqualsImpl(if (a.value) 1.js() else 0.js(), b, runtime)
        b is JSBooleanWrapper -> OpLooselyEqualsImpl(a, if (b.value) 1.js() else 0.js(), runtime)

        // Number to String: convert the string to a number. Conversion failure results in NaN, which will guarantee the equality to be false.
        a is JsNumberWrapper && b is JsStringWrapper || b is JsNumberWrapper && a is JsStringWrapper -> {
            val ad = runtime.toNumber(a).toDouble()
            val bd = runtime.toNumber(b).toDouble()
            (ad == bd && !ad.isNaN())
        }
        else -> JSStringFunction.toString(a, runtime) == JSStringFunction.toString(b, runtime)
    }
}

internal tailrec suspend fun OpEqualsImpl(a : JsAny?, b : JsAny?, typed : Boolean, runtime: ScriptRuntime) : Boolean {

    return if (typed) OpStrictEqualsImpl(a,b,runtime) else OpLooselyEqualsImpl(a,b,runtime)

//    return when {
//        a == null || b == null -> a == b
//        !typed && a is Number && b is JsAny -> OpEqualsImpl(a, runtime.toNumber(b), typed, runtime)
//        !typed && a is Wrapper<*> -> OpEqualsImpl(a.value, b, typed, runtime)
//        !typed && b is Number && a is JsAny-> OpEqualsImpl(b, runtime.toNumber(a), typed, runtime)
//        !typed && b is Wrapper<*> -> OpEqualsImpl(a, b.value, typed, runtime)
//        typed -> a::class == b::class && OpEqualsImpl(a, b, false, runtime)
//        a is List<*> && b is List<*> -> a === b
//        a is String && a.isEmpty() && b == false -> true
//        b is String && b.isEmpty() && a == false -> true
//        a is Number && b is Number -> {
//            val ad = a.toDouble()
//            val bd = b.toDouble()
//            (ad == bd && !ad.isNaN())
//        }
//        a is Number && b is JsAny -> OpEqualsImpl(a, runtime.toNumber(b), typed, runtime)
//        b is Number && a is JsAny-> OpEqualsImpl(b, runtime.toNumber(a), typed, runtime)
////        a::class == b::class -> a == b
//        else -> a.toString() == b.toString()
//    }
}