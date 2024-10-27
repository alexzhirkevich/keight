package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.invoke

internal class JSMapFunction : JSFunction(
    name = "Map",
    parameters = emptyList(),
    body = OpConstant(Unit)
) {

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()) {
            mutableMapOf<Any?, Any?>()
        } else {
            TODO()
        }
    }

    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        return invoke(args, runtime)
    }
}