package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression

internal fun OpCompare(
    a : Expression,
    b : Expression,
    result : (Int) -> Boolean
) = Expression {

    val ta = a(it)
    val tb = b(it)

    if (it.isComparable(ta, tb)) {
        result(it.compare(ta, tb))
    } else {
        false
    }
}

internal fun OpNot(
    condition : Expression,
) = Expression {
    it.isFalse(condition(it))
}
