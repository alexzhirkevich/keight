package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.js.js
import kotlin.math.pow

internal fun OpExp(
    x : Expression,
    degree : Expression
) = Expression {
    val xn = it.toNumber(x(it)).toDouble()
    val degreeN = it.toNumber(degree(it)).toDouble()
    xn.pow(degreeN).js
}