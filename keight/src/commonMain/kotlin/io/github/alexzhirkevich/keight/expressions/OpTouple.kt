package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.invoke

internal class OpTouple(
    val expressions: List<Expression>
) : Expression {
    override suspend fun invokeRaw(runtime: ScriptRuntime): Any? {
        var res: Any? = Unit
        expressions.fastForEach { res = it.invoke(runtime) }
        return res
    }
}