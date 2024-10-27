package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.checkNotEmpty
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.valueAtIndexOrUnit

internal class OpIndex(
    val variable : Expression,
    val index : Expression,
) : Expression {

    override suspend fun invokeRaw(context: ScriptRuntime): Any? {
        return invoke(context, variable, index)
    }

    companion object {
        suspend fun invoke(runtime: ScriptRuntime, variable : Expression, index : Expression) : Any? {
            val v = checkNotEmpty(variable(runtime))
            val idx = index(runtime)

            return v.valueAtIndexOrUnit(idx, runtime.toNumber(idx).toInt(), runtime)
        }
    }
}