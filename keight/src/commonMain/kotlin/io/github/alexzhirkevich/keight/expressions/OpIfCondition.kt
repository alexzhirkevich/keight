package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.js.Undefined


internal fun OpIfCondition(
    condition : Expression,
    onTrue : Expression? = null,
    onFalse : Expression? = null,
    expressible : Boolean = false
) = Expression {
    val expr = if (it.isFalse(condition(it))) onFalse else onTrue

    val res = expr?.invoke(it)

    if (expressible) {
        res
    } else {
        Undefined
    }
}