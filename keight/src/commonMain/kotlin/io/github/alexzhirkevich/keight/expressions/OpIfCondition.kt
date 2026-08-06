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

    // Branches are wrapped in an `OpBlock` with `isExpressible = false` by the parser, which
    // would discard the branch's last statement value. For completion-value semantics (Issue #23)
    // we must evaluate the taken branch as expressible so its value propagates.
    val res = expr?.asExpressible()?.invoke(it)

    if (expressible) res ?: Undefined else Undefined
}