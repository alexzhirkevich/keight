package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression

internal fun OpLogicalAnd(
    a : Expression,
    b : Expression
) = Expression {
    val av = a(it)
    if (it.isFalse(av)) {
        return@Expression av
    }
    b(it)
}

internal fun OpLogicalOr(
    a : Expression,
    b : Expression
) = Expression {
    val av = a(it)
    if (!it.isFalse(av)) {
        return@Expression av
    }
    b(it)
}