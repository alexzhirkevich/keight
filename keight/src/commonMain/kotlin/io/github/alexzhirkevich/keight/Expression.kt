package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.OpGetProperty
import io.github.alexzhirkevich.keight.expressions.OpIndex


public fun interface Expression {

    /**
     * Evaluate IR expression.
     *
     * Should not be called directly because the return value is usually a raw Kotlin object.
     * Use [invoke] instead to receive wrapped runtime-compatible result
     * */
    public suspend fun invokeRaw(runtime: ScriptRuntime): Any?
}

public suspend operator fun Expression.invoke(runtime: ScriptRuntime): Any? =
    runtime.fromKotlin(invokeRaw(runtime))


internal fun Expression.isAssignable() : Boolean {
    return this is OpGetProperty ||
            this is OpIndex && property is OpGetProperty
}


internal fun  List<Expression>.argForNameOrIndex(
    index : Int,
    vararg name : String,
) : Expression? {

    return argAtOrNull(index)
//    forEach { op ->
//        if (op is OpAssign && name.any { op.variableName == it }) {
//            return op.assignableValue
//        }
//    }
//
//    return argAtOrNull(index)
}

internal fun  List<Expression>.argAt(
    index : Int,
) : Expression {

    return get(index)
//        .let {
//        if (it is OpAssign)
//            it.assignableValue
//        else it
//    }
}

internal fun  List<Expression>.argAtOrNull(
    index : Int,
) : Expression? {

    return getOrNull(index)
//        /**/.let {
//        if (it is OpAssign)
//            it.assignableValue
//        else it
//    }
}