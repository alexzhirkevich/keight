package io.github.alexzhirkevich.keight.common

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.invoke

internal fun OpLongInt(
    a : Expression,
    b : Expression,
    op : (Long, Int) -> Long
) = Expression {
    val an = it.toNumber(a(it)).toLong()
    val bn = it.toNumber(b(it)).toInt()

    op(an, bn)
}

internal fun OpLongLong(
    a : Expression,
    b : Expression,
    op: (Long, Long) -> Long
)  = Expression{
    val an = it.toNumber(a(it)).toLong()
    val bn = it.toNumber(b(it)).toLong()

    op(an, bn)
}