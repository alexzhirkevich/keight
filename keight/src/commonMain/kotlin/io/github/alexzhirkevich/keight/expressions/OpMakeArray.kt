package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke

internal fun OpMakeArray(
    items : List<Expression>
) = Expression { context ->
    buildList {
        items.fastForEach { i ->
            val value = i(context)
            if (i is OpSpread && value is Iterable<*>) {
                addAll(value as Iterable<Any?>)
            } else {
                add(value)
            }
        }
    }
}