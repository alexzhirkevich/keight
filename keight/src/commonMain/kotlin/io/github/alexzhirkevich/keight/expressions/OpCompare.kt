package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.js.js

internal fun OpCompare(
    a : Expression,
    b : Expression,
    result : (Int) -> Boolean
) = Expression {

    val ta = a(it)
    val tb = b(it)

    if (ta?.toKotlin(it).let { it as? Number }?.toDouble()?.isNaN() == true){
        return@Expression false.js
    }
    if (tb?.toKotlin(it).let { it as? Number }?.toDouble()?.isNaN() == true){
        return@Expression false.js
    }

//    if (it.isComparable(ta, tb)) {
        result(it.compare(ta, tb)).js
//    } else {
//        false.js
//    }
}

internal fun OpNot(
    condition : Expression,
) = Expression {
    it.isFalse(condition(it)).js
}
