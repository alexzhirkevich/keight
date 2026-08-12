package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.js.Undefined


internal fun OpIfCondition(
    condition : Expression,
    onTrue : Expression? = null,
    onFalse : Expression? = null,
    expressible : Boolean = false
) : Expression {
    // Branches are wrapped in an `OpBlock` with `isExpressible = false` by the parser, which
    // would discard the branch's last statement value. For completion-value semantics (Issue #23)
    // we must evaluate the taken branch as expressible so its value propagates. This is done here,
    // at construction time, rather than inside the executor lambda.
    val onTrueE = onTrue?.asExpressible()
    val onFalseE = onFalse?.asExpressible()
    return Expression { r ->
        val expr = if (r.isFalse(condition(r))) onFalseE else onTrueE
        val res = expr?.invoke(r)
        if (expressible) res ?: Undefined else Undefined
    }
}