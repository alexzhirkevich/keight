package io.github.alexzhirkevich.keight.common

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.invoke

internal class OpTouple(
    val expressions: List<Expression>
) : Expression {
    override fun invokeRaw(context: ScriptRuntime): Any? {
        var res: Any? = Unit
        expressions.fastForEach { res = it.invoke(context) }
        return res
    }
}