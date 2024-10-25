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

    override fun invokeRaw(context: ScriptRuntime): Any? {
        return invoke(context, variable, index)
    }

    companion object {
        fun invoke(context: ScriptRuntime, variable : Expression, index : Expression) : Any? {
            val v = checkNotEmpty(variable(context))
            val idx = index(context)

            return v.valueAtIndexOrUnit(idx, context.toNumber(idx).toInt())
        }
    }
}