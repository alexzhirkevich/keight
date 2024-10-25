package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke

internal fun  OpMakeArray(
    items : List<Expression>
) = Expression { context ->
    items.fastMap { it(context) }.toMutableList()
}