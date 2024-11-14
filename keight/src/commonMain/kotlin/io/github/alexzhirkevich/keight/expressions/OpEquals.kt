package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsWrapper
import kotlin.math.sign

internal fun OpEquals(
    a : Expression,
    b : Expression,
    isTyped : Boolean
) = Expression {
    OpEqualsImpl(a(it), b(it), isTyped, it)
}

internal fun OpNotEquals(
    a : Expression,
    b : Expression,
    isTyped : Boolean
) = Expression {
    !OpEqualsImpl(a(it), b(it), isTyped, it)
}

private val listOfPositiveZero = listOf(0.0)

internal tailrec fun OpEqualsImpl(a : Any?, b : Any?, typed : Boolean, runtime: ScriptRuntime) : Boolean {

    return when {
        !typed && a is JsWrapper<*> -> OpEqualsImpl(a.value, b, typed, runtime)
        !typed && b is JsWrapper<*> -> OpEqualsImpl(a, b.value, typed, runtime)
        a == null || b == null -> a == b
        typed -> a::class == b::class && OpEqualsImpl(a, b, false, runtime)
        a is Number && b is Number -> {
            val ad = a.toDouble()
            val bd = b.toDouble()
            (ad == bd && !ad.isNaN())
        }
        a is Number -> OpEqualsImpl(a, runtime.toNumber(b), typed, runtime)
        b is Number -> OpEqualsImpl(b, runtime.toNumber(a), typed, runtime)
//        a::class == b::class -> a == b
        else -> a.toString() == b.toString()
    }
}