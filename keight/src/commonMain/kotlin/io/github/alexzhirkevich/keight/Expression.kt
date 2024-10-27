package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.OpGetVariable
import io.github.alexzhirkevich.keight.expressions.OpIndex


public fun interface Expression {

    public suspend fun invokeRaw(context: ScriptRuntime): Any?
}

public suspend operator fun Expression.invoke(context: ScriptRuntime): Any? =
    context.fromKotlin(invokeRaw(context))


internal fun Expression.isAssignable() : Boolean {
    return this is OpGetVariable && assignmentType == null ||
            this is OpIndex && variable is OpGetVariable
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